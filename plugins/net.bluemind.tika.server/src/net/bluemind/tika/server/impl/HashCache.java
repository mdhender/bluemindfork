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
package net.bluemind.tika.server.impl;

import java.io.File;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;

public class HashCache {
	private static final Cache<String, File> hashes = CacheBuilder.newBuilder()
			.recordStats()
			.expireAfterAccess(10, TimeUnit.MINUTES)
			.maximumSize(1024)
			.removalListener(new ExpireListener())
			.build();

	public static class CacheRegistration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register(HashCache.class, hashes);
		}
	}

	private static class ExpireListener implements RemovalListener<String, File> {
		@Override
		public void onRemoval(RemovalNotification<String, File> notification) {
			if (notification.wasEvicted()) {
				notification.getValue().delete();
			}
		}

	};

	public static File getIfPresent(String hash) {
		return hashes.getIfPresent(hash);
	}

	public static CacheStats stats() {
		return hashes.stats();
	}

	public static void put(String hash, File cachedText) {
		hashes.put(hash, cachedText);
	}

}
