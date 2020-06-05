/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.domain.service.internal;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.rest.BmContext;

public class DomainSettingsCache {
	public static class Registration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register(
				DomainSettingsCache.class,
				CacheBuilder.newBuilder()
					.recordStats()
					.expireAfterWrite(10, TimeUnit.MINUTES)
					.build());
		}
	}

	private static final DomainSettingsCache NOT_CACHED = new DomainSettingsCache(null);

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(DomainSettingsCache.class);

	private final Cache<String, Map<String, String>> icache;

	public DomainSettingsCache(Cache<String, Map<String, String>> cache) {
		this.icache = cache;
	}

	public static DomainSettingsCache get(BmContext context) {
		CacheRegistry cr = context.provider().instance(CacheRegistry.class);
		if (cr == null) {
			return NOT_CACHED;
		} else {
			return new DomainSettingsCache(cr.get(DomainSettingsCache.class));
		}

	}

	public Map<String, String> getIfPresent(String uid) {
		if (icache != null) {
			return icache.getIfPresent(uid);
		} else {
			return null;
		}
	}

	public void put(String uid, Map<String, String> settings) {
		if (icache == null) {
			return;
		}
		icache.put(uid, settings);
	}

	public void invalidate(String uid) {
		if (icache != null) {
			icache.invalidate(uid);
		}
	}

	public void invalidateAll() {
		if (icache != null) {
			icache.invalidateAll();
		}
	}

}
