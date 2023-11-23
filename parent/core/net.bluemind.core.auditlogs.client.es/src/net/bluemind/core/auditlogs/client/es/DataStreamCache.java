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

package net.bluemind.core.auditlogs.client.es;

import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.rest.BmContext;

public class DataStreamCache {

	public static class Registration implements ICacheRegistration {

		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register(DataStreamCache.class,
					Caffeine.newBuilder().recordStats().expireAfterWrite(10, TimeUnit.MINUTES).build());

		}

	}

	private static final DataStreamCache NOT_CACHED = new DataStreamCache(null);
	private final Cache<String, String> icache;

	public DataStreamCache(Cache<String, String> cache) {
		this.icache = cache;
	}

	public static DataStreamCache get(BmContext context) {
		CacheRegistry cr = context.provider().instance(CacheRegistry.class);
		if (cr == null) {
			return NOT_CACHED;
		} else {
			return new DataStreamCache(cr.get(DataStreamCache.class));
		}
	}

	public String getIfPresent(String uid) {
		if (icache != null) {
			return icache.getIfPresent(uid);
		} else {
			return null;
		}
	}

	public void put(String uid, String name) {
		if (icache == null) {
			return;
		}
		icache.put(uid, name);
	}

	public void invalidate(String name) {
		if (icache != null) {
			icache.invalidate(name);
		}
	}
}
