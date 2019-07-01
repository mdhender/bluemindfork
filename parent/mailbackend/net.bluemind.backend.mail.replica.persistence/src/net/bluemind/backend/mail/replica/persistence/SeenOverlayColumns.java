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

import net.bluemind.backend.mail.replica.api.SeenOverlay;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore.EntityPopulator;
import net.bluemind.core.jdbc.JdbcAbstractStore.StatementValues;

public class SeenOverlayColumns {

	private SeenOverlayColumns() {
	}

	public static final Columns COLUMNS = Columns.create() //
			.col("user_id")//
			.col("unique_id")//
			.col("last_read")//
			.col("last_uid")//
			.col("last_change")//
			.col("seen_uids");

	public static EntityPopulator<SeenOverlay> populator() {
		return new EntityPopulator<SeenOverlay>() {

			@Override
			public int populate(ResultSet rs, int index, SeenOverlay value) throws SQLException {
				value.userId = rs.getString(index++);
				value.uniqueId = rs.getString(index++);
				value.lastRead = rs.getLong(index++);
				value.lastUid = rs.getLong(index++);
				value.lastChange = rs.getLong(index++);
				value.seenUids = rs.getString(index++);
				return index;
			}
		};
	}

	public static StatementValues<SeenOverlay> values() {
		return new StatementValues<SeenOverlay>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					SeenOverlay value) throws SQLException {
				statement.setString(index++, value.userId);
				statement.setString(index++, value.uniqueId);
				statement.setLong(index++, value.lastRead);
				statement.setLong(index++, value.lastUid);
				statement.setLong(index++, value.lastChange);
				statement.setString(index++, value.seenUids);
				return index;
			}
		};
	}

}
