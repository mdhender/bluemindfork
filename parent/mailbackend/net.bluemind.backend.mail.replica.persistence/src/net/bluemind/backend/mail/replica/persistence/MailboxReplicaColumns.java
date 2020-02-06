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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.backend.mail.replica.api.MailboxReplica.Acl;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore.EntityPopulator;
import net.bluemind.core.jdbc.JdbcAbstractStore.StatementValues;

public class MailboxReplicaColumns {

	private MailboxReplicaColumns() {
	}

	public static final Columns COLUMNS = Columns.create() //
			.col("short_name") //
			.col("parent_uid") //
			.col("name")//
			.col("last_uid")//
			.col("highest_mod_seq")//
			.col("recent_uid")//
			.col("recent_time")//
			.col("last_append_date")//
			.col("pop3_last_login")//
			.col("uid_validity")//
			.col("acls", "jsonb")//
			.col("options")//
			.col("sync_crc")//
			.col("quotaroot")//
	;

	public static EntityPopulator<MailboxReplica> populator() {
		return new EntityPopulator<MailboxReplica>() {

			@Override
			public int populate(ResultSet rs, int index, MailboxReplica value) throws SQLException {
				value.name = rs.getString(index++);
				value.parentUid = rs.getString(index++);
				value.fullName = rs.getString(index++);
				value.lastUid = rs.getLong(index++);
				value.highestModSeq = rs.getLong(index++);
				value.recentUid = rs.getLong(index++);
				value.recentTime = rs.getTimestamp(index++);
				value.lastAppendDate = rs.getTimestamp(index++);
				value.pop3LastLogin = rs.getTimestamp(index++);
				value.uidValidity = rs.getLong(index++);
				value.acls = acls(new JsonArray(rs.getString(index++)));
				value.options = rs.getString(index++);
				value.syncCRC = rs.getLong(index++);
				value.quotaRoot = rs.getString(index++);
				return index;
			}
		};
	}

	public static StatementValues<MailboxReplica> values(final Item item) {
		return new StatementValues<MailboxReplica>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					MailboxReplica value) throws SQLException {
				statement.setString(index++, value.name);
				statement.setString(index++, value.parentUid);
				statement.setString(index++, value.fullName);
				statement.setLong(index++, value.lastUid);
				statement.setLong(index++, value.highestModSeq);
				statement.setLong(index++, value.recentUid);
				statement.setTimestamp(index++,
						new Timestamp(value.recentTime == null ? 0 : value.recentTime.getTime()));
				statement.setTimestamp(index++,
						new Timestamp(value.lastAppendDate == null ? 0 : value.lastAppendDate.getTime()));
				statement.setTimestamp(index++,
						new Timestamp(value.pop3LastLogin == null ? 0 : value.pop3LastLogin.getTime()));
				statement.setLong(index++, value.uidValidity);
				statement.setString(index++, acls(value).encode());
				statement.setString(index++, value.options);
				statement.setLong(index++, value.syncCRC);
				statement.setString(index++, value.quotaRoot);
				statement.setLong(index++, item.id);
				return 0;
			}
		};
	}

	private static List<Acl> acls(JsonArray jsonArray) {
		int size = jsonArray.size();
		if (size == 0) {
			return Collections.emptyList();
		} else {
			List<Acl> ret = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				JsonObject js = jsonArray.getJsonObject(i);
				ret.add(Acl.create(js.getString("subject"), js.getString("rights")));
			}
			return ret;
		}
	}

	private static JsonArray acls(MailboxReplica replica) {
		JsonArray ret = new JsonArray();
		for (Acl acl : replica.acls) {
			ret.add(new JsonObject().put("subject", acl.subject).put("rights", acl.rights));
		}
		return ret;
	}
}
