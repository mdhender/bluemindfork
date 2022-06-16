package net.bluemind.central.reverse.proxy.model.client;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import net.bluemind.central.reverse.proxy.model.client.impl.ProxyInfoStoreClientImpl;
import net.bluemind.central.reverse.proxy.model.common.DirInfo;
import net.bluemind.central.reverse.proxy.model.common.DomainInfo;
import net.bluemind.central.reverse.proxy.model.common.InstallationInfo;

public interface ProxyInfoStoreClient {

	static ProxyInfoStoreClient create(Vertx vertx) {
		return new ProxyInfoStoreClientImpl(vertx);
	}

	Future<String> addInstallation(InstallationInfo info);

	Future<Void> addDomain(DomainInfo info);

	Future<Void> addDir(DirInfo info);

	Future<String> ip(String login);

	Future<String> anyIp();

}
