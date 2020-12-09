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

package net.bluemind.system.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.bluemind.core.jdbc.Columns;
import net.bluemind.system.api.Database;
import net.bluemind.system.persistence.Upgrader.UpgradePhase;

public class UpgraderColumns {

	private UpgraderColumns() {
	}

	public static final Columns cols = Columns.create().col("server").col("phase", "enum_upgrader_phase")
			.col("database_name", "enum_database_name").col("upgrader_id").col("success");

	public static UpgraderStore.StatementValues<Upgrader> statementValues() {
		return new UpgraderStore.StatementValues<Upgrader>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow, Upgrader value)
					throws SQLException {
				statement.setString(index++, value.server);
				statement.setString(index++, value.phase.name());
				statement.setString(index++, value.database.name());
				statement.setString(index++, value.upgraderId);
				statement.setBoolean(index++, value.success);
				return index;
			}
		};
	}

	public static UpgraderStore.EntityPopulator<Upgrader> populator() {
		return new UpgraderStore.EntityPopulator<Upgrader>() {

			@Override
			public int populate(ResultSet rs, int index, Upgrader value) throws SQLException {
				value.server = rs.getString(index++);
				value.phase = UpgradePhase.valueOf(rs.getString(index++));
				value.database = Database.valueOf(rs.getString(index++));
				value.upgraderId = rs.getString(index++);
				value.success = rs.getBoolean(index++);
				return index;
			}

		};
	}

}
