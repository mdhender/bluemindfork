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
package net.bluemind.backend.mail.replica.persistence;

import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.replica.api.MailboxSub;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class MailboxSubStore extends JdbcAbstractStore {

	private static final Logger logger = LoggerFactory.getLogger(MailboxSubStore.class);

	public MailboxSubStore(DataSource pool) {
		super(pool);
	}

	public void store(MailboxSub sub) throws SQLException {
		String query = "INSERT INTO t_mailbox_sub ( " + MailboxSubColumns.COLUMNS.names() + ") VALUES ("
				+ MailboxSubColumns.COLUMNS.values() + ") ON CONFLICT (" + MailboxSubColumns.COLUMNS.names()
				+ ") DO NOTHING";
		insert(query, sub, MailboxSubColumns.values());
		logger.info("Sub inserted.");
	}

	public void delete(MailboxSub sub) throws SQLException {
		String query = "DELETE FROM t_mailbox_sub where user_id=? AND mbox=?";
		delete(query, new Object[] { sub.userId, sub.mboxName });
		logger.info("Sub {} deleted.", sub);
	}

	public List<MailboxSub> byUser(String userId) throws SQLException {
		String query = "SELECT " + MailboxSubColumns.COLUMNS.names() + " FROM t_mailbox_sub WHERE user_id = ?";
		return select(query, rs -> new MailboxSub(), MailboxSubColumns.populator(), new Object[] { userId });

	}
}
