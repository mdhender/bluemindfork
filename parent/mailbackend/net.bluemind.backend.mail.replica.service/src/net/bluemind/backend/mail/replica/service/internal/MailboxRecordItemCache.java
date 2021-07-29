/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.replica.service.internal;

import java.util.Optional;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.container.model.Item;

/**
 * When restoring a bluemind installation from a kafka topic, the mailbox record
 * item to store is known.
 * 
 * Each mailbox record item read from kafka is stored here and the replication
 * use them to enforce the item id, version, created and updated date
 *
 */
public class MailboxRecordItemCache {

	private MailboxRecordItemCache() {
	}

	private static final Cache<String, Item> uidToItem = Caffeine.newBuilder().recordStats().maximumSize(512).build();

	public static class CacheRegistration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.registerReadOnly("mail-replica-uid-to-item", uidToItem);
		}
	}

	public static Optional<Item> item(String uid) {
		return Optional.ofNullable(uidToItem.getIfPresent(uid));
	}

	public static void store(String uid, Item item) {
		uidToItem.put(uid, item);
	}

	public static void invalidate(String uid) {
		if (uid != null) {
			uidToItem.invalidate(uid);
		}
	}

}
