package net.bluemind.central.reverse.proxy.model;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import net.bluemind.central.reverse.proxy.model.impl.ProxyInfoStoreClientImpl;
import net.bluemind.central.reverse.proxy.model.mapper.DirInfo;
import net.bluemind.central.reverse.proxy.model.mapper.DomainInfo;
import net.bluemind.central.reverse.proxy.model.mapper.InstallationInfo;

public interface ProxyInfoStoreClient {

	static ProxyInfoStoreClient create(Vertx vertx) {
		return new ProxyInfoStoreClientImpl(vertx);
	}

	Future<Void> addInstallation(InstallationInfo info);

	Future<Void> addDomain(DomainInfo info);

	Future<Void> addDir(DirInfo info);

	Future<String> ip(String login);

	Future<String> anyIp();

}
