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

import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;

public class KnownRoots {
	private static final Cache<String, Boolean> validatedPartitions = Caffeine.newBuilder().recordStats()
			.expireAfterAccess(3, TimeUnit.HOURS).build();
	private static final Cache<String, Boolean> validatedRoots = Caffeine.newBuilder().recordStats()
			.expireAfterAccess(3, TimeUnit.HOURS).build();

	private KnownRoots() {
	}

	public static boolean isKnownPartition(String partition) {
		return validatedPartitions.getIfPresent(partition) != null;
	}

	public static boolean isKnownRoot(String root) {
		return validatedRoots.getIfPresent(root) != null;
	}

	public static void putKnownRoot(String root) {
		validatedRoots.put(root, true);
	}

	public static void putKnownParitition(String partition) {
		validatedPartitions.put(partition, true);
	}

	public static class RootsCacheReg implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register("KnownRoots.validatedPartitions", validatedPartitions);
			cr.register("KnownRoots.validatedRoots", validatedRoots);
		}
	}
}
