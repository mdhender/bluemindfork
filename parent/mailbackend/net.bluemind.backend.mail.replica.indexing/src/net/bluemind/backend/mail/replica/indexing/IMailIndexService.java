/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxFolderSearchQuery;
import net.bluemind.backend.mail.api.SearchResult;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.ShardStats;
import net.bluemind.mailbox.api.SimpleShardStats;

public interface IMailIndexService {

	public interface BulkOperation {

		void commit(boolean waitForRefresh);

	}

	public void deleteBox(ItemValue<Mailbox> box, String folderUid);

	public void cleanupFolder(ItemValue<Mailbox> box, ItemValue<MailboxFolder> folder, Set<Integer> keySet);

	public List<MailSummary> fetchSummary(ItemValue<Mailbox> box, ItemValue<MailboxFolder> f, IDSet set);

	public void syncFlags(ItemValue<Mailbox> box, ItemValue<MailboxFolder> folder, List<MailSummary> mails);

	public double getArchivedMailSum(String mailboxUid);

	/**
	 * check if alias exists, if not create it. If alias is an index, delete
	 * alias/index and create alias to mailspool
	 * 
	 * @param iServerTaskMonitor
	 * 
	 * @param entityId
	 */
	public void repairMailbox(String mailboxUid, IServerTaskMonitor iServerTaskMonitor);

	public boolean checkMailbox(String mailboxUid);

	public void createMailbox(String mailboxUid);

	public void deleteMailbox(String mailboxUid);

	default void moveMailbox(String mailboxUid, String indexName) {
		moveMailbox(mailboxUid, indexName, true);
	}

	public void moveMailbox(String mailboxUid, String indexName, boolean deleteSource);

	Set<String> getFolders(String entityId);

	public List<ShardStats> getStats();

	default List<SimpleShardStats> getLiteStats() {
		return getStats().stream().map(s -> s).collect(Collectors.toList());
	}

	BulkOperation startBulk();

	Map<String, Object> storeBody(IndexedMessageBody body);

	void storeMessage(String mailboxUniqueId, ItemValue<MailboxRecord> mail, String user, Optional<BulkOperation> bulk);

	default void storeMessage(String mailboxUniqueId, ItemValue<MailboxRecord> mail, String user) {
		storeMessage(mailboxUniqueId, mail, user, Optional.empty());
	}

	public void expunge(ItemValue<Mailbox> box, ItemValue<MailboxFolder> folder, IDSet set);

	public void deleteBodyEntries(List<String> bodyIds);

	public SearchResult searchItems(String dirEntryUid, MailboxFolderSearchQuery query);

	public long resetMailboxIndex(String mailboxUid);

}
