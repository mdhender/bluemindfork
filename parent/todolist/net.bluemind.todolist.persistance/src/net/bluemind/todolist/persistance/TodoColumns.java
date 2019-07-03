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
package net.bluemind.todolist.persistance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.convert.DateTimeType;
import net.bluemind.todolist.api.VTodo;

public class TodoColumns {
	public static final Columns cols = Columns.create().col("due_timestamp") //
			.col("due_timezone") //
			.col("due_precision", "e_datetime_precision") //
			.col("percent") //
			.col("completed_timestamp") //
			.col("completed_timezone") //
			.col("completed_precision", "e_datetime_precision") //
			.col("uid");

	public static VTodoStore.StatementValues<VTodo> values() {
		return new VTodoStore.StatementValues<VTodo>() {

			@Override
			public int setValues(Connection conn, PreparedStatement statement, int index, int currentRow, VTodo value)
					throws SQLException {

				DateTimeType.setDateTime(statement, index, value.due);
				index += DateTimeType.LENGTH;

				statement.setInt(index++, value.percent);

				DateTimeType.setDateTime(statement, index, value.completed);
				index += DateTimeType.LENGTH;

				statement.setString(index++, value.uid);
				return index;

			}

		};
	}

	public static VTodoStore.EntityPopulator<VTodo> populator() {
		return new VTodoStore.EntityPopulator<VTodo>() {

			@Override
			public int populate(ResultSet rs, int index, VTodo value) throws SQLException {

				value.due = DateTimeType.getDateTime(rs, index);
				index += DateTimeType.LENGTH;

				String percent = rs.getString(index++);
				if (percent != null) {
					value.percent = Integer.parseInt(percent);
				}

				value.completed = DateTimeType.getDateTime(rs, index);
				index += DateTimeType.LENGTH;

				value.uid = rs.getString(index++);
				return index;
			}

		};

	}

}