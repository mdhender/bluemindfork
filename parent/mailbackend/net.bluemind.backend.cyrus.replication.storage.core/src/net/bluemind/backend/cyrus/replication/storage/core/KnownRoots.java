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
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;
import com.google.common.collect.ImmutableMap;

import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;

public class KnownRoots {

	public static final Set<String> validatedPartitions = ConcurrentHashMap.newKeySet();
	public static final Set<String> validatedRoots = ConcurrentHashMap.newKeySet();

	public static class RootsCacheReg implements ICacheRegistration {

		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register("rootsFakeCache", new Cache<Object, Object>() {

				@Override
				public Object getIfPresent(Object key) {
					return null;
				}

				@Override
				public Object get(Object key, Callable<? extends Object> loader) throws ExecutionException {
					return null;
				}

				@Override
				public ImmutableMap<Object, Object> getAllPresent(Iterable<?> keys) {
					return null;
				}

				@Override
				public void put(Object key, Object value) {
				}

				@Override
				public void putAll(Map<? extends Object, ? extends Object> m) {
				}

				@Override
				public void invalidate(Object key) {
				}

				@Override
				public void invalidateAll(Iterable<?> keys) {
				}

				@Override
				public void invalidateAll() {
					validatedPartitions.clear();
					validatedRoots.clear();
				}

				@Override
				public long size() {
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
				}

			});
		}

	}

}
