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
import net.bluemind.exchange.mapi.api.MapiRawMessage;

public class MapiRawMessageColumns {

	public static final Columns cols = Columns.create().col("content", "jsonb");

	public static StatementValues<MapiRawMessage> values(long id) {
		return new StatementValues<MapiRawMessage>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					MapiRawMessage value) throws SQLException {
				statement.setString(index++, value.contentJson);
				statement.setLong(index++, id);
				return index;
			}
		};
	}

}
