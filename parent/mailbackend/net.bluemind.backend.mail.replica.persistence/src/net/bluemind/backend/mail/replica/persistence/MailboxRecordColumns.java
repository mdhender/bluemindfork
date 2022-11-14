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
import java.util.LinkedList;
import java.util.List;

import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag.System;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxRecord.InternalFlag;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore.EntityPopulator;
import net.bluemind.core.jdbc.JdbcAbstractStore.StatementValues;

public class MailboxRecordColumns {

	private MailboxRecordColumns() {
	}

	public static final Columns COLUMNS = Columns.create() //
			.col("imap_uid")//
			.col("last_updated")//
			.col("internal_date")//
			.col("system_flags")//
			.col("other_flags")//
			.col("conversation_id")//
	;

	public static List<MailboxItemFlag> extractSystemFlags(int encodedFlags) {
		List<MailboxItemFlag> decoded = new LinkedList<>();
		for (System sf : System.values()) {
			if ((encodedFlags & sf.value().value) == sf.value().value) {
				decoded.add(sf.value());
			}
		}
		return decoded;
	}

	public static EntityPopulator<MailboxRecord> populator() {
		return (ResultSet rs, int index, MailboxRecord value) -> {
			value.messageBody = rs.getString(index++);
			value.imapUid = rs.getLong(index++);
			value.modSeq = 0; // TODO: REMOVED IN BM 5.x: remove all modSeq thingy
			value.lastUpdated = rs.getTimestamp(index++);
			value.internalDate = rs.getTimestamp(index++);
			int encodedFlags = rs.getInt(index++);
			value.flags = extractSystemFlags(encodedFlags);
			value.internalFlags = InternalFlag.of(encodedFlags);
			value.flags.addAll(toList(rs.getArray(index++)).stream().map(MailboxItemFlag::new).toList());
			value.conversationId = (Long) rs.getObject(index++);
			return index;
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

	public static StatementValues<MailboxRecord> values(long subtreeContainerId, long containerId, final Item item) {
		return (Connection con, PreparedStatement statement, int index, int currentRow, MailboxRecord value) -> {
			statement.setString(index++, value.messageBody);
			statement.setLong(index++, value.imapUid);
			statement.setTimestamp(index++, Timestamp.from(value.lastUpdated.toInstant()));
			statement.setTimestamp(index++, Timestamp.from(value.internalDate.toInstant()));
			int compoundFlags = 0;
			List<String> otherFlags = new LinkedList<>();
			for (MailboxItemFlag mif : value.flags) {
				MailboxItemFlag flag = MailboxItemFlag.of(mif.flag, 0);
				if (flag.value == 0) {
					otherFlags.add(flag.flag);
				} else {
					compoundFlags |= flag.value;
				}
			}
			compoundFlags |= InternalFlag.valueOf(value.internalFlags);
			statement.setInt(index++, compoundFlags);
			statement.setArray(index++, con.createArrayOf("text", otherFlags.toArray()));
			statement.setObject(index++, value.conversationId);
			statement.setLong(index++, subtreeContainerId);
			statement.setLong(index++, containerId);
			statement.setLong(index++, item.id);
			return 0;
		};
	}

}
