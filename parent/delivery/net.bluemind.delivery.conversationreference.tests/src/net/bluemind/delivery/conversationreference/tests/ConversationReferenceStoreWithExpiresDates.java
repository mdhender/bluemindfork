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
package net.bluemind.delivery.conversationreference.tests;

import java.sql.SQLException;
import java.util.Collections;

import javax.sql.DataSource;

import net.bluemind.delivery.conversationreference.persistence.ConversationReferenceStore;

public class ConversationReferenceStoreWithExpiresDates extends ConversationReferenceStore {

	private static final String INSERT_WITH_EXPIRES_QUERY = """
			INSERT INTO t_conversationreference(mailbox_id,message_id_hash,conversation_id,expires)
			VALUES (?,?,?,now() - '2 year'::interval)
			""";

	private static final String SELECT_QUERY = """
			SELECT COUNT(*) FROM t_conversationreference;
			""";

	public ConversationReferenceStoreWithExpiresDates(DataSource dataSource) {
		super(dataSource);
	}

	public void insertWithExpires(long mailboxId, long messageIdHash, long conversationId) {
		try {
			insert(INSERT_WITH_EXPIRES_QUERY, new Object[] { mailboxId, messageIdHash, conversationId, });
		} catch (SQLException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}

	public long getNumberOfEntries() throws SQLException {
		return unique(SELECT_QUERY, con -> con.getLong(1), Collections.emptyList(), new Object[] {});
	}

}
