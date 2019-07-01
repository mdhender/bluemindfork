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
package net.bluemind.icalendar.persistence;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore.EntityPopulator;
import net.bluemind.core.jdbc.JdbcAbstractStore.StatementValues;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;

public class VAlarmColumns {

	public static final Columns cols = Columns.create() //
			.col("valarm_action") //
			.col("valarm_trigger") //
			.col("valarm_description") //
			.col("valarm_duration") //
			.col("valarm_repeat") //
			.col("valarm_summary");

	public static StatementValues<ICalendarElement> values() {
		return new StatementValues<ICalendarElement>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					ICalendarElement value) throws SQLException {
				List<VAlarm> alarms = value.alarm;

				if (alarms != null && alarms.size() > 0) {
					int s = alarms.size();

					ICalendarElement.VAlarm.Action[] actions = new ICalendarElement.VAlarm.Action[s];
					Integer[] triggers = new Integer[s];
					String[] descriptions = new String[s];
					Integer[] durations = new Integer[s];
					Integer[] repeats = new Integer[s];
					String[] summaries = new String[s];

					for (int i = 0; i < s; i++) {
						VAlarm alarm = alarms.get(i);
						actions[i] = alarm.action;
						triggers[i] = alarm.trigger;
						descriptions[i] = alarm.description;
						durations[i] = alarm.duration;
						repeats[i] = alarm.repeat;
						summaries[i] = alarm.summary;
					}
					statement.setArray(index++, con.createArrayOf("t_icalendar_valarm_action", actions));
					statement.setArray(index++, con.createArrayOf("int", triggers));
					statement.setArray(index++, con.createArrayOf("text", descriptions));
					statement.setArray(index++, con.createArrayOf("int", durations));
					statement.setArray(index++, con.createArrayOf("int", repeats));
					statement.setArray(index++, con.createArrayOf("text", summaries));

				} else {
					statement.setNull(index++, Types.ARRAY);
					statement.setNull(index++, Types.ARRAY);
					statement.setNull(index++, Types.ARRAY);
					statement.setNull(index++, Types.ARRAY);
					statement.setNull(index++, Types.ARRAY);
					statement.setNull(index++, Types.ARRAY);
				}

				return index;
			}

		};
	}

	public static EntityPopulator<ICalendarElement> populator() {
		return new EntityPopulator<ICalendarElement>() {

			@Override
			public int populate(ResultSet rs, int index, ICalendarElement value) throws SQLException {

				List<VAlarm.Action> actions = arrayOfActions(rs.getArray(index++));
				Integer[] triggers = arrayOfInt(rs.getArray(index++));
				String[] descriptions = arrayOfString(rs.getArray(index++));
				Integer[] durations = arrayOfInt(rs.getArray(index++));
				Integer[] repeats = arrayOfInt(rs.getArray(index++));
				String[] summaries = arrayOfString(rs.getArray(index++));

				if (actions != null && actions.size() > 0) {
					value.alarm = new ArrayList<VAlarm>(actions.size());
					for (int i = 0; i < actions.size(); i++) {
						VAlarm alarm = VAlarm.create(actions.get(i), triggers[i], descriptions[i], durations[i],
								repeats[i], summaries[i]);
						value.alarm.add(alarm);
					}
				}

				return index;
			}

		};
	}

	private static List<VAlarm.Action> arrayOfActions(Array array) throws SQLException {
		List<VAlarm.Action> ret = null;

		if (array != null) {
			String[] values = (String[]) array.getArray();
			ret = new ArrayList<VAlarm.Action>(values.length);
			for (String value : values) {
				if (value != null) {
					ret.add(VAlarm.Action.valueOf(value));
				} else {
					ret.add(VAlarm.Action.Email);
				}
			}
		}
		return ret;
	}

	private static String[] arrayOfString(Array array) throws SQLException {
		String[] ret = null;
		if (array != null) {
			ret = (String[]) array.getArray();
		} else {
			ret = new String[0];// FIXME length
		}
		return ret;
	}

	private static Integer[] arrayOfInt(Array array) throws SQLException {
		Integer[] ret = null;
		if (array != null) {
			ret = (Integer[]) array.getArray();
		} else {
			ret = new Integer[0];// FIXME length
		}
		return ret;
	}

}
