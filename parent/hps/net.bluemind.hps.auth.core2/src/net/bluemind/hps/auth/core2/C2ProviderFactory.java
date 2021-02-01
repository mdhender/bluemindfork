/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.hps.auth.core2;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.patterns.PolledMeter;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.common.cache.persistence.CacheBackingStore;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.proxy.http.IAuthProvider;
import net.bluemind.proxy.http.IAuthProviderFactory;
import net.bluemind.proxy.http.ILogoutListener;
import net.bluemind.system.api.SystemState;
import net.bluemind.system.stateobserver.IStateListener;

public class C2ProviderFactory implements IAuthProviderFactory {
	private static final Logger logger = LoggerFactory.getLogger(C2ProviderFactory.class);

	public static final String KIND = "CORE2";
	public static final CacheBackingStore<SessionData> sessions = sessions();

	public static class CacheRegistration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.registerReadOnly(C2ProviderFactory.class, sessions.getCache());
		}
	}

	private ILogoutListener logoutListener;

	public C2ProviderFactory() {
		VertxPlatform.eventBus().consumer(Topic.CORE_SESSIONS,

				new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> event) {
						JsonObject cm = event.body();
						String op = cm.getString("operation");
						if ("logout".equals(op)) {
							String sid = cm.getString("sid");
							SessionData toRemove = sessions.getIfPresent(sid);
							if (toRemove != null) {
								sessions.getCache().invalidate(sid);
								logoutListener.loggedOut(sid);
								logger.info("[{}] Logged-out {}", toRemove.loginAtDomain, sid);
							} else {
								logger.warn("[{}] not found for mq logout", sid);
							}
						}
					}
				});

		VertxPlatform.eventBus().consumer(IStateListener.STATE_BUS_ADDRESS, new Handler<Message<String>>() {
			@Override
			public void handle(Message<String> event) {
				String stateAsString = event.body();
				SystemState newState = SystemState.valueOf(stateAsString);

				switch (newState) {
				case CORE_STATE_UNKNOWN:
				case CORE_STATE_STARTING:
				case CORE_STATE_MAINTENANCE:
				case CORE_STATE_STOPPING:
				case CORE_STATE_UPGRADE:
					logoutListener.checkAll();
					logger.warn("All sessions was set to check (core not ready or forgot sessions : {})", newState);
					break;
				case CORE_STATE_RUNNING:
					break;
				case CORE_STATE_NOT_INSTALLED:
				default:
					break;
				}

			}
		});
	}

	@Override
	public IAuthProvider get(Vertx vertx) {
		// Every day, remove sessions files from disk that are not in cache
		vertx.setPeriodic(TimeUnit.DAYS.toMillis(1), i -> sessions.cleanUp());

		return new C2Provider(vertx, sessions);
	}

	private static CacheBackingStore<SessionData> sessions() {
		CacheBackingStore<SessionData> cachePersistence = new CacheBackingStore<>(Caffeine.newBuilder().recordStats(),
				"/var/cache/bm-hps/core2", SessionData::toJson, SessionData::fromJson, Optional.empty());

		Registry reg = MetricsRegistry.get();
		IdFactory idf = new IdFactory("activeSessions", reg, C2ProviderFactory.class);
		PolledMeter.using(reg).withId(idf.name("distinctUsers")).monitorSize(cachePersistence.getCache().asMap());
		return cachePersistence;
	}

	@Override
	public void setLogoutListener(ILogoutListener ll) {
		this.logoutListener = ll;
	}

	@Override
	public String getKind() {
		return KIND;
	}

}
