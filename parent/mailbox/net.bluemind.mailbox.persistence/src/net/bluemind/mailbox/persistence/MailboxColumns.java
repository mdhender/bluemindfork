/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.mailbox.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import net.bluemind.core.jdbc.Columns;
import net.bluemind.mailbox.api.Mailbox;

public class MailboxColumns {

	public static final Columns cols = Columns.create() //
			.col("name") //
			.col("type", "enum_mailbox_type") //
			.col("routing", "enum_mailbox_routing") //
			.col("hidden") //
			.col("system") //
			.col("archived") //
			.col("quota");

	/**
	 * @return
	 */
	public static MailboxStore.StatementValues<Mailbox> statementValues(final long itemId) {
		return new MailboxStore.StatementValues<Mailbox>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow, Mailbox m)
					throws SQLException {
				statement.setString(index++, m.name);
				statement.setString(index++, m.type.name());
				statement.setString(index++, m.routing.name());
				statement.setBoolean(index++, m.hidden);
				statement.setBoolean(index++, m.system);
				statement.setBoolean(index++, m.archived);

				if (m.quota == null) {
					statement.setNull(index++, Types.INTEGER);
				} else {
					statement.setInt(index++, m.quota);
				}

				statement.setLong(index++, itemId);
				return index;
			}
		};
	}

	public static MailboxStore.EntityPopulator<Mailbox> populator() {
		return new MailboxStore.EntityPopulator<Mailbox>() {

			@Override
			public int populate(ResultSet rs, int index, Mailbox value) throws SQLException {
				value.name = rs.getString(index++);
				value.type = Mailbox.Type.valueOf(rs.getString(index++));
				value.routing = Mailbox.Routing.valueOf(rs.getString(index++));
				value.hidden = rs.getBoolean(index++);
				value.system = rs.getBoolean(index++);
				value.archived = rs.getBoolean(index++);
				int q = rs.getInt(index++);
				if (q == 0) {
					value.quota = null;
				} else {
					value.quota = q;
				}
				value.dataLocation = rs.getString(index++);
				return index;
			}

		};
	}
}
