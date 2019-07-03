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

import java.sql.SQLException;
import java.util.Arrays;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.exchange.mapi.api.MapiReplica;

public class MapiReplicaStore extends JdbcAbstractStore {

	private static final Logger logger = LoggerFactory.getLogger(MapiReplicaStore.class);

	public MapiReplicaStore(DataSource dataSource) {
		super(dataSource);
	}

	public void store(MapiReplica value) throws SQLException {
		String query = "INSERT INTO t_mapi_replica (" + MapiReplicaColumns.cols.names() + ") VALUES ("
				+ MapiReplicaColumns.cols.values() + ") ON CONFLICT (mailbox_uid) DO UPDATE SET ("
				+ MapiReplicaColumns.cols.names() + ")=(" + MapiReplicaColumns.cols.values() + ")";
		insert(query, value, Arrays.asList(MapiReplicaColumns.values(), MapiReplicaColumns.values()));
	}

	public void delete(String mailboxUid) throws SQLException {
		delete("DELETE FROM t_mapi_replica WHERE mailbox_uid = ?", new Object[] { mailboxUid });
	}

	public MapiReplica get(String mailboxUid) throws SQLException {
		String query = "SELECT " + MapiReplicaColumns.cols.names() + " FROM t_mapi_replica WHERE mailbox_uid=?";
		return unique(query, rs -> new MapiReplica(), MapiReplicaColumns.populator(), new Object[] { mailboxUid });
	}

	public MapiReplica byMailboxGuid(String mailboxGuid) throws SQLException {
		String query = "SELECT " + MapiReplicaColumns.cols.names() + " FROM t_mapi_replica WHERE mailbox_guid=?";
		return unique(query, rs -> new MapiReplica(), MapiReplicaColumns.populator(), new Object[] { mailboxGuid });
	}

}
