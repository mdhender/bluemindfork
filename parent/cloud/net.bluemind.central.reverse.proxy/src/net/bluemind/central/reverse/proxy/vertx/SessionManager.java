package net.bluemind.central.reverse.proxy.vertx;

import com.typesafe.config.Config;

import net.bluemind.central.reverse.proxy.vertx.impl.CachingSessionManager;
import net.bluemind.central.reverse.proxy.vertx.impl.CloseableSession;

public interface SessionManager {

	static SessionManager create(Config config) {
		return new CachingSessionManager(config);
	}

	void add(String host, CloseableSession session);

	void close(String host);
}
