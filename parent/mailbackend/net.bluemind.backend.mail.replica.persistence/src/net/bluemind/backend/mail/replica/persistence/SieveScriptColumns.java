/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.mail.replica.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.bluemind.backend.mail.replica.api.SieveScript;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore.EntityPopulator;
import net.bluemind.core.jdbc.JdbcAbstractStore.StatementValues;

public class SieveScriptColumns {

	private SieveScriptColumns() {
	}

	public static final Columns COLUMNS = Columns.create() //
			.col("user_id")//
			.col("filename")//
			.col("last_update")//
			.col("active");

	public static EntityPopulator<SieveScript> populator() {
		return new EntityPopulator<SieveScript>() {

			@Override
			public int populate(ResultSet rs, int index, SieveScript value) throws SQLException {
				value.userId = rs.getString(index++);
				value.fileName = rs.getString(index++);
				value.lastUpdate = rs.getLong(index++);
				value.isActive = rs.getBoolean(index++);
				return index;
			}
		};
	}

	public static StatementValues<SieveScript> values() {
		return new StatementValues<SieveScript>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					SieveScript value) throws SQLException {
				statement.setString(index++, value.userId);
				statement.setString(index++, value.fileName);
				statement.setLong(index++, value.lastUpdate);
				statement.setBoolean(index++, value.isActive);
				return index;
			}
		};
	}

}
