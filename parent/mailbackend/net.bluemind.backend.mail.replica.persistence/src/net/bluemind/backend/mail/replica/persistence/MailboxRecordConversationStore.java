/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.backend.mail.replica.persistence;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import net.bluemind.backend.mail.api.Conversation;
import net.bluemind.backend.mail.replica.persistence.internal.InternalMessageRef;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.container.persistence.LongCreator;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class MailboxRecordConversationStore extends JdbcAbstractStore {
	private final Container subtreeContainer;

	public MailboxRecordConversationStore(DataSource dataSource, Container subtreeContainer) {
		super(dataSource);
		this.subtreeContainer = subtreeContainer;
	}

	public Conversation get(String conversationUid) throws SQLException {
		Long conversationId = Long.parseUnsignedLong(conversationUid, 16);
		return Conversation.of(getMessageRefs(conversationId).stream().map(InternalMessageRef::toMessageRef).toList(),
				conversationId);
	}

	public List<Conversation> getMultiple(List<String> conversationUids) throws SQLException {
		List<InternalMessageRef> messageRefs = getMessageRefs(
				conversationUids.stream().map(uid -> Long.parseUnsignedLong(uid, 16)).toList().toArray(Long[]::new));
		List<Conversation> conversations = new ArrayList<>(conversationUids.size());
		messageRefs.stream().collect(Collectors.groupingBy(mr -> mr.conversationId)).forEach((conversationId, refs) -> {
			conversations
					.add(Conversation.of(refs.stream().map(InternalMessageRef::toMessageRef).toList(), conversationId));
		});
		return conversations;
	}

	public List<Long> getConversationIds(long folderId, SortDescriptor sorted) throws SQLException {
		StringBuilder query = new StringBuilder(
				"SELECT conversation_id FROM v_conversation_by_folder WHERE folder_id = ?");
		if (isFilteredOnNotDeletedAndImportant(sorted)) {
			query.append(" AND flagged is TRUE ");
		} else if (isFilteredOnNotDeletedAndNotSeen(sorted)) {
			query.append(" AND unseen is TRUE ");
		}

		// We always want a sort, because if we request something like "conversations,
		// sorted by sender, by date desc"
		// there is a good chance that many conversations exists with the same paramters
		var sortFieldItemId = new SortDescriptor.Field();
		sortFieldItemId.dir = SortDescriptor.Direction.Asc;
		sortFieldItemId.column = "conversation_id";

		ArrayList<SortDescriptor.Field> fields = new ArrayList<>(sorted.fields);
		fields.add(sortFieldItemId);
		// LC: not needed anymore
		// fields.stream().filter(f -> "internal_date".equals(f.column)).forEach(f ->
		// f.column = "date");
		String sort = fields.stream()
				.map(f -> f.column + " " + (f.dir == SortDescriptor.Direction.Asc ? "ASC" : "DESC"))
				.collect(Collectors.joining(","));
		query.append(" ORDER BY ").append(sort);

		return select(query.toString(), LongCreator.FIRST, Collections.emptyList(), new Object[] { folderId });
	}

	private static boolean isFilteredOnNotDeletedAndNotSeen(SortDescriptor sortDesc) {
		return sortDesc.filter != null && sortDesc.filter.mustNot.size() == 2
				&& sortDesc.filter.mustNot.stream().anyMatch(f -> f == ItemFlag.Seen)
				&& sortDesc.filter.mustNot.stream().anyMatch(f -> f == ItemFlag.Deleted);
	}

	private static boolean isFilteredOnNotDeletedAndImportant(SortDescriptor sortDesc) {
		return sortDesc.filter != null && sortDesc.filter.must.size() == 1 && sortDesc.filter.mustNot.size() == 1
				&& sortDesc.filter.must.stream().anyMatch(f -> f == ItemFlag.Important)
				&& sortDesc.filter.mustNot.stream().anyMatch(f -> f == ItemFlag.Deleted);
	}

	protected List<InternalMessageRef> getMessageRefs(Long[] conversationIds) throws SQLException {
		return select("""
				SELECT
				    item_id,
				    internal_date,
				    replace(t_container.uid, 'mbox_records_', '') AS folder_uid,
				    conversation_id
				FROM t_mailbox_record
				JOIN t_container ON (t_mailbox_record.container_id = t_container.id)
				WHERE subtree_id = ?
				AND conversation_id = ANY(?)
				AND system_flags::bit(32) & 4::bit(32) = 0::bit(32)
				""", rs -> new InternalMessageRef(), (rs, index, value) -> {
			value.itemId = rs.getInt(index++);
			value.date = rs.getTimestamp(index++);
			value.folderUid = rs.getString(index++);
			value.conversationId = rs.getLong(index++);
			return index;
		}, new Object[] { subtreeContainer.id, conversationIds });
	}

	protected List<InternalMessageRef> getMessageRefs(long conversationId) throws SQLException {
		return getMessageRefs(new Long[] { conversationId });
	}

	public List<Long> getRecordsIdsFromConversations(List<String> conversationUids) throws SQLException {
		return select("""
				SELECT item_id
				FROM t_mailbox_record
				WHERE subtree_id = ?
				AND conversation_id = ANY(?)
				AND system_flags::bit(32) & 4::bit(32) = 0::bit(32)
				""", LongCreator.FIRST, Collections.emptyList(),
				new Object[] { subtreeContainer.id,
						conversationUids.stream().map(conversationUid -> Long.parseUnsignedLong(conversationUid, 16))
								.toList().toArray(Long[]::new) });
	}
}
