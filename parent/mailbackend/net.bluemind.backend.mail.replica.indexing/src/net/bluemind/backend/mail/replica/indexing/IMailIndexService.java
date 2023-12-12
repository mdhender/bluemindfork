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
import java.util.Set;
import java.util.stream.Collectors;

import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.SearchResult;
import net.bluemind.backend.mail.api.utils.MailIndexQuery;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.ShardStats;
import net.bluemind.mailbox.api.SimpleShardStats;
import net.bluemind.utils.ByteSizeUnit;

public interface IMailIndexService {

	public record BulkOp(String index, String id, String routing, Map<String, Object> doc) {

	}

	public interface BulkAction {

		void commit(boolean waitForRefresh);

	}

	void deleteBox(ItemValue<Mailbox> box, String folderUid);

	List<MailSummary> fetchSummary(ItemValue<Mailbox> box, ItemValue<MailboxFolder> f, IDSet set);

	void syncFlags(ItemValue<Mailbox> box, ItemValue<MailboxFolder> folder, List<MailSummary> mails);

	/**
	 * check if alias exists, if not create it. If alias is an index, delete
	 * alias/index and create alias to mailspool
	 * 
	 * @param iServerTaskMonitor
	 * 
	 * @param entityId
	 */
	void repairMailbox(String mailboxUid, IServerTaskMonitor iServerTaskMonitor);

	boolean checkMailbox(String mailboxUid);

	void createMailbox(String mailboxUid);

	void deleteMailbox(String mailboxUid);

	default void moveMailbox(String mailboxUid, String indexName) {
		moveMailbox(mailboxUid, indexName, true);
	}

	void moveMailbox(String mailboxUid, String indexName, boolean deleteSource);

	Set<String> getFolders(String entityId);

	List<ShardStats> getStats();

	default List<SimpleShardStats> getLiteStats() {
		return getStats().stream().map(s -> s).collect(Collectors.toList());
	}

	void doBulk(List<BulkOp> operations);

	Map<String, Object> storeBody(IndexedMessageBody body);

	List<BulkOp> storeMessage(String mailboxUniqueId, ItemValue<MailboxRecord> mail, String user, boolean bulk);

	default void storeMessage(String mailboxUniqueId, ItemValue<MailboxRecord> mail, String user) {
		storeMessage(mailboxUniqueId, mail, user, false);
	}

	public Map<String, Object> fetchBody(String mailboxUniqueId, MailboxRecord value);

	void expunge(ItemValue<Mailbox> box, ItemValue<MailboxFolder> folder, IDSet set);

	void deleteBodyEntries(List<String> bodyIds);

	SearchResult searchItems(String domainUid, String dirEntryUid, MailIndexQuery query);

	long resetMailboxIndex(String mailboxUid);

	long getMailboxConsumedStorage(String userEntityId, ByteSizeUnit bsu);

	void storeBodyAsByte(String uid, byte[] body);

	void addIndexToRing(Integer numericIndex);

	void removeIndexFromRing(Integer numericIndex);

}
