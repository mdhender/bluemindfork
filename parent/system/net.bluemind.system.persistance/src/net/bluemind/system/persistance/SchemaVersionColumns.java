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

package net.bluemind.system.persistance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.bluemind.core.jdbc.Columns;
import net.bluemind.system.persistance.SchemaVersion.UpgradePhase;

public class SchemaVersionColumns {

	public static final Columns cols = Columns.create().col("schemaversion").col("phase", "enum_upgrader_phase")
			.col("component").col("success");

	public static SchemaVersionStore.StatementValues<SchemaVersion> statementValues() {
		return new SchemaVersionStore.StatementValues<SchemaVersion>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					SchemaVersion value) throws SQLException {
				statement.setLong(index++, value.toDbSchemaVersion());
				statement.setString(index++, value.phase.name());
				statement.setString(index++, value.component);
				statement.setBoolean(index++, value.success);
				return index;
			}
		};
	}

	public static SchemaVersionStore.EntityPopulator<SchemaVersion> populator() {
		return new SchemaVersionStore.EntityPopulator<SchemaVersion>() {

			@Override
			public int populate(ResultSet rs, int index, SchemaVersion value) throws SQLException {
				value.fromDbSchemaversion(rs.getLong(index++));
				value.phase = UpgradePhase.valueOf(rs.getString(index++));
				value.component = rs.getString(index++);
				value.success = rs.getBoolean(index++);
				return index;
			}

		};
	}

}
