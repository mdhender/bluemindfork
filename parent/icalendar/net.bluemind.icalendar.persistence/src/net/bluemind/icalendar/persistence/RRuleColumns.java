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
import java.util.Arrays;
import java.util.List;

import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.JdbcAbstractStore.EntityPopulator;
import net.bluemind.core.jdbc.JdbcAbstractStore.StatementValues;
import net.bluemind.core.jdbc.convert.DateTimeType;
import net.bluemind.icalendar.api.ICalendarElement;

public class RRuleColumns {
	public static final Columns cols = Columns.create() //
			.col("rrule_frequency", "t_icalendar_rrule_frequency") //
			.col("rrule_interval") //
			.col("rrule_count") //
			.col("rrule_until_timestamp") //
			.col("rrule_until_timezone") //
			.col("rrule_until_precision", "e_datetime_precision") //
			.col("rrule_bySecond") //
			.col("rrule_byMinute") //
			.col("rrule_byHour") //
			.col("rrule_byDay") //
			.col("rrule_byMonthDay") //
			.col("rrule_byYearDay") //
			.col("rrule_byWeekNo") //
			.col("rrule_byMonth");

	public static StatementValues<ICalendarElement> values() {
		return new StatementValues<ICalendarElement>() {

			@Override
			public int setValues(Connection conn, PreparedStatement statement, int index, int currentRow,
					ICalendarElement value) throws SQLException {

				ICalendarElement.RRule rrule = value.rrule;

				if (rrule == null) {
					statement.setNull(index++, Types.VARCHAR); // Freq
					statement.setNull(index++, Types.INTEGER); // Interval
					statement.setNull(index++, Types.INTEGER); // Count
					statement.setNull(index++, Types.TIMESTAMP);// Until
					statement.setNull(index++, Types.VARCHAR);// Until
					statement.setNull(index++, Types.VARCHAR);// Until
					statement.setNull(index++, Types.ARRAY); // bySecond
					statement.setNull(index++, Types.ARRAY); // byMinute
					statement.setNull(index++, Types.ARRAY); // byHour
					statement.setNull(index++, Types.ARRAY); // byDay
					statement.setNull(index++, Types.ARRAY); // byMonthDay
					statement.setNull(index++, Types.ARRAY); // byYearDay
					statement.setNull(index++, Types.ARRAY); // byWeekNo
					statement.setNull(index++, Types.ARRAY); // byMonth
				} else {
					if (rrule.frequency != null) {
						statement.setString(index++, rrule.frequency.name());
					} else {
						statement.setNull(index++, Types.VARCHAR);
					}

					if (rrule.interval != null) {
						statement.setInt(index++, rrule.interval);
					} else {
						statement.setNull(index++, Types.INTEGER);
					}

					if (rrule.count != null) {
						statement.setInt(index++, rrule.count);
					} else {
						statement.setNull(index++, Types.INTEGER);
					}

					DateTimeType.setDateTime(statement, index, rrule.until);
					index += DateTimeType.LENGTH;

					if (rrule.bySecond != null) {
						statement.setArray(index++, conn.createArrayOf("int", rrule.bySecond.toArray()));
					} else {
						statement.setNull(index++, Types.ARRAY);
					}

					if (rrule.byMinute != null) {
						statement.setArray(index++, conn.createArrayOf("int", rrule.byMinute.toArray()));
					} else {
						statement.setNull(index++, Types.ARRAY);
					}

					if (rrule.byHour != null) {
						statement.setArray(index++, conn.createArrayOf("int", rrule.byHour.toArray()));
					} else {
						statement.setNull(index++, Types.ARRAY);
					}

					if (rrule.byDay != null) {
						statement.setArray(index++, conn.createArrayOf("text", rrule.byDay.toArray()));
					} else {
						statement.setNull(index++, Types.ARRAY);
					}

					if (rrule.byMonthDay != null) {
						statement.setArray(index++, conn.createArrayOf("int", rrule.byMonthDay.toArray()));
					} else {
						statement.setNull(index++, Types.ARRAY);
					}

					if (rrule.byYearDay != null) {
						statement.setArray(index++, conn.createArrayOf("int", rrule.byYearDay.toArray()));
					} else {
						statement.setNull(index++, Types.ARRAY);
					}

					if (rrule.byWeekNo != null) {
						statement.setArray(index++, conn.createArrayOf("int", rrule.byWeekNo.toArray()));
					} else {
						statement.setNull(index++, Types.ARRAY);
					}

					if (rrule.byMonth != null) {
						statement.setArray(index++, conn.createArrayOf("int", rrule.byMonth.toArray()));
					} else {
						statement.setNull(index++, Types.ARRAY);
					}
				}
				return index;
			}

		};
	}

	public static EntityPopulator<ICalendarElement> populator() {
		return new EntityPopulator<ICalendarElement>() {

			@Override
			public int populate(ResultSet rs, int index, ICalendarElement value) throws SQLException {

				ICalendarElement.RRule rrule = new ICalendarElement.RRule();
				String freq = rs.getString(index++);
				if (freq != null) {
					rrule.frequency = ICalendarElement.RRule.Frequency.valueOf(freq);

					String interval = rs.getString(index++);
					if (interval != null) {
						rrule.interval = Integer.parseInt(interval);
					}

					String count = rs.getString(index++);
					if (count != null) {
						rrule.count = Integer.parseInt(count);
					}

					rrule.until = DateTimeType.getDateTime(rs, index);
					index += DateTimeType.LENGTH;

					Array array = rs.getArray(index++);
					rrule.bySecond = asIntegerList(array);

					array = rs.getArray(index++);
					rrule.byMinute = asIntegerList(array);

					array = rs.getArray(index++);
					rrule.byHour = asIntegerList(array);

					rrule.byDay = asDayList(rs.getArray(index++));

					array = rs.getArray(index++);
					rrule.byMonthDay = asIntegerList(array);

					array = rs.getArray(index++);
					rrule.byYearDay = asIntegerList(array);

					array = rs.getArray(index++);
					rrule.byWeekNo = asIntegerList(array);

					array = rs.getArray(index++);
					rrule.byMonth = asIntegerList(array);

					value.rrule = rrule;
				} else {
					index += 13;
				}
				return index;
			}

		};

	}

	private static List<Integer> asIntegerList(Array array) throws SQLException {
		if (array != null) {
			Integer[] integers = (Integer[]) array.getArray();
			return Arrays.asList(integers);
		} else {
			return null;
		}
	}

	private static void asIntegerList(Array array, List<Integer> list) throws SQLException {
		Integer[] integers = (Integer[]) array.getArray();
		for (Integer item : integers) {
			list.add(item);
		}
	}

	private static List<ICalendarElement.RRule.WeekDay> asDayList(Array array) throws SQLException {
		List<ICalendarElement.RRule.WeekDay> ret = null;
		if (array != null) {
			String[] values = (String[]) array.getArray();
			ret = new ArrayList<ICalendarElement.RRule.WeekDay>();
			for (String value : values) {
				ret.add(new ICalendarElement.RRule.WeekDay(value));
			}
		}
		return ret;
	}
}
