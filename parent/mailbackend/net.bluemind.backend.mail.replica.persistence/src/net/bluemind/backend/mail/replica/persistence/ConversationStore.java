/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.backend.mail.replica.persistence;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.AbstractItemValueStore;
import net.bluemind.core.container.persistence.LongCreator;

public class ConversationStore extends AbstractItemValueStore<InternalConversation> {

	private static final Logger logger = LoggerFactory.getLogger(ConversationStore.class);
	private Container container;

	public ConversationStore(DataSource pool, Container container) {
		super(pool);
		this.container = container;
	}

	@Override
	public void create(Item item, InternalConversation conversation) throws SQLException {
		String query = "INSERT INTO " + ConversationColumns.TABLE + " (" + ConversationColumns.COLUMNS.names()
				+ ", item_id, container_id" + ") VALUES (" + ConversationColumns.COLUMNS.values() + " , ?, ?)";
		insert(query, conversation, ConversationColumns.values(item, container.id));
	}

	@Override
	public void update(Item item, InternalConversation conversation) throws SQLException {
		String query = "UPDATE " + ConversationColumns.TABLE + " SET (" + ConversationColumns.COLUMNS.names() + ") = ("
				+ ConversationColumns.COLUMNS.values() + ")" + " WHERE item_id = ? and container_id = ?";
		update(query, conversation, ConversationColumns.values(item, container.id));
	}

	@Override
	public void delete(Item item) throws SQLException {
		String query = "DELETE FROM " + ConversationColumns.TABLE + " WHERE item_id=?";
		delete(query, new Object[] { item.id });
		logger.debug("Conversation {} deleted.", item.id);
	}

	@Override
	public InternalConversation get(Item item) throws SQLException {
		String query = "SELECT " + ConversationColumns.COLUMNS.names() + " FROM " + ConversationColumns.TABLE
				+ " WHERE item_id = ?";
		return unique(query, rs -> new InternalConversation(), ConversationColumns.populator(),
				new Object[] { item.id });
	}

	@Override
	public void deleteAll() throws SQLException {
		logger.error("deleteAll not implemented");
	}

	public Long byConversationId(long conversationId) throws SQLException {
		String query = "SELECT item_id FROM " + ConversationColumns.TABLE
				+ " WHERE container_id = ? AND conversation_id = ?";
		return unique(query, LongCreator.FIRST, Collections.emptyList(), new Object[] { container.id, conversationId });
	}

	public List<ItemV<InternalConversation>> byFolder(long folderId) throws SQLException {
		String query = "SELECT item_id, " + ConversationColumns.COLUMNS.names() + " FROM " + ConversationColumns.TABLE
				+ " WHERE container_id = ? AND messages @> '[{\"folderId\": " + folderId + "}]'::jsonb";
		return select(query, con -> new ItemV<InternalConversation>(),
				(rs, index, itemv) -> {
					itemv.itemId = rs.getLong(index++);
					itemv.value = new InternalConversation();
					return ConversationColumns.populator().populate(rs, index, itemv.value);
				}, new Object[] { container.id });

	}

	public List<Long> byMessage(long folderId, long itemId) throws SQLException {
		String query = "SELECT item_id FROM " + ConversationColumns.TABLE
				+ " WHERE container_id = ? AND '[{\"folderId\": " + folderId + ", \"itemId\": " + itemId
				+ "}]'::jsonb <@ messages";

		return select(query, LongCreator.FIRST, Collections.emptyList(), new Object[] { container.id });
	}

}
