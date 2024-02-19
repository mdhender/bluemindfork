/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.webmodule.authenticationfilter.internal;

import com.github.benmanes.caffeine.cache.Caffeine;

import net.bluemind.common.cache.persistence.CacheBackingStore;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;

public class SessionsCache {

	public static class CacheRegistration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register(SessionsCache.class, sessions.getCache());
		}
	}

	private static final CacheBackingStore<SessionData> sessions = new CacheBackingStore<>(
			Caffeine.newBuilder().recordStats(), "/var/cache/bm-sessions/core2", SessionData::toJson,
			SessionData::fromJson);

	private SessionsCache() {
	}

	public static CacheBackingStore<SessionData> get() {
		return sessions;
	}

}
