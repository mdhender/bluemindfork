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

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.patterns.PolledMeter;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
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

	private static final Cache<String, SessionData> sessions = sessions();

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
								sessions.invalidate(sid);
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
					sessions.invalidateAll();
					logoutListener.loggedOutAll();
					logger.warn("all sessions was cleared (core not ready or forgot sessions : {})", newState);
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
		return new C2Provider(vertx, sessions);
	}

	private static Cache<String, SessionData> sessions() {
		Cache<String, SessionData> coreSessions = CacheBuilder.newBuilder()//
				.recordStats()//
				.build();

		Registry reg = MetricsRegistry.get();
		IdFactory idf = new IdFactory("activeSessions", reg, C2ProviderFactory.class);
		AtomicLong active = new AtomicLong();
		PolledMeter.using(reg).withId(idf.name("distinctUsers")).monitorValue(active);

		VertxPlatform.getVertx().setPeriodic(60000, tid -> {
			coreSessions.cleanUp();
			active.set(coreSessions.size());
			logger.info("SESSION STATS: size: {}, {}", coreSessions.size(), coreSessions.stats());
		});
		return coreSessions;
	}

	@Override
	public void setLogoutListener(ILogoutListener ll) {
		this.logoutListener = ll;
	}

	@Override
	public String getKind() {
		return "CORE2";
	}

}
