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

package net.bluemind.server.persistance;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.google.common.collect.Lists;

import net.bluemind.core.jdbc.Columns;
import net.bluemind.server.api.Server;

public class ServerColumns {

	public static final Columns cols = Columns.create().col("ip").col("fqdn").col("name").col("tags");

	public static ServerStore.StatementValues<Server> statementValues() {
		return new ServerStore.StatementValues<Server>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow, Server value)
					throws SQLException {
				statement.setString(index++, value.ip);
				statement.setString(index++, value.fqdn);
				statement.setString(index++, value.name);
				statement.setArray(index++, con.createArrayOf("text", value.tags.toArray()));
				return index;
			}
		};
	}

	public static ServerStore.EntityPopulator<Server> populator() {
		return new ServerStore.EntityPopulator<Server>() {

			@Override
			public int populate(ResultSet rs, int index, Server value) throws SQLException {
				value.ip = rs.getString(index++);
				value.fqdn = rs.getString(index++);
				value.name = rs.getString(index++);
				value.tags = listOfString(rs.getArray(index++));
				return index;
			}

		};
	}

	private static List<String> listOfString(Array array) throws SQLException {
		return Lists.newArrayList(arrayOfString(array));
	}

	private static String[] arrayOfString(Array array) throws SQLException {
		String[] ret = null;
		if (array != null) {
			ret = (String[]) array.getArray();
		} else {
			ret = new String[0];
		}
		return ret;
	}
}
