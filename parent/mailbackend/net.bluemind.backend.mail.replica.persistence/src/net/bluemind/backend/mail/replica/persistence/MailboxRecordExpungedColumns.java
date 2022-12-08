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
package net.bluemind.backend.mail.replica.persistence;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import net.bluemind.backend.mail.replica.api.MailboxRecordExpunged;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore.EntityPopulator;
import net.bluemind.core.jdbc.JdbcAbstractStore.StatementValues;

public class MailboxRecordExpungedColumns {

	private MailboxRecordExpungedColumns() {
	}

	public static final Columns COLUMNS = Columns.create() //
			.col("container_id")//
			.col("subtree_id")//
			.col("item_id") //
			.col("imap_uid")//
			.col("created");

	public static EntityPopulator<MailboxRecordExpunged> populator(Integer containerId, Long itemId) {
		final EntityPopulator<MailboxRecordExpunged> simple = simplePopulator();
		return (ResultSet rs, int index, MailboxRecordExpunged value) -> {
			value.containerId = containerId;
			value.itemId = itemId;
			return simple.populate(rs, index, value);
		};

	}

	public static EntityPopulator<MailboxRecordExpunged> populator(Long itemId) {
		final EntityPopulator<MailboxRecordExpunged> simple = simplePopulator();
		return (ResultSet rs, int index, MailboxRecordExpunged value) -> {
			value.itemId = itemId;
			return simple.populate(rs, index, value);
		};

	}

	public static EntityPopulator<MailboxRecordExpunged> simplePopulator() {
		return (ResultSet rs, int index, MailboxRecordExpunged value) -> {
			value.containerId = rs.getInt(index++);
			value.subtreeId = rs.getInt(index++);
			value.itemId = rs.getLong(index++);
			value.imapUid = rs.getLong(index++);
			value.created = rs.getDate(index++);
			return index;
		};
	}

	public static StatementValues<MailboxRecordExpunged> values(Long itemId) {
		return new StatementValues<MailboxRecordExpunged>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					MailboxRecordExpunged value) throws SQLException {
				statement.setInt(index++, value.containerId);
				statement.setInt(index++, value.subtreeId);
				if (itemId != null) {
					statement.setLong(index++, itemId);
				}
				statement.setLong(index++, value.imapUid);
				statement.setTimestamp(index++, value.created == null ? new Timestamp(new Date(0).getTime())
						: Timestamp.from(value.created.toInstant()));

				return index;
			}

		};
	}
}
