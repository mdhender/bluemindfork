package net.bluemind.central.reverse.proxy.vertx.impl;

import static net.bluemind.central.reverse.proxy.common.config.CrpConfig.Proxy.INITIAL_CAPACITY;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.typesafe.config.Config;

import net.bluemind.central.reverse.proxy.vertx.SessionManager;

public class CachingSessionManager implements SessionManager {
	private final Logger logger = LoggerFactory.getLogger(CachingSessionManager.class);

	private final Config config;
	private final Map<String, Cache<String, CloseableSession>> hostsSessions;

	public CachingSessionManager(Config config) {
		this.config = config;
		this.hostsSessions = new ConcurrentHashMap<>();
	}

	@Override
	public void add(String host, CloseableSession session) {
		Cache<String, CloseableSession> sessions = hostsSessions.computeIfAbsent(host, key -> buildHostCache());
		sessions.put(session.id(), session);
		session.onEnd(() -> sessions.invalidate(session.id()));
	}

	@Override
	public void close(String host) {
		Cache<String, CloseableSession> hostSessions = hostsSessions.get(host);
		if (hostSessions == null) {
			return;
		}
		hostSessions.asMap().values().forEach(this::closeSession);
		hostSessions.invalidateAll();
		hostsSessions.remove(host);
	}

	private Cache<String, CloseableSession> buildHostCache() {
		return Caffeine.newBuilder().recordStats() //
				.initialCapacity(config.getInt(INITIAL_CAPACITY)) //
				.build();
	}

	private void closeSession(CloseableSession session) {
		try {
			session.close();
		} catch (Exception e) {
			logger.error("Fail to close a session on installation IP change");
		}
	}

}
