/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.smime.cacerts.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.bluemind.core.container.model.Item;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.smime.cacerts.api.SmimeCacert;

public class SmimeCacertColumns {
	public static final Columns cols = Columns.create().col("cert");

	public static SmimeCacertStore.StatementValues<SmimeCacert> values(Item item) {
		return new SmimeCacertStore.StatementValues<SmimeCacert>() {

			@Override
			public int setValues(Connection conn, PreparedStatement statement, int index, int currentRow,
					SmimeCacert value) throws SQLException {
				statement.setString(index++, value.cert);
				statement.setLong(index++, item.id);
				return index;
			}
		};
	}

	public static SmimeCacertStore.EntityPopulator<SmimeCacert> populator() {
		return new SmimeCacertStore.EntityPopulator<SmimeCacert>() {

			@Override
			public int populate(ResultSet rs, int index, SmimeCacert value) throws SQLException {
				value.cert = rs.getString(index++);
				return index;
			}

		};

	}

}
