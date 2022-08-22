package net.bluemind.core.sessions;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.common.cache.persistence.CacheBackingStore;
import net.bluemind.config.Token;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.lib.vertx.VertxPlatform;

public class Sessions implements BundleActivator {
	private static final CacheBackingStore<SecurityContext> STORE = SessionsBackingStore.build();

	private static final Logger logger = LoggerFactory.getLogger(Sessions.class);

	public static final CacheBackingStore<SecurityContext> get() {
		return STORE;
	}

	private long timerId;
	private long logStatsTimerId;
	private long storeCleanUpTimerId;

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		Vertx vx = VertxPlatform.getVertx();
		vx.eventBus().consumer("core.status.broadcast", (Message<JsonObject> event) -> {
			String op = event.body().getString("operation");
			if (op.equals("core.state.maintenance")) {
				STORE.getCache().invalidateAll();
			}
		});

		timerId = vx.setPeriodic(5000, i -> STORE.getCache().cleanUp());

		logStatsTimerId = vx.setPeriodic(60000, i -> logger.info("STATS size: {}, stats: {}",
				STORE.getCache().estimatedSize(), STORE.getCache().stats()));

		// Every day, remove sessions files from disk that are not in cache
		storeCleanUpTimerId = vx.setPeriodic(TimeUnit.DAYS.toMillis(1), i -> STORE.cleanUp());
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		VertxPlatform.getVertx().cancelTimer(timerId);
		VertxPlatform.getVertx().cancelTimer(logStatsTimerId);
		VertxPlatform.getVertx().cancelTimer(storeCleanUpTimerId);
	}

	public static SecurityContext sessionContext(String key) {
		if (key == null || key.isEmpty()) {
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
