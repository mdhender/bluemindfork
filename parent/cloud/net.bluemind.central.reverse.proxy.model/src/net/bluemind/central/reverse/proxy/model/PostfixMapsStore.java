package net.bluemind.central.reverse.proxy.model;

import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.ADDRESS;
import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.ADD_DIR_NAME;
import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.ADD_DOMAIN_NAME;
import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.ADD_INSTALLATION_NAME;
import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.ALIAS_TO_MAILBOX;
import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.DEL_DIR_NAME;
import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.DEL_DOMAIN_NAME;
import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.MAILBOX_DOMAIN_MANAGED;
import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.MAILBOX_EXISTS;
import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.MAILBOX_STORE;
import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.MANAGE_MEMBER_NAME;
import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.SRS_RECIPIENT;
import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.TIME_WARN;
import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.UPDATE_DOMAIN_SETTINGS_NAME;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.curator.shaded.com.google.common.annotations.VisibleForTesting;
import org.apache.curator.shaded.com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import net.bluemind.central.reverse.proxy.model.common.DirInfo;
import net.bluemind.central.reverse.proxy.model.common.DomainInfo;
import net.bluemind.central.reverse.proxy.model.common.DomainSettings;
import net.bluemind.central.reverse.proxy.model.common.InstallationInfo;
import net.bluemind.central.reverse.proxy.model.common.MemberInfo;

public class PostfixMapsStore {
	private final Logger logger = LoggerFactory.getLogger(PostfixMapsStore.class);

	private final PostfixMapsStorage storage;

	private MessageConsumer<JsonObject> consumer;

	private PostfixMapsStore(PostfixMapsStorage storage) {
		this.storage = storage;
	}

	public static PostfixMapsStore create() {
		return PostfixMapsStore.create(PostfixMapsStorage.create());
	}

	public static PostfixMapsStore create(PostfixMapsStorage storage) {
		return new PostfixMapsStore(storage);
	}

	public PostfixMapsStore setupService(Vertx vertx) {
		consumer = vertx.eventBus().<JsonObject>consumer(ADDRESS).handler(event -> {
			long time = System.currentTimeMillis();

			String action = event.headers().get("action");
			switch (action) {
			case ADD_INSTALLATION_NAME:
				addInstallation(event);
				break;
			case ADD_DIR_NAME:
				addDir(event);
				break;
			case DEL_DIR_NAME:
				delDir(event);
				break;
			case ADD_DOMAIN_NAME:
				addDomain(event);
				break;
			case UPDATE_DOMAIN_SETTINGS_NAME:
				updateDomainSettings(event);
				break;
			case DEL_DOMAIN_NAME:
				delDomain(event);
				break;
			case MANAGE_MEMBER_NAME:
				manageMember(event);
				break;
			case ALIAS_TO_MAILBOX:
				aliasToMailboxes(event);
				break;
			case MAILBOX_EXISTS:
				mailboxExists(event);
				break;
			case MAILBOX_DOMAIN_MANAGED:
				mailboxDomainManaged(event);
				break;
			case MAILBOX_STORE:
				mailboxRelay(event);
				break;
			case SRS_RECIPIENT:
				srsRecipient(event);
				break;
			default:
				event.fail(404, "Unknown action '" + action + "'");
			}

			time = System.currentTimeMillis() - time;
			if (logger.isDebugEnabled()) {
				logger.debug("PostfixMapsStore: vertx event consumption took {}ms long", time);
			} else if (time > TIME_WARN) {
				logger.warn("PostfixMapsStore: vertx event consumption took {}ms long", time);
			}
		});

		return this;
	}

	private void addInstallation(Message<JsonObject> event) {
		try {
			InstallationInfo installationInfo = event.body().mapTo(InstallationInfo.class);
			if (!installationInfo.hasCore) {
				storage.removeDataLocation(installationInfo.dataLocationUid);
				event.reply(null);
				return;
			}

			storage.updateInstallationUid(installationInfo.uid);
			storage.updateDataLocation(installationInfo.dataLocationUid, installationInfo.ip);
			event.reply(null);
		} catch (IllegalArgumentException e) {
			event.fail(500, "unable to decode parameters '" + event.body().encode() + "'");
		}
	}

	private void addDir(Message<JsonObject> event) {
		try {
			DirInfo dir = event.body().mapTo(DirInfo.class);
			if (dir.archived) {
				storage.removeUid(dir.entryUid);
				event.reply(null);
				return;
			}

			if (dir.kind.equalsIgnoreCase("group")) {
				if (dir.routing.equalsIgnoreCase("internal")) {
					storage.updateMailbox(dir.domainUid, dir.entryUid, getDirMailboxName(dir), dir.routing,
							dir.dataLocation);
					storage.addRecipient(dir.entryUid, "group-archive", dir.entryUid);
				} else {
					storage.removeRecipient(dir.entryUid, "group-archive", dir.entryUid);
					storage.removeMailbox(dir.entryUid);
				}
			} else if (!Strings.isNullOrEmpty(dir.mailboxName) && !Strings.isNullOrEmpty(dir.routing)
					&& !Strings.isNullOrEmpty(dir.dataLocation)) {
				storage.updateMailbox(dir.domainUid, dir.entryUid, getDirMailboxName(dir), dir.routing,
						dir.dataLocation);
			}

			storage.updateEmails(dir.entryUid, dir.emails);
			event.reply(null);
		} catch (IllegalArgumentException e) {
			event.fail(500, "unable to decode parameters '" + event.body().encode() + "'");
		}
	}

	private void delDir(Message<JsonObject> event) {
		try {
			String entryUid = event.body().getString("uid");
			storage.removeUid(entryUid);
			event.reply(null);
		} catch (IllegalArgumentException e) {
			event.fail(500, "unable to decode parameters '" + event.body().encode() + "'");
		}
	}

	private String getDirMailboxName(DirInfo dir) {
		return (dir.kind.equalsIgnoreCase("user") ? "" : "+") + dir.mailboxName + "@" + dir.domainUid;
	}

	private void addDomain(Message<JsonObject> event) {
		try {
			DomainInfo domain = event.body().mapTo(DomainInfo.class);
			storage.updateDomain(domain.uid, domain.aliases);
			event.reply(null);
		} catch (IllegalArgumentException e) {
			event.fail(500, "unable to decode parameters '" + event.body().encode() + "'");
		}
	}

	private void updateDomainSettings(Message<JsonObject> event) {
		try {
			DomainSettings domainSettings = event.body().mapTo(DomainSettings.class);
			storage.updateDomainSettings(domainSettings.domainUid, domainSettings.mailRoutingRelay,
					domainSettings.mailForwardUnknown);
			event.reply(null);
		} catch (IllegalArgumentException e) {
			event.fail(500, "unable to decode parameters '" + event.body().encode() + "'");
		}
	}

	private void delDomain(Message<JsonObject> event) {
		try {
			DomainInfo domain = event.body().mapTo(DomainInfo.class);
			storage.removeDomain(domain.uid);
			event.reply(null);
		} catch (IllegalArgumentException e) {
			event.fail(500, "unable to decode parameters '" + event.body().encode() + "'");
		}
	}

	private void manageMember(Message<JsonObject> event) {
		try {
			MemberInfo member = event.body().mapTo(MemberInfo.class);
			if (member.added) {
				storage.addRecipient(member.groupUid, member.memberType, member.memberUid);
			} else {
				storage.removeRecipient(member.groupUid, member.memberType, member.memberUid);
			}
			event.reply(null);
		} catch (IllegalArgumentException e) {
			event.fail(500, "unable to decode parameters '" + event.body().encode() + "'");
		}
	}

	private void aliasToMailboxes(Message<JsonObject> event) {
		String email = event.body().getString("email");
		event.reply(new JsonObject().put("mailboxes", storage.aliasToMailboxes(email)));
	}

	private void mailboxExists(Message<JsonObject> event) {
		String mailbox = event.body().getString("mailbox");
		event.reply(new JsonObject().put("exists", storage.mailboxManaged(mailbox)));
	}

	private void mailboxDomainManaged(Message<JsonObject> event) {
		String mailboxDomain = event.body().getString("mailboxDomain");
		event.reply(new JsonObject().put("managed", storage.domainManaged(mailboxDomain)));
	}

	private void mailboxRelay(Message<JsonObject> event) {
		String mailbox = event.body().getString("mailbox");
		event.reply(new JsonObject().put("relay", storage.mailboxRelay(mailbox)));
	}

	private void srsRecipient(Message<JsonObject> event) {
		String recipient = event.body().getString("recipient");
		event.reply(new JsonObject().put("recipient", storage.srsRecipient(recipient)));
	}

	@VisibleForTesting
	public void tearDown() throws InterruptedException, ExecutionException {
		if (consumer != null) {
			CompletableFuture<Void> c = new CompletableFuture<>();
			consumer.unregister().onSuccess(c::complete).onFailure(c::completeExceptionally);
			c.get();
		}
	}
}
