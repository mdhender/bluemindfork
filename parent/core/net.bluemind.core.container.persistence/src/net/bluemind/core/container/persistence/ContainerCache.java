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
package net.bluemind.core.container.persistence;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.rest.BmContext;

public final class ContainerCache {
	public static class Registration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register(
				ContainerCache.class,
				CacheBuilder.newBuilder()
					.recordStats()
					.expireAfterAccess(10, TimeUnit.MINUTES)
					.build()
			);
		}
	}

	private Cache<String, Container> cache;

	public ContainerCache(Cache<String, Container> cache) {
		this.cache = cache;
	}

	public static ContainerCache get(BmContext context) {
		if (context == null || context.provider().instance(CacheRegistry.class) == null) {
			return new ContainerCache(null);
		} else {
			return new ContainerCache(context.provider().instance(CacheRegistry.class).get(ContainerCache.class));
		}
	}

	public Container getIfPresent(String uid) {
		if (cache != null) {
			return cache.getIfPresent(uid);
		} else {
			return null;
		}
	}

	public void put(String uid, Container c) {
		if (cache != null) {
			cache.put(uid, c);
		}
	}

	public void invalidate(String uid) {
		if (cache != null) {
			cache.invalidate(uid);
		}
	}

}
