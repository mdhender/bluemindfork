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
package net.bluemind.authentication.service;

import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.rest.BmContext;

public class OpenIdContextCache {
	public static class Registration implements ICacheRegistration {

		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register(OpenIdContextCache.class,
					Caffeine.newBuilder().recordStats().expireAfterWrite(10, TimeUnit.MINUTES).build());
		}
	}

	private final Cache<String, OpenIdContext> cache;

	public OpenIdContextCache(Cache<String, OpenIdContext> c) {
		this.cache = c;
	}

	public static OpenIdContextCache get(BmContext context) {
		if (context == null || context.provider().instance(CacheRegistry.class) == null) {
			return new OpenIdContextCache(null);
		} else {
			return new OpenIdContextCache(
					context.provider().instance(CacheRegistry.class).get(OpenIdContextCache.class));
		}
	}

	public OpenIdContext getIfPresent(String contextId) {
		if (cache != null) {
			return cache.getIfPresent(contextId);
		} else {
			return null;
		}

	}

	public void put(String contextId, OpenIdContext c) {
		if (cache != null) {
			cache.put(contextId, c);
		}
	}

	public void invalidate(String uid) {
		if (cache != null) {
			cache.invalidate(uid);
		}
	}

}
