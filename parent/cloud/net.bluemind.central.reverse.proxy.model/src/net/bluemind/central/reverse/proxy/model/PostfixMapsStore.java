package net.bluemind.central.reverse.proxy.model;

import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.ADDRESS;
import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.HEADER_ACTION;
import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.HEADER_TS;
import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.TIME_MANAGE_WARN;
import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.TIME_PROCES_WARN;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.curator.shaded.com.google.common.annotations.VisibleForTesting;
import org.apache.curator.shaded.com.google.common.base.Strings;
import org.apache.curator.shaded.com.google.common.util.concurrent.RateLimiter;
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
import net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.PostfixActionHeader;

public class PostfixMapsStore {
	private final Logger logger = LoggerFactory.getLogger(PostfixMapsStore.class);
	private final RateLimiter rlLogLag = RateLimiter.create(1);

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
			logEventProcessDuration(event);
			long start = System.nanoTime();

			PostfixActionHeader action = PostfixActionHeader.fromString(event.headers().get(HEADER_ACTION));
			switch (action) {
			case ADD_INSTALLATION:
				addInstallation(event);
				break;
			case ADD_DIR:
				addDir(event);
				break;
			case DEL_DIR:
				delDir(event);
				break;
			case ADD_DOMAIN:
				addDomain(event);
				break;
			case UPDATE_DOMAIN_SETTINGS:
				updateDomainSettings(event);
				break;
			case DEL_DOMAIN:
				delDomain(event);
				break;
			case MANAGE_MEMBER:
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

			long processedTime = System.nanoTime() - start;
			if (logger.isDebugEnabled()) {
				logger.debug("PostfixMapsStore: vertx event consumption took {}ms long", processedTime / 1000);
			} else if (processedTime > TIME_MANAGE_WARN && rlLogLag.tryAcquire()) {
				logger.warn("PostfixMapsStore: vertx event consumption took more than {}ms long: {}ms",
						TIME_MANAGE_WARN / 1000, processedTime / 1000);
			}
		});

		return this;
	}

	private void logEventProcessDuration(Message<JsonObject> event) {
		try {
			long ts = Long.parseLong(event.headers().get(HEADER_TS));

			long processTime = System.nanoTime() - ts;
			if (processTime > TIME_PROCES_WARN && rlLogLag.tryAcquire()) {
				logger.warn("PostfixMapsStore: vertx event process took more than {}ms long: {}ms",
						TIME_PROCES_WARN / 1000, processTime / 1000);
			}
		} catch (NumberFormatException nfe) {
			// Ignore bad event timestamp
		}
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
