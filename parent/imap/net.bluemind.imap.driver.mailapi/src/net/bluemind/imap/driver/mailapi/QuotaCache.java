/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.imap.driver.mailapi;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;

public class QuotaCache {

	private static Cache<String, AtomicLong> mboxUidToAvailableSpace = build();

	public static class Reg implements ICacheRegistration {

		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register(QuotaCache.class, mboxUidToAvailableSpace);
		}

	}

	private QuotaCache() {
	}

	private static Cache<String, AtomicLong> build() {
		return Caffeine.newBuilder().expireAfterAccess(2, TimeUnit.MINUTES).recordStats().build();
	}

	public static Optional<Long> availableSpace(String mboxUid) {
		return Optional.ofNullable(mboxUidToAvailableSpace.getIfPresent(mboxUid)).map(al -> al.get());
	}

	public static void setAvailableSpace(String mboxUid, long spaceBytes) {
		mboxUidToAvailableSpace.put(mboxUid, new AtomicLong(spaceBytes));
	}

	public static void consumeBytes(String mboxUid, long spaceBytes) {
		Optional.ofNullable(mboxUidToAvailableSpace.getIfPresent(mboxUid))
				.ifPresent(al -> al.addAndGet(0L - spaceBytes));
	}

}
