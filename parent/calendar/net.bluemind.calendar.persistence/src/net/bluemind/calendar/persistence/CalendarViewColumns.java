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
package net.bluemind.calendar.persistence;

import java.sql.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.bluemind.calendar.api.CalendarView;
import net.bluemind.calendar.api.CalendarView.CalendarViewType;
import net.bluemind.core.jdbc.Columns;

public class CalendarViewColumns {

	public static final Columns cols = Columns.create() //
			.col("label") //
			.col("type", "t_calendarview_type") //
			.col("calendars"); //

	public static final Columns forSelect = Columns.create() //
			.col("label") //
			.col("type", "t_calendarview_type") //
			.col("calendars") //
			.col("is_default"); //

	public static VEventStore.StatementValues<CalendarView> values() {
		return (conn, statement, index, currentRow, value) -> {

			statement.setString(index++, value.label);
			statement.setString(index++, value.type.name());
			statement.setArray(index++, conn.createArrayOf("text", value.calendars.toArray()));

			return index;

		};
	}

	public static VEventStore.EntityPopulator<CalendarView> populator() {
		return (rs, index, value) -> {

			value.label = rs.getString(index++);
			value.type = CalendarViewType.valueOf(rs.getString(index++));
			value.calendars = asStringList(rs.getArray(index++));
			value.isDefault = rs.getBoolean(index++);

			return index;
		};
	}

	private static List<String> asStringList(Array array) throws SQLException {
		List<String> list = null;

		if (array != null) {
			list = Collections.emptyList();
			String[] strings = (String[]) array.getArray();
			list = new ArrayList<String>(strings.length);
			for (String s : strings) {
				list.add(s);
			}
		}
		return list;
	}

}
