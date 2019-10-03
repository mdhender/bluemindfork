package net.bluemind.core.sessions;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import net.bluemind.config.Token;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.IMQConnectHandler;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.Producer;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.api.SystemState;

public class Sessions implements BundleActivator {

	private static BundleContext context;

	private static final Cache<String, SecurityContext> sessions = buildCache();

	private static final String identity = UUID.randomUUID().toString();

	private static Cache<String, SecurityContext> buildCache() {
		Cache<String, SecurityContext> ret = CacheBuilder.newBuilder() //
				.recordStats() //
				.expireAfterAccess(20, TimeUnit.MINUTES)
				.removalListener(new RemovalListener<String, SecurityContext>() {

					@Override
					public void onRemoval(RemovalNotification<String, SecurityContext> notification) {
						if (notification.getCause() != RemovalCause.REPLACED
								&& notification.getValue().isInteractive()) {
							notifyDeletion(notification.getKey());
						}

					}
				}).build();

		return ret;
	}

	private static void notifyDeletion(String sessionId) {
		Producer prod = MQ.getProducer(Topic.CORE_SESSIONS);
		if (prod != null) {
			OOPMessage cm = MQ.newMessage();
			cm.putStringProperty("sender", identity);
			cm.putStringProperty("operation", "logout");
			cm.putStringProperty("sid", sessionId);
			prod.send(cm);
			logger.debug("MQ: logout {} sent.", sessionId);
		} else {
			logger.warn("MQ is missing, logout support will fail");
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(Sessions.class);

	public static final Cache<String, SecurityContext> get() {
		return sessions;
	}

	static BundleContext getContext() {
		return context;
	}

	private long timerId;

	private long logStatsTimerId;

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		Sessions.context = bundleContext;
		VertxPlatform.getVertx().eventBus().registerHandler(SystemState.BROADCAST, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				String op = event.body().getString("operation");
				SystemState state = SystemState.fromOperation(op);
				if (state == SystemState.CORE_STATE_MAINTENANCE) {
					sessions.invalidateAll();
				}
			}
		});
		MQ.init(new IMQConnectHandler() {

			@Override
			public void connected() {
				MQ.registerProducer(Topic.CORE_SESSIONS);
				logger.info("**** SESSION NOTIFICATION REGISTERED ****");
			}
		});

		timerId = VertxPlatform.getVertx().setPeriodic(5000, (i) -> {
			sessions.cleanUp();
		});

		logStatsTimerId = VertxPlatform.getVertx().setPeriodic(60000, (i) -> {
			logger.info("Sessions STATS: size: {}, stats: {}", sessions.size(), sessions.stats());
		});
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		VertxPlatform.getVertx().cancelTimer(timerId);
		VertxPlatform.getVertx().cancelTimer(logStatsTimerId);
		Sessions.context = null;
	}

	public static SecurityContext sessionContext(String key) {
		if (key != null && key.equals(Token.admin0())) {
			return SecurityContext.SYSTEM;
		}

		return Optional.ofNullable(sessions.getIfPresent(key)).orElseGet(() -> {
			for (ISessionsProvider sp : SessionProviders.get()) {
				SecurityContext fromProv = sp.get(key).orElse(null);
				if (fromProv != null) {
					// prevent rebuild from provider
					sessions.put(key, fromProv);
					return fromProv;
				}
			}
			return null;
		});
	}

}
