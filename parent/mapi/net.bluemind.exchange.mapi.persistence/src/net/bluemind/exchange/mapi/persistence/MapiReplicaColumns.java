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
package net.bluemind.exchange.mapi.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore.EntityPopulator;
import net.bluemind.core.jdbc.JdbcAbstractStore.StatementValues;
import net.bluemind.exchange.mapi.api.MapiReplica;

public class MapiReplicaColumns {

	public static final Columns cols = Columns.create()//
			.col("local_replica_guid")//
			.col("logon_replica_guid")//
			.col("mailbox_guid")//
			.col("message_objects_guid")//
			.col("mailbox_uid");

	public static StatementValues<MapiReplica> values() {
		return new StatementValues<MapiReplica>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					MapiReplica value) throws SQLException {
				statement.setString(index++, value.localReplicaGuid);
				statement.setString(index++, value.logonReplicaGuid);
				statement.setString(index++, value.mailboxGuid);
				statement.setString(index++, value.messageObjectsGuid);
				statement.setString(index++, value.mailboxUid);
				return index;
			}
		};
	}

	public static EntityPopulator<MapiReplica> populator() {
		return (ResultSet rs, int index, MapiReplica value) -> {
			value.localReplicaGuid = rs.getString(index++);
			value.logonReplicaGuid = rs.getString(index++);
			value.mailboxGuid = rs.getString(index++);
			value.messageObjectsGuid = rs.getString(index++);
			value.mailboxUid = rs.getString(index++);
			return index;
		};
	}

}
