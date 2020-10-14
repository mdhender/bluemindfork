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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.bluemind.backend.mail.replica.persistence.InternalConversation.InternalMessageRef;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore.EntityPopulator;
import net.bluemind.core.jdbc.JdbcAbstractStore.StatementValues;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ListReader;

public class ConversationColumns {

	private ConversationColumns() {
	}

	public static final String TABLE = "t_conversation";

	public static final Columns COLUMNS = Columns.create() //
			.col("conversation_id")//
			.col("messages", "jsonb");

	private static final ListReader<InternalMessageRef> messageIdReader = JsonUtils.listReader(InternalMessageRef.class);

	public static EntityPopulator<InternalConversation> populator() {
		return new EntityPopulator<InternalConversation>() {

			@Override
			public int populate(ResultSet rs, int index, InternalConversation value) throws SQLException {
				value.conversationId = rs.getLong(index++);
				value.messageRefs = messageIdReader.read(rs.getString(index++));
				return index;
			}
		};
	}

	public static StatementValues<InternalConversation> values(Item item, long containerId) {
		return new StatementValues<InternalConversation>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					InternalConversation value) throws SQLException {
				statement.setLong(index++, value.conversationId);
				statement.setString(index++, JsonUtils.asString(value.messageRefs));
				statement.setLong(index++, item.id);
				statement.setLong(index++, containerId);
				return index;
			}
		};
	}

}
