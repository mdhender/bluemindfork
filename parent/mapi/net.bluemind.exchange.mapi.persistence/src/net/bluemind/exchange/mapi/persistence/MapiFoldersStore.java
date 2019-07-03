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
import net.bluemind.exchange.mapi.api.MapiFolder;

public class MapiFoldersStore extends JdbcAbstractStore {

	private static final Logger logger = LoggerFactory.getLogger(MapiFoldersStore.class);

	public MapiFoldersStore(DataSource dataSource) {
		super(dataSource);
	}

	public void store(MapiFolder value) throws SQLException {
		String query = "INSERT INTO t_mapi_folders (" + MapiFoldersColumns.cols.names() + ") VALUES ("
				+ MapiFoldersColumns.cols.values() + ") ON CONFLICT (replica_guid, container_uid) DO UPDATE SET ("
				+ MapiFoldersColumns.cols.names() + ")=(" + MapiFoldersColumns.cols.values() + ")";
		insert(query, value, Arrays.asList(MapiFoldersColumns.values(), MapiFoldersColumns.values()));
	}

	public void delete(String containerUid) throws SQLException {
		delete("DELETE FROM t_mapi_folders WHERE container_uid = ?", new Object[] { containerUid });
	}

	public MapiFolder get(String containerUid) throws SQLException {
		String query = "SELECT " + MapiFoldersColumns.cols.names() + " FROM t_mapi_folders WHERE container_uid=?";
		return unique(query, rs -> new MapiFolder(), MapiFoldersColumns.populator(), new Object[] { containerUid });
	}

}
