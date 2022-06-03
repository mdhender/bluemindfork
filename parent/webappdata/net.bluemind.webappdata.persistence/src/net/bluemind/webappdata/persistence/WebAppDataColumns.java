/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.webappdata.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.bluemind.core.container.model.Item;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.webappdata.api.WebAppData;

public class WebAppDataColumns {
	public static final Columns cols = Columns.create().col("key") //
			.col("value");

	public static WebAppDataStore.StatementValues<WebAppData> values(final Item item) {
		return new WebAppDataStore.StatementValues<WebAppData>() {

			@Override
			public int setValues(Connection conn, PreparedStatement statement, int index, int currentRow,
					WebAppData webAppData) throws SQLException {
				statement.setString(index++, webAppData.key);
				statement.setString(index++, webAppData.value);
				statement.setLong(index++, item.id);
				return index;
			}
		};
	}

	public static WebAppDataStore.EntityPopulator<WebAppData> populator() {
		return new WebAppDataStore.EntityPopulator<WebAppData>() {

			@Override
			public int populate(ResultSet rs, int index, WebAppData webAppData) throws SQLException {
				webAppData.key = rs.getString(index++);
				webAppData.value = rs.getString(index++);
				return index;
			}

		};

	}

}
