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
import net.bluemind.exchange.mapi.api.MapiFolder;

public class MapiFoldersColumns {

	public static final Columns cols = Columns.create()//
			.col("replica_guid")//
			.col("container_uid")//
			.col("parent_container_uid")//
			.col("display_name")//
			.col("container_class")//
	;

	public static StatementValues<MapiFolder> values() {
		return new StatementValues<MapiFolder>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					MapiFolder value) throws SQLException {
				statement.setString(index++, value.replicaGuid);
				statement.setString(index++, value.containerUid);
				statement.setString(index++, value.parentContainerUid);
				statement.setString(index++, value.displayName);
				statement.setString(index++, value.pidTagContainerClass);
				return index;
			}
		};
	}

	public static EntityPopulator<MapiFolder> populator() {
		return (ResultSet rs, int index, MapiFolder f) -> {
			f.replicaGuid = rs.getString(index++);
			f.containerUid = rs.getString(index++);
			f.parentContainerUid = rs.getString(index++);
			f.displayName = rs.getString(index++);
			f.pidTagContainerClass = rs.getString(index++);
			return index;
		};
	}

}
