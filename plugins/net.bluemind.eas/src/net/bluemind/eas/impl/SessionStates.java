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
package net.bluemind.eas.impl;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.patterns.PolledMeter;

import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.eas.backend.SessionPersistentState;
import net.bluemind.eas.dto.device.DeviceId;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

public class SessionStates {
	private static final Cache<DeviceId, SessionPersistentState> states = buildCache();

	private static Cache<DeviceId, SessionPersistentState> buildCache() {
		Cache<DeviceId, SessionPersistentState> s = CacheBuilder.newBuilder()
				.recordStats()
				.expireAfterAccess(1, TimeUnit.HOURS)
				.build();

		Registry reg = MetricsRegistry.get();
		IdFactory idf = new IdFactory("activeSessions", reg, SessionStates.class);
		PolledMeter.using(reg).withId(idf.name("devices")).monitorSize(s.asMap());

		return s;
	}

	public static class CacheRegistration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register(SessionStates.class, states);
		}
	}

	private SessionStates() {
	}

	public static SessionPersistentState get(DeviceId did) {
		SessionPersistentState mutable = states.getIfPresent(did);
		if (mutable == null) {
			mutable = new SessionPersistentState();
			states.put(did, mutable);
		}
		return mutable;
	}

}
