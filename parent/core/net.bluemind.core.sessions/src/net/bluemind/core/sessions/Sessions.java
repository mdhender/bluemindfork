package net.bluemind.core.sessions;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;

import net.bluemind.config.Token;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.api.SystemState;

public class Sessions implements BundleActivator {

	private static final Cache<String, SecurityContext> STORE = buildCache();

	private static final String IDENTITY = UUID.randomUUID().toString();

	private static Cache<String, SecurityContext> buildCache() {
		return CacheBuilder.newBuilder() //
				.recordStats() //
				.expireAfterAccess(20, TimeUnit.MINUTES)
				.removalListener((RemovalNotification<String, SecurityContext> notification) -> {
					if (notification.getCause() != RemovalCause.REPLACED && notification.getValue().isInteractive()) {
						notifyDeletion(notification.getKey());
					}
				}).build();
	}

	private static void notifyDeletion(String sessionId) {
		for (ISessionDeletionListener listener : SessionDeletionListeners.get()) {
			listener.deleted(IDENTITY, sessionId);
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(Sessions.class);

	public static final Cache<String, SecurityContext> get() {
		return STORE;
	}

	private long timerId;

	private long logStatsTimerId;

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		Vertx vx = VertxPlatform.getVertx();
		vx.eventBus().registerHandler(SystemState.BROADCAST, (Message<JsonObject> event) -> {
			String op = event.body().getString("operation");
			SystemState state = SystemState.fromOperation(op);
			if (state == SystemState.CORE_STATE_MAINTENANCE) {
				STORE.invalidateAll();
			}
		});

		timerId = vx.setPeriodic(5000, i -> STORE.cleanUp());

		logStatsTimerId = vx.setPeriodic(60000,
				i -> logger.info("STATS size: {}, stats: {}", STORE.size(), STORE.stats()));
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		VertxPlatform.getVertx().cancelTimer(timerId);
		VertxPlatform.getVertx().cancelTimer(logStatsTimerId);
	}

	public static SecurityContext sessionContext(String key) {
		if (key == null) {
			return null;
		}
		if (key.equals(Token.admin0())) {
			return SecurityContext.SYSTEM;
		}

		return Optional.ofNullable(STORE.getIfPresent(key)).orElseGet(() -> {
			for (ISessionsProvider sp : SessionProviders.get()) {
				SecurityContext fromProv = sp.get(key).orElse(null);
				if (fromProv != null) {
					// prevent rebuild from provider
					STORE.put(key, fromProv);
					return fromProv;
				}
			}
			return null;
		});
	}

}
