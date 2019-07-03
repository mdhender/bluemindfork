/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.backend.mail.replica.indexing;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxFolderSearchQuery;
import net.bluemind.backend.mail.api.SearchResult;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.ShardStats;

public class NoopMailIndexService implements IMailIndexService {

	private static final Logger logger = LoggerFactory.getLogger(NoopMailIndexService.class);

	@Override
	public void deleteBox(String latd, ItemValue<Mailbox> box, String folderUid) {
		logger.debug("NOOP");
	}

	@Override
	public void cleanupFolder(ItemValue<Mailbox> box, ItemValue<MailboxFolder> folder, Set<Integer> keySet) {
		logger.debug("NOOP");
	}

	@Override
	public List<MailSummary> fetchSummary(ItemValue<Mailbox> box, ItemValue<MailboxFolder> f, IDSet set) {
		logger.debug("NOOP");
		return Collections.emptyList();
	}

	@Override
	public void syncFlags(ItemValue<Mailbox> box, ItemValue<MailboxFolder> folder, List<MailSummary> mails) {
		logger.debug("NOOP");
	}

	@Override
	public double getArchivedMailSum(String mailboxUid) {
		logger.debug("NOOP");

		return 0;
	}

	@Override
	public void repairMailbox(String mailboxUid, IServerTaskMonitor iServerTaskMonitor) {
		logger.debug("NOOP");

	}

	@Override
	public boolean checkMailbox(String mailboxUid) {
		logger.debug("NOOP");

		return false;
	}

	@Override
	public void createMailbox(String mailboxUid) {
		logger.debug("NOOP");

	}

	@Override
	public void deleteMailbox(String mailboxUid) {
		logger.debug("NOOP");

	}

	@Override
	public void moveMailbox(String mailboxUid, String indexName) {
		logger.debug("NOOP");

	}

	@Override
	public Set<String> getFolders(String entityId) {
		logger.debug("NOOP");
		return Collections.emptySet();
	}

	@Override
	public List<ShardStats> getStats() {
		logger.debug("NOOP");
		return Collections.emptyList();
	}

	private static final BulkOperation noopBulk = new BulkOperation() {

		@Override
		public void commit(boolean waitForRefresh) {
			logger.debug("NOOP Bulk");
		}
	};

	@Override
	public BulkOperation startBulk() {
		logger.debug("NOOP");
		return noopBulk;
	}

	@Override
	public Map<String, Object> storeBody(IndexedMessageBody body) {
		logger.debug("NOOP");
		return Collections.emptyMap();
	}

	@Override
	public void storeMessage(String mailboxUniqueId, ItemValue<MailboxRecord> mail, String user,
			Optional<BulkOperation> bulk) {
		logger.debug("NOOP");

	}

	@Override
	public void expunge(String latd, ItemValue<Mailbox> box, ItemValue<MailboxFolder> folder) {
		logger.debug("NOOP");
	}

	@Override
	public void expunge(String latd, ItemValue<Mailbox> box, ItemValue<MailboxFolder> folder, IDSet set) {
		logger.debug("NOOP");
	}

	@Override
	public void deleteBodyEntries(List<String> deletedOrphanBodies) {
		logger.debug("NOOP");

	}

	@Override
	public SearchResult searchItems(String dirEntryUid, MailboxFolderSearchQuery query) {
		logger.debug("NOOP");
		return new SearchResult();
	}

}
