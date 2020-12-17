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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.replica.service.internal;

import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.container.model.ItemValue;

public class MboxReplicasCache {
	private static final Cache<String, ItemValue<MailboxReplica>> replicas = Caffeine.newBuilder().recordStats()
			.expireAfterWrite(10, TimeUnit.MINUTES).build();

	private MboxReplicasCache() {

	}

	public static class Reg implements ICacheRegistration {

		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register("mbox.replicas.cache", replicas);
		}

	}

	public static ItemValue<MailboxReplica> byUid(String s) {
		return replicas.getIfPresent(s);
	}

	public static void cache(ItemValue<MailboxReplica> mr) {
		replicas.put(mr.uid, mr);
	}

	public static void invalidate(String uid) {
		replicas.invalidate(uid);
	}

}
