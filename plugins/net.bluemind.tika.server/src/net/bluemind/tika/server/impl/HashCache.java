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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;

public class HashCache {

	private HashCache() {
	}

	private static final Cache<String, File> hashes = Caffeine.newBuilder().recordStats()
			.expireAfterAccess(10, TimeUnit.MINUTES).maximumSize(1024)
			.removalListener((String k, File v, RemovalCause cause) -> {
				if (cause.wasEvicted()) {
					v.delete(); // NOSONAR
				}
			}).build();

	public static class CacheRegistration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register(HashCache.class, hashes);
		}
	}

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
