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
package net.bluemind.todolist.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.bluemind.icalendar.persistence.AttendeeColumns;
import net.bluemind.icalendar.persistence.ICalendarElementColumns;
import net.bluemind.icalendar.persistence.RRuleColumns;
import net.bluemind.icalendar.persistence.VAlarmColumns;
import net.bluemind.todolist.api.VTodo;

public class VTodoColumns {

	public static void appendNames(String prefix, StringBuilder query) {
		ICalendarElementColumns.cols.appendNames(prefix, query);
		query.append(",");
		VAlarmColumns.cols.appendNames(prefix, query);
		query.append(",");
		TodoColumns.cols.appendNames(prefix, query);
		query.append(",");
		AttendeeColumns.cols.appendNames(prefix, query);
		query.append(",");
		RRuleColumns.cols.appendNames(prefix, query);

	}

	public static void appendValues(StringBuilder query) {
		ICalendarElementColumns.cols.appendValues(query);
		query.append(",");
		VAlarmColumns.cols.appendValues(query);
		query.append(",");
		TodoColumns.cols.appendValues(query);
		query.append(",");
		AttendeeColumns.cols.appendValues(query);
		query.append(",");
		RRuleColumns.cols.appendValues(query);

	}

	public static VTodoStore.StatementValues<VTodo> values() {
		return new VTodoStore.StatementValues<VTodo>() {

			@Override
			public int setValues(Connection conn, PreparedStatement statement, int index, int currentRow, VTodo value)
					throws SQLException {

				index = ICalendarElementColumns.values().setValues(conn, statement, index, currentRow, value);
				index = VAlarmColumns.values().setValues(conn, statement, index, currentRow, value);
				index = TodoColumns.values().setValues(conn, statement, index, currentRow, value);
				index = AttendeeColumns.values().setValues(conn, statement, index, currentRow, value);
				index = RRuleColumns.values().setValues(conn, statement, index, currentRow, value);
				return index;

			}

		};

	}

	public static VTodoStore.EntityPopulator<VTodo> populator() {
		return new VTodoStore.EntityPopulator<VTodo>() {

			@Override
			public int populate(ResultSet rs, int index, VTodo value) throws SQLException {

				index = ICalendarElementColumns.populator().populate(rs, index, value);
				index = VAlarmColumns.populator().populate(rs, index, value);
				index = TodoColumns.populator().populate(rs, index, value);
				index = AttendeeColumns.populator().populate(rs, index, value);
				index = RRuleColumns.populator().populate(rs, index, value);
				return index;
			}

		};

	}

	public static VTodoStore.EntityPopulator<VTodoStore.ItemUid> itemUidPopulator() {
		return new VTodoStore.EntityPopulator<VTodoStore.ItemUid>() {

			@Override
			public int populate(ResultSet rs, int index, VTodoStore.ItemUid value) throws SQLException {
				value.itemUid = rs.getString(index++);
				return index;
			}

		};

	}

}
