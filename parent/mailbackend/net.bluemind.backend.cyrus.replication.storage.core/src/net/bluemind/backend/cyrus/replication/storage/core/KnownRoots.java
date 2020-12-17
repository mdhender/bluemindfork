/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.backend.cyrus.replication.storage.core;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Policy;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;

public class KnownRoots {

	public static final Set<String> validatedPartitions = ConcurrentHashMap.newKeySet();
	public static final Set<String> validatedRoots = ConcurrentHashMap.newKeySet();

	private KnownRoots() {

	}

	public static class RootsCacheReg implements ICacheRegistration {

		@Override
		public void registerCaches(CacheRegistry cr) {

			cr.register("rootsFakeCache", new Cache<Object, Object>() {

				@Override
				public Object getIfPresent(Object key) {

					return null;
				}

				@Override
				public Object get(Object key, Function<? super Object, ? extends Object> mappingFunction) {

					return null;
				}

				@Override
				public Map<Object, Object> getAllPresent(Iterable<?> keys) {

					return null;
				}

				@Override
				public void put(Object key, Object value) {
					// fine
				}

				@Override
				public void putAll(Map<? extends Object, ? extends Object> map) {
					// fine
				}

				@Override
				public void invalidate(Object key) {
					// fine
				}

				@Override
				public void invalidateAll(Iterable<?> keys) {
					// fine
				}

				@Override
				public void invalidateAll() {
					validatedPartitions.clear();
					validatedRoots.clear();
				}

				@Override
				public long estimatedSize() {

					return 0;
				}

				@Override
				public CacheStats stats() {

					return null;
				}

				@Override
				public ConcurrentMap<Object, Object> asMap() {

					return null;
				}

				@Override
				public void cleanUp() {
					// fine
				}

				@Override
				public Policy<Object, Object> policy() {

					return null;
				}
			});
		}

	}

}
