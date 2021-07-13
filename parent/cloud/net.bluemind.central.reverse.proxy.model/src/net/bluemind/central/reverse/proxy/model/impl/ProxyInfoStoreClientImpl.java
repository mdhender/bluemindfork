package net.bluemind.central.reverse.proxy.model.impl;

import static net.bluemind.central.reverse.proxy.model.ProxyInfoStoreAddress.ADDRESS;
import static net.bluemind.central.reverse.proxy.model.ProxyInfoStoreAddress.ADD_DIR;
import static net.bluemind.central.reverse.proxy.model.ProxyInfoStoreAddress.ADD_DOMAIN;
import static net.bluemind.central.reverse.proxy.model.ProxyInfoStoreAddress.ADD_INSTALLATION;
import static net.bluemind.central.reverse.proxy.model.ProxyInfoStoreAddress.ANY_IP;
import static net.bluemind.central.reverse.proxy.model.ProxyInfoStoreAddress.IP;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonObject;
import net.bluemind.central.reverse.proxy.model.ProxyInfoStoreClient;
import net.bluemind.central.reverse.proxy.model.mapper.DirInfo;
import net.bluemind.central.reverse.proxy.model.mapper.DomainInfo;
import net.bluemind.central.reverse.proxy.model.mapper.InstallationInfo;

public class ProxyInfoStoreClientImpl implements ProxyInfoStoreClient {

	private static final String STORE_NOT_AVAILABLE = "proxy info store not available";

	private final Logger logger = LoggerFactory.getLogger(ProxyInfoStoreClientImpl.class);

	private final Vertx vertx;

	public ProxyInfoStoreClientImpl(Vertx vertx) {
		this.vertx = vertx;
	}

	@Override
	public Future<Void> addInstallation(InstallationInfo info) {
		Promise<Void> p = Promise.promise();
		logger.info("Adding dataLocation: {}", info);
		vertx.eventBus().request(ADDRESS, JsonObject.mapFrom(info), ADD_INSTALLATION, ar -> {
			if (ar.succeeded()) {
				p.complete();
			} else {
				p.fail(STORE_NOT_AVAILABLE);
			}
		});
		return p.future();
	}

	@Override
	public Future<Void> addDomain(DomainInfo info) {
		Promise<Void> p = Promise.promise();
		logger.info("Adding domain: {}", info);
		vertx.eventBus().request(ADDRESS, JsonObject.mapFrom(info), ADD_DOMAIN, ar -> {
			if (ar.succeeded()) {
				p.complete();
			} else {
				p.fail(STORE_NOT_AVAILABLE);
			}
		});
		return p.future();
	}

	@Override
	public Future<Void> addDir(DirInfo info) {
		Promise<Void> p = Promise.promise();
		logger.info("Adding login: {}", info);
		vertx.eventBus().request(ADDRESS, JsonObject.mapFrom(info), ADD_DIR, ar -> {
			if (ar.succeeded()) {
				p.complete();
			} else {
				p.fail(STORE_NOT_AVAILABLE);
			}
		});
		return p.future();
	}

	@Override
	public Future<String> ip(String login) {
		Promise<String> p = Promise.promise();
		vertx.eventBus().<JsonObject>request(ADDRESS, new JsonObject().put("login", login), IP, ar -> {
			if (ar.succeeded()) {
				p.complete(ar.result().body().getString("ip"));
			} else if (is404(ar.cause())) {
				p.fail("no user registred with this login");
			} else {
				p.fail(STORE_NOT_AVAILABLE);
			}
		});
		return p.future();
	}

	@Override
	public Future<String> anyIp() {
		Promise<String> p = Promise.promise();
		vertx.eventBus().<JsonObject>request(ADDRESS, null, ANY_IP, ar -> {
			if (ar.succeeded()) {
				p.complete(ar.result().body().getString("ip"));
			} else if (is404(ar.cause())) {
				p.fail("no downstream ip available");
			} else {
				p.fail(STORE_NOT_AVAILABLE);
			}
		});
		return p.future();
	}

	private boolean is404(Throwable t) {
		return t instanceof ReplyException && ((ReplyException) t).failureCode() == 404;
	}

}
