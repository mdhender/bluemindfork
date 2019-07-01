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
import java.sql.SQLException;

import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore.StatementValues;
import net.bluemind.exchange.mapi.api.MapiFAI;

public class MapiFAIColumns {

	public static final Columns cols = Columns.create().col("folder_id").col("fai", "jsonb");

	public static StatementValues<MapiFAI> values(long id) {
		return new StatementValues<MapiFAI>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow, MapiFAI value)
					throws SQLException {
				statement.setString(index++, value.folderId);
				statement.setString(index++, value.faiJson);
				statement.setLong(index++, id);
				return index;
			}
		};
	}

}
