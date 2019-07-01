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

import net.bluemind.backend.mail.replica.api.QuotaRoot;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore.EntityPopulator;
import net.bluemind.core.jdbc.JdbcAbstractStore.StatementValues;

public class QuotaColumns {

	public static final Columns COLUMNS = Columns.create() //
			.col("root")//
			.col("limit_kb");

	public static EntityPopulator<QuotaRoot> populator() {
		return new EntityPopulator<QuotaRoot>() {

			@Override
			public int populate(ResultSet rs, int index, QuotaRoot value) throws SQLException {
				value.root = rs.getString(index++);
				value.limit = rs.getInt(index++);
				return index;
			}
		};
	}

	public static StatementValues<QuotaRoot> values() {
		return new StatementValues<QuotaRoot>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					QuotaRoot value) throws SQLException {
				statement.setString(index++, value.root);
				statement.setInt(index++, value.limit);
				return index;
			}
		};
	}

}
