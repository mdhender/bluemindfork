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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.api.utils.Subtree;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore.EntityPopulator;
import net.bluemind.core.jdbc.JdbcAbstractStore.StatementValues;

public class SubtreeUidColumns {

	public SubtreeUidColumns() {

	}

	public static final Columns COLUMNS = Columns.create() //
			.col("mailbox_uid")//
			.col("mailbox_name")//
			.col("namespace")//
			.col("domain_uid");

	public static EntityPopulator<Subtree> populator() {
		return new EntityPopulator<Subtree>() {

			@Override
			public int populate(ResultSet rs, int index, Subtree value) throws SQLException {
				value.ownerUid = rs.getString(index++);
				value.mailboxName = rs.getString(index++);
				value.namespace = Namespace.valueOf(rs.getString(index++));
				value.domainUid = rs.getString(index++);
				return index;
			}
		};
	}

	public static StatementValues<Subtree> values() {
		return new StatementValues<Subtree>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow, Subtree value)
					throws SQLException {
				statement.setString(index++, value.ownerUid);
				statement.setString(index++, value.mailboxName);
				statement.setString(index++, value.namespace.name());
				statement.setString(index++, value.domainUid);
				return index;
			}
		};
	}

}
