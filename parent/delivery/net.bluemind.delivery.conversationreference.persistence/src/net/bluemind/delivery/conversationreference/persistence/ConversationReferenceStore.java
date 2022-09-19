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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.delivery.conversationreference.persistence;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.helpers.MessageFormatter;

import net.bluemind.core.container.persistence.LongCreator;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class ConversationReferenceStore extends JdbcAbstractStore {
	private static final String TABLE = "t_conversationreference";

	private static final Object[] INSERT_PARAMS = { TABLE, ConversationServiceColumns.cols.names(),
			ConversationServiceColumns.cols.values() };
	private static final String INSERT_QUERY = MessageFormatter.arrayFormat("""
			INSERT INTO {} ({}) VALUES ({})
			ON CONFLICT (mailbox_id, message_id_hash)
			DO UPDATE SET conversation_id=EXCLUDED.conversation_id
			WHERE EXCLUDED.message_id_hash != EXCLUDED.conversation_id
			RETURNING message_id_hash, conversation_id, mailbox_id
			""", INSERT_PARAMS).getMessage();

	private static final String GET_QUERY = "SELECT conversation_id FROM " + TABLE
			+ " WHERE mailbox_id = ? AND message_id_hash = ANY(?) LIMIT 1;";

	public ConversationReferenceStore(DataSource dataSource) {
		super(dataSource);
	}

	public long get(Long mailboxId, List<Long> references) throws SQLException {
		Long[] conversationIds = references.stream().toArray(Long[]::new);
		List<Long> results = select(GET_QUERY, LongCreator.FIRST, Collections.emptyList(),
				new Object[] { mailboxId, conversationIds });
		if (results.isEmpty()) {
			return 0L;
		} else {
			return results.get(0);
		}
	}

	public ConversationReference create(ConversationReference conversationReference) throws SQLException {
		var conversationRefList = create(List.of(conversationReference));
		if (conversationRefList != null) {
			return conversationRefList.get(0);
		} else {
			return null;
		}
	}

	public List<ConversationReference> create(List<ConversationReference> conversationReferences) throws SQLException {
		return batchInsertAndReturn(INSERT_QUERY, conversationReferences,
				Arrays.<StatementValues<ConversationReference>>asList((con, statement, index, rowIndex, value) -> {
					statement.setLong(index++, value.mailboxId);
					statement.setLong(index++, value.messageIdHash);
					statement.setLong(index++, value.conversationId);
					return index;
				}), new ConversationReference.ConversationReferencePopulator(), null);
	}

}
