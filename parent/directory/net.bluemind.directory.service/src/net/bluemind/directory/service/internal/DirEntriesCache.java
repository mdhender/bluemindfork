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
package net.bluemind.directory.service.internal;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.DirEntry;

public class DirEntriesCache {

	public static class Registration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			logger.info("Registered DirEntries cache");
			cr.register(DirEntriesCache.class,
					CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).build());
		}
	}

	private final Cache<String, ItemValue<DirEntry>> dirCache;
	private static final Logger logger = LoggerFactory.getLogger(DirEntriesCache.class);

	private final String domainUid;

	public DirEntriesCache(Cache<String, ItemValue<DirEntry>> dirCache) {
		this("global.virt", dirCache);
	}

	public DirEntriesCache(String domainUid, Cache<String, ItemValue<DirEntry>> dirCache) {
		this.domainUid = domainUid;
		this.dirCache = dirCache;
	}

	public static DirEntriesCache get(BmContext context, String domainUid) {
		if (context == null || context.provider().instance(CacheRegistry.class) == null) {
			return new DirEntriesCache(domainUid, null);
		} else {
			return new DirEntriesCache(domainUid,
					context.provider().instance(CacheRegistry.class).get(DirEntriesCache.class));
		}
	}

	private String key(String uid) {
		return uid + "@" + domainUid;
	}

	public ItemValue<DirEntry> get(String uid, Supplier<ItemValue<DirEntry>> de) {
		if (dirCache == null) {
			return de.get();
		}
		String key = key(uid);
		ItemValue<DirEntry> ret = dirCache.getIfPresent(key);
		if (ret == null) {
			ret = de.get();
			if (ret != null) {
				dirCache.put(key, ret);
			}
		}
		return ret;
	}

	public void cache(ItemValue<DirEntry> item) {
		if (dirCache == null) {
			return;
		}
		dirCache.put(key(item.uid), item);
	}

	public void invalidate(String uid) {
		if (dirCache == null) {
			return;
		}
		dirCache.invalidate(key(uid));
	}

	public void invalidateAll() {
		if (dirCache == null) {
			return;
		}
		dirCache.invalidateAll();
	}
}
