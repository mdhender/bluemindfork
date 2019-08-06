/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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

import javax.sql.DataSource;

import net.bluemind.backend.mail.replica.api.utils.Subtree;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class SubtreeUidStore extends JdbcAbstractStore {

	public SubtreeUidStore(DataSource dataSource) {
		super(dataSource);
	}

	public void store(Subtree subtree) throws SQLException {
		String query = "INSERT INTO t_subtree_uid ( " + SubtreeUidColumns.COLUMNS.names() + ") VALUES ("
				+ SubtreeUidColumns.COLUMNS.values() + ")";
		insert(query, subtree, SubtreeUidColumns.values());
	}

	public Subtree getByMboxName(String domainUid, String mailboxName) throws SQLException {
		String query = "SELECT " + SubtreeUidColumns.COLUMNS.names()
				+ " FROM t_subtree_uid WHERE domain_uid = ? AND mailbox_name = ?";
		return unique(query, rs -> new Subtree(), SubtreeUidColumns.populator(),
				new Object[] { domainUid, mailboxName });

	}

}
