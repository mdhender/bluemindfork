/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.backend.mail.replica.service.internal;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;

public class GuidExpectedIdCache {
	private static final Cache<String, Long> cache = CacheBuilder.newBuilder()
			.recordStats()
			.maximumSize(512)
			.expireAfterWrite(1, TimeUnit.MINUTES)
			.build();

	public static class CacheRegistration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.registerReadOnly(GuidExpectedIdCache.class, cache);
		}
	}

	private GuidExpectedIdCache() {
	}

	public static Long expectedId(String guid) {
		return cache.getIfPresent(guid);
	}

	public static void store(String guid, Long id) {
		cache.put(guid, id);
	}

	public static void invalidate(String guid) {
		cache.invalidate(guid);
	}
}
