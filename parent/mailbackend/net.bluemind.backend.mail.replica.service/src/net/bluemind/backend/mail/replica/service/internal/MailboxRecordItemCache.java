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

import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.container.model.ItemValue;

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

	public static class RecordRef {

		public final String mailboxUniqueId;
		public final long imapUid;
		public final String bodyGuid;

		public RecordRef(String mailboxUniqueId, long imapUid, String bodyGuid) {
			this.mailboxUniqueId = mailboxUniqueId;
			this.imapUid = imapUid;
			this.bodyGuid = bodyGuid;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((bodyGuid == null) ? 0 : bodyGuid.hashCode());
			result = prime * result + (int) (imapUid ^ (imapUid >>> 32));
			result = prime * result + ((mailboxUniqueId == null) ? 0 : mailboxUniqueId.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RecordRef other = (RecordRef) obj;
			if (bodyGuid == null) {
				if (other.bodyGuid != null)
					return false;
			} else if (!bodyGuid.equals(other.bodyGuid))
				return false;
			if (imapUid != other.imapUid)
				return false;
			if (mailboxUniqueId == null) {
				if (other.mailboxUniqueId != null)
					return false;
			} else if (!mailboxUniqueId.equals(other.mailboxUniqueId))
				return false;
			return true;
		}

	}

	private static final Cache<RecordRef, ItemValue<MailboxRecord>> uidToItem = Caffeine.newBuilder().recordStats()
			.maximumSize(512).build();

	public static class CacheRegistration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.registerReadOnly("mail-replica-uid-to-item", uidToItem);
		}
	}

	public static Optional<ItemValue<MailboxRecord>> getAndInvalidate(RecordRef ref) {
		ItemValue<MailboxRecord> item = uidToItem.getIfPresent(ref);
		uidToItem.invalidate(ref);
		return Optional.ofNullable(item);
	}

	public static void store(String mailboxUniqueId, ItemValue<MailboxRecord> item) {
		RecordRef ref = new RecordRef(mailboxUniqueId, item.value.imapUid, item.value.messageBody);

		uidToItem.put(ref, item);
	}

}
