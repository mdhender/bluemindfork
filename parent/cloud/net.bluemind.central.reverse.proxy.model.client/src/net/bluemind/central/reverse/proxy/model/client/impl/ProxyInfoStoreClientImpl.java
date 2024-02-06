package net.bluemind.central.reverse.proxy.model.client.impl;

import static net.bluemind.central.reverse.proxy.model.common.ProxyInfoStoreEventBusAddress.ADDRESS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonObject;
import net.bluemind.central.reverse.proxy.model.client.ProxyInfoStoreClient;
import net.bluemind.central.reverse.proxy.model.common.DirInfo;
import net.bluemind.central.reverse.proxy.model.common.DomainInfo;
import net.bluemind.central.reverse.proxy.model.common.InstallationInfo;
import net.bluemind.central.reverse.proxy.model.common.ProxyInfoStoreEventBusAddress.ActionHeader;

public class ProxyInfoStoreClientImpl implements ProxyInfoStoreClient {
	private final Logger logger = LoggerFactory.getLogger(ProxyInfoStoreClientImpl.class);

	private final Vertx vertx;

	public ProxyInfoStoreClientImpl(Vertx vertx) {
		this.vertx = vertx;
	}

	@Override
	public Future<String> addInstallation(InstallationInfo info) {
		Promise<String> p = Promise.promise();
		logger.debug("[model] Adding dataLocation: {}", info);
		vertx.eventBus().request(ADDRESS, JsonObject.mapFrom(info), ActionHeader.ADD_INSTALLATION.getDeliveryOptions(),
				ar -> {
					if (ar.succeeded()) {
						p.complete(ar.result().body() != null ? (String) ar.result().body() : null);
					} else {
						onError(p, ar);
					}
				});

		return p.future();
	}

	@Override
	public Future<Void> addDomain(DomainInfo info) {
		Promise<Void> p = Promise.promise();
		logger.debug("[model] Adding domain: {}", info);
		vertx.eventBus().request(ADDRESS, JsonObject.mapFrom(info), ActionHeader.ADD_DOMAIN.getDeliveryOptions(),
				ar -> {
					if (ar.succeeded()) {
						p.complete();
					} else {
						onError(p, ar);
					}
				});

		return p.future();
	}

	@Override
	public Future<Void> addDir(DirInfo info) {
		Promise<Void> p = Promise.promise();
		logger.debug("[model] Adding login: {}", info);
		vertx.eventBus().request(ADDRESS, JsonObject.mapFrom(info), ActionHeader.ADD_DIR.getDeliveryOptions(), ar -> {
			if (ar.succeeded()) {
				p.complete();
			} else {
				onError(p, ar);
			}
		});

		return p.future();
	}

	@Override
	public Future<String> ip(String login) {
		Promise<String> p = Promise.promise();
		vertx.eventBus().<JsonObject>request(ADDRESS, new JsonObject().put("login", login),
				ActionHeader.IP.getDeliveryOptions(), ar -> {
					if (ar.succeeded()) {
						p.complete(ar.result().body().getString("ip"));
					} else if (is404(ar.cause())) {
						p.fail("no user registred with this login");
					} else {
						onError(p, ar);
					}
				});

		return p.future();
	}

	@Override
	public Future<String> anyIp() {
		Promise<String> p = Promise.promise();
		vertx.eventBus().<JsonObject>request(ADDRESS, null, ActionHeader.ANY_IP.getDeliveryOptions(), ar -> {
			if (ar.succeeded()) {
				p.complete(ar.result().body().getString("ip"));
			} else if (is404(ar.cause())) {
				p.fail("no downstream ip available");
			} else {
				onError(p, ar);
			}
		});
		return p.future();
	}

	private boolean is404(Throwable t) {
		return t instanceof ReplyException re && re.failureCode() == 404;
	}

	private void onError(Promise<?> p, AsyncResult<?> ar) {
		if (ar.cause() != null) {
			p.fail(ar.cause());
		} else {
			p.fail("proxy info store not available");
		}
	}
}
