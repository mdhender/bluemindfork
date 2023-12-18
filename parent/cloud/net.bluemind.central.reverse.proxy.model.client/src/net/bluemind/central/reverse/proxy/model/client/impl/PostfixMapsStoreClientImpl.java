package net.bluemind.central.reverse.proxy.model.client.impl;

import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.ADDRESS;
import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.ADD_DIR;
import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.ADD_DOMAIN;
import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.ADD_INSTALLATION;
import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.DEL_DIR;
import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.GET_ALIAS_TO_DOMAIN;
import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.GET_MAILBOX_DOMAIN_MANAGED;
import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.GET_MAILBOX_EXISTS;
import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.GET_MAILBOX_STORE;
import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.GET_SRS_RECIPIENT;
import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.MANAGE_MEMBER;
import static net.bluemind.central.reverse.proxy.model.common.PostfixMapsStoreEventBusAddress.UPDATE_DOMAIN_SETTINGS;

import java.util.Collection;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import net.bluemind.central.reverse.proxy.model.client.PostfixMapsStoreClient;
import net.bluemind.central.reverse.proxy.model.common.DirInfo;
import net.bluemind.central.reverse.proxy.model.common.DomainInfo;
import net.bluemind.central.reverse.proxy.model.common.DomainSettings;
import net.bluemind.central.reverse.proxy.model.common.InstallationInfo;
import net.bluemind.central.reverse.proxy.model.common.MemberInfo;

public class PostfixMapsStoreClientImpl implements PostfixMapsStoreClient {
	private final Logger logger = LoggerFactory.getLogger(PostfixMapsStoreClientImpl.class);

	private final Vertx vertx;

	public PostfixMapsStoreClientImpl(Vertx vertx) {
		this.vertx = vertx;
	}

	@Override
	public Future<Void> addInstallation(InstallationInfo installation) {
		Promise<Void> p = Promise.promise();
		logger.debug("[postfixmaps:model] Adding installation: {}", installation);
		vertx.eventBus().request(ADDRESS, JsonObject.mapFrom(installation), ADD_INSTALLATION, ar -> {
			if (ar.succeeded()) {
				p.complete();
			} else {
				onError(p, ar);
			}
		});
		return p.future();
	}

	@Override
	public Future<Void> addDomain(DomainInfo domainInfo) {
		Promise<Void> p = Promise.promise();
		logger.debug("[postfixmaps:model] Adding domain: {}", domainInfo);
		vertx.eventBus().request(ADDRESS, JsonObject.mapFrom(domainInfo), ADD_DOMAIN, ar -> {
			if (ar.succeeded()) {
				p.complete();
			} else {
				onError(p, ar);
			}
		});
		return p.future();
	}

	@Override
	public Future<Void> addDomainSettings(DomainSettings domainSettings) {
		Promise<Void> p = Promise.promise();
		logger.debug("[postfixmaps:model] Adding domain settings: {}", domainSettings);
		vertx.eventBus().request(ADDRESS, JsonObject.mapFrom(domainSettings), UPDATE_DOMAIN_SETTINGS, ar -> {
			if (ar.succeeded()) {
				p.complete();
			} else {
				onError(p, ar);
			}
		});
		return p.future();
	}

	@Override
	public Future<Void> addDir(DirInfo dirInfo) {
		Promise<Void> p = Promise.promise();
		logger.debug("[postfixmaps:model] Adding entry: {}", dirInfo);
		vertx.eventBus().request(ADDRESS, JsonObject.mapFrom(dirInfo), ADD_DIR, ar -> {
			if (ar.succeeded()) {
				p.complete();
			} else {
				onError(p, ar);
			}
		});
		return p.future();
	}

	@Override
	public Future<Void> removeDir(String deletedUid) {
		Promise<Void> p = Promise.promise();
		logger.debug("[postfixmaps:model] Delete entry: {}", deletedUid);
		vertx.eventBus().request(ADDRESS, JsonObject.of("uid", deletedUid), DEL_DIR, ar -> {
			if (ar.succeeded()) {
				p.complete();
			} else {
				onError(p, ar);
			}
		});
		return p.future();
	}

	@Override
	public Future<Void> manageMember(MemberInfo member) {
		Promise<Void> p = Promise.promise();
		logger.debug("[postfixmaps:model] Manage member: {}", member);
		vertx.eventBus().request(ADDRESS, JsonObject.mapFrom(member), MANAGE_MEMBER, ar -> {
			if (ar.succeeded()) {
				p.complete();
			} else {
				onError(p, ar);
			}
		});
		return p.future();
	}

	@Override
	public Future<Collection<String>> aliasToMailboxes(String email) {
		Promise<Collection<String>> p = Promise.promise();
		logger.debug("[postfixmaps:model] get mailboxes for email: {}", email);

		vertx.eventBus().<JsonObject>request(ADDRESS, new JsonObject().put("email", email), GET_ALIAS_TO_DOMAIN, ar -> {
			if (ar.succeeded()) {
				p.complete(ar.result().body().getJsonArray("mailboxes").stream().map(Object::toString).toList());
			} else {
				onError(p, ar);
			}
		});
		return p.future();
	}

	@Override
	public Future<Boolean> mailboxExists(String mailbox) {
		Promise<Boolean> p = Promise.promise();
		logger.debug("[postfixmaps:model] check mailbox exists: {}", mailbox);

		vertx.eventBus().<JsonObject>request(ADDRESS, new JsonObject().put("mailbox", mailbox), GET_MAILBOX_EXISTS,
				ar -> {
					if (ar.succeeded()) {
						p.complete(Optional.ofNullable(ar.result().body().getString("exists")).map(Boolean::valueOf)
								.orElse(Boolean.FALSE));
					} else {
						onError(p, ar);
					}
				});
		return p.future();
	}

	@Override
	public Future<Boolean> mailboxDomainsManaged(String mailboxDomain) {
		Promise<Boolean> p = Promise.promise();
		logger.debug("[postfixmaps:model] check mailbox domain allowed: {}", mailboxDomain);

		vertx.eventBus().<JsonObject>request(ADDRESS, new JsonObject().put("mailboxDomain", mailboxDomain),
				GET_MAILBOX_DOMAIN_MANAGED, ar -> {
					if (ar.succeeded()) {
						p.complete(Optional.ofNullable(ar.result().body().getString("managed")).map(Boolean::valueOf)
								.orElse(Boolean.FALSE));
					} else {
						onError(p, ar);
					}
				});
		return p.future();
	}

	@Override
	public Future<String> getMailboxRelay(String mailbox) {
		Promise<String> p = Promise.promise();
		logger.debug("[postfixmaps:model] get relay for mailbox: {}", mailbox);

		vertx.eventBus().<JsonObject>request(ADDRESS, new JsonObject().put("mailbox", mailbox), GET_MAILBOX_STORE,
				ar -> {
					if (ar.succeeded()) {
						p.complete(ar.result().body().getString("relay"));
					} else {
						onError(p, ar);
					}
				});
		return p.future();
	}

	@Override
	public Future<String> srsRecipient(String recipient) {
		Promise<String> p = Promise.promise();
		logger.debug("[postfixmaps:model] srs recipient: {}", recipient);

		vertx.eventBus().<JsonObject>request(ADDRESS, new JsonObject().put("recipient", recipient), GET_SRS_RECIPIENT,
				ar -> {
					if (ar.succeeded()) {
						p.complete(ar.result().body().getString("recipient"));
					} else {
						onError(p, ar);
					}
				});
		return p.future();
	}

	private void onError(Promise<?> p, AsyncResult<?> ar) {
		if (ar.cause() != null) {
			p.fail(ar.cause());
		} else {
			p.fail("postfix maps store not available");
		}
	}
}
