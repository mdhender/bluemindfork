/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2020
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

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.api.flags.SystemFlag;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxRecord.InternalFlag;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore.EntityPopulator;
import net.bluemind.core.jdbc.JdbcAbstractStore.StatementValues;

public class MailboxRecordColumns {

	public static final Columns COLUMNS = Columns.create() //
			.col("imap_uid")//
			.col("mod_seq")//
			.col("last_updated")//
			.col("internal_date")//
			.col("system_flags")//
			.col("other_flags")//
	;

	public static EntityPopulator<MailboxRecord> populator() {
		return new EntityPopulator<MailboxRecord>() {

			@Override
			public int populate(ResultSet rs, int index, MailboxRecord value) throws SQLException {
				value.messageBody = rs.getString(index++);
				value.imapUid = rs.getLong(index++);
				value.modSeq = rs.getLong(index++);
				value.lastUpdated = rs.getTimestamp(index++);
				value.internalDate = rs.getTimestamp(index++);
				int encodedFlags = rs.getInt(index++);
				value.flags = SystemFlag.of(encodedFlags);
				value.internalFlags = InternalFlag.of(encodedFlags);
				value.flags.addAll(toList(rs.getArray(index++)).stream()
						.map(MailboxItemFlag::new).collect(Collectors.toList()));
				return index;
			}
		};
	}

	private static List<String> toList(Array array) throws SQLException {
		if (array != null) {
			String[] ret = (String[]) array.getArray();
			return Arrays.asList(ret);
		} else {
			return Collections.emptyList();
		}
	}

	public static StatementValues<MailboxRecord> values(final Item item) {
		return new StatementValues<MailboxRecord>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					MailboxRecord value) throws SQLException {
				statement.setString(index++, value.messageBody);
				statement.setLong(index++, value.imapUid);
				statement.setLong(index++, value.modSeq);
				statement.setTimestamp(index++, new Timestamp(value.lastUpdated.getTime()));
				statement.setTimestamp(index++, new Timestamp(value.internalDate.getTime()));
				List<SystemFlag> allSystemFlags = SystemFlag.all();
				List<SystemFlag> systemFlags = value.flags.stream().filter(f -> f.isSystem).map(systemFlag -> {
					return allSystemFlags.stream().filter(asf -> asf.flag.equals(systemFlag.flag)).findFirst().get();
				}).collect(Collectors.toList());
				int compoundFlags = SystemFlag.valueOf(systemFlags) | InternalFlag.valueOf(value.internalFlags);
				statement.setInt(index++, compoundFlags);
				List<MailboxItemFlag> otherFlags = value.flags.stream().filter(f -> !f.isSystem).collect(Collectors.toList());
				statement.setArray(index++, con.createArrayOf("text", otherFlags.toArray()));
				statement.setLong(index++, item.id);
				return 0;
			}
		};
	}

}
