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

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Iterables;

import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;

public final class ContainerCache {

	public static class Registration implements ICacheRegistration {
		private static final long duration = 10;
		private static final TimeUnit unit = TimeUnit.MINUTES;

		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register("ContainerUidCache",
					Caffeine.newBuilder().recordStats().expireAfterAccess(duration, unit).build());
			BmContext suCtx = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();
			for (DataSource ds : Iterables.concat(Collections.singleton(suCtx.getDataSource()),
					suCtx.getAllMailboxDataSource())) {
				cr.register("ContainerIdCache-" + suCtx.dataSourceLocation(ds),
						Caffeine.newBuilder().recordStats().expireAfterAccess(duration, unit).build());
			}
		}
	}

	private Cache<String, Container> uidCache;
	private Cache<Long, Container> idCache;

	public ContainerCache(Cache<String, Container> uidCache, Cache<Long, Container> idCache) {
		this.uidCache = uidCache;
		this.idCache = idCache;
	}

	public static ContainerCache get(BmContext context, DataSource dataSource) {
		if (context == null || context.provider().instance(CacheRegistry.class) == null) {
			return new ContainerCache(null, null);
		} else {
			CacheRegistry cacheRegistry = context.provider().instance(CacheRegistry.class);
			String lookupKey = "ContainerIdCache-" + context.dataSourceLocation(dataSource);
			Cache<Long, Container> lookup = cacheRegistry.get(lookupKey);
			return new ContainerCache(cacheRegistry.get("ContainerUidCache"), lookup);
		}
	}

	public Container getIfPresent(String uid) {
		if (uidCache != null) {
			return uidCache.getIfPresent(uid);
		} else {
			return null;
		}
	}

	public Container getIfPresent(long id) {
		if (idCache != null) {
			return idCache.getIfPresent(id);
		} else {
			return null;
		}
	}

	public void put(String uid, long id, Container c) {
		if (uidCache != null) {
			uidCache.put(uid, c);
		}
		if (idCache != null) {
			idCache.put(id, c);
		}
	}

	public void invalidate(String uid, long id) {
		if (uidCache != null) {
			uidCache.invalidate(uid);
		}
		if (idCache != null) {
			idCache.invalidate(id);
		}
	}

}
