/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.lib.elasticsearch;

import net.bluemind.lib.elasticsearch.IndexAliasMode.Mode;

public abstract class IndexAliasMapping {

	public abstract String getReadAliasByMailboxUid(String mailboxUid);

	public abstract String getWriteAliasByMailboxUid(String mailboxUid);

	public static IndexAliasMapping get() {
		return IndexAliasMode.getMode() == Mode.ONE_TO_ONE ? new OneToOneIndexAliasMapping()
				: new RingIndexAliasMapping();
	}

	public static class OneToOneIndexAliasMapping extends IndexAliasMapping {

		@Override
		public String getReadAliasByMailboxUid(String mailboxUid) {
			return getAlias(mailboxUid);
		}

		@Override
		public String getWriteAliasByMailboxUid(String mailboxUid) {
			return getAlias(mailboxUid);
		}

		private String getAlias(String mailboxUid) {
			return "mailspool_alias_" + mailboxUid;
		}

	}

	public static class RingIndexAliasMapping extends IndexAliasMapping {

		@Override
		public String getReadAliasByMailboxUid(String mailboxUid) {
			return "mailspool_ring_alias_read" + aliasPosition(mailboxUid);
		}

		@Override
		public String getWriteAliasByMailboxUid(String mailboxUid) {
			return "mailspool_ring_alias_write" + aliasPosition(mailboxUid);
		}

		private int aliasPosition(String mailboxUid) {
			int count = getMaxAliasCount();
			return (mailboxUid.hashCode() & 0xfffffff) % count;
		}

		private int getMaxAliasCount() {
			int initialTotalNumberOfIndexes = ESearchActivator.getIndexCount("mailspool");
			int aliasMultiplicator = ElasticsearchClientConfig.getMaxAliasMultiplier();
			return initialTotalNumberOfIndexes * aliasMultiplicator;
		}

	}
}
