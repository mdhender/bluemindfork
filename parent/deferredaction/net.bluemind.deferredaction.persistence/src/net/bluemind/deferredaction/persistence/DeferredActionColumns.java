/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.deferredaction.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore.EntityPopulator;
import net.bluemind.core.jdbc.JdbcAbstractStore.StatementValues;
import net.bluemind.deferredaction.api.DeferredAction;

public class DeferredActionColumns {

	public static final Columns cols = Columns.create() //
			.col("action_id") //
			.col("reference") //
			.col("execution_date") //
			.col("configuration");

	public static StatementValues<DeferredAction> statementValues() {
		return new StatementValues<DeferredAction>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					DeferredAction action) throws SQLException {
				statement.setString(index++, action.actionId);
				statement.setString(index++, action.reference);
				statement.setTimestamp(index++, Timestamp.from(action.executionDate.toInstant()));
				statement.setObject(index++, action.configuration);
				return index;
			}
		};
	}

	public static EntityPopulator<DeferredAction> populator() {
		return new EntityPopulator<DeferredAction>() {

			@SuppressWarnings("unchecked")
			@Override
			public int populate(ResultSet rs, int index, DeferredAction value) throws SQLException {
				value.actionId = rs.getString(index++);
				value.reference = rs.getString(index++);
				value.executionDate = rs.getTimestamp(index++);

				value.configuration = new HashMap<String, String>();
				Object configuration = rs.getObject(index++);
				if (configuration != null) {
					value.configuration.putAll((Map<String, String>) configuration);
				}

				return index;
			}

		};
	}
}
