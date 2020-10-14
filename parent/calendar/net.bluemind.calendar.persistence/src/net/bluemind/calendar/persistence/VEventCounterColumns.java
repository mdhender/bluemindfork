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

import net.bluemind.calendar.api.VEventCounter;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.Columns.Column;
import net.bluemind.core.jdbc.convert.DateTimeType;
import net.bluemind.icalendar.persistence.AttendeeColumns;
import net.bluemind.icalendar.persistence.ICalendarElementColumns;
import net.bluemind.icalendar.persistence.RRuleColumns;
import net.bluemind.icalendar.persistence.VAlarmColumns;

public class VEventCounterColumns {

	public static final Columns ALL = Columns.create() //
			.cols(adapt(VEventOccurrenceColumns.ALL, "vevent")) //
			.col("originator_cn") //
			.col("originator_email");

	public static final Columns SELECT_ALL = Columns.create() //
			.cols(adapt(VEventOccurrenceColumns.ALL, "(vevent)")) //
			.col("originator_cn") //
			.col("originator_email");

	public static void appendNames(String prefix, StringBuilder query) {
		ALL.appendNames(prefix, query);
	}

	private static Columns adapt(Columns columns, String ref) {
		Columns cloned = Columns.create();
		for (Column col : columns.cols) {
			cloned.cols.add(new Column(ref + "." + col.name, col.enumType));
		}
		return cloned;
	}

	public static void appendValues(StringBuilder query) {
		ALL.appendValues(query);
	}

	public static VEventStore.StatementValues<VEventCounter> values(long id) {
		return (conn, statement, index, currentRow, value) -> {
			index = ICalendarElementColumns.values().setValues(conn, statement, index, currentRow, value.counter);
			index = VAlarmColumns.values().setValues(conn, statement, index, currentRow, value.counter);
			index = EventColumns.values().setValues(conn, statement, index, currentRow, value.counter);
			index = AttendeeColumns.values().setValues(conn, statement, index, currentRow, value.counter);
			index = RRuleColumns.values().setValues(conn, statement, index, currentRow, value.counter);
			DateTimeType.setDateTime(statement, index, ((VEventOccurrence) value.counter).recurid);
			index += DateTimeType.LENGTH;
			statement.setString(index++, value.originator.commonName);
			statement.setString(index++, value.originator.email);
			statement.setLong(index++, id);
			return index;

		};

	}

	public static VEventStore.EntityPopulator<VEventCounter> populator() {
		return (rs, index, value) -> {
			index = ICalendarElementColumns.populator().populate(rs, index, value.counter);
			index = VAlarmColumns.populator().populate(rs, index, value.counter);
			index = EventColumns.populator().populate(rs, index, value.counter);
			index = AttendeeColumns.populator().populate(rs, index, value.counter);
			index = RRuleColumns.populator().populate(rs, index, value.counter);
			value.counter.recurid = DateTimeType.getDateTime(rs, index);
			index += DateTimeType.LENGTH;
			value.originator.commonName = rs.getString(index++);
			value.originator.email = rs.getString(index++);
			return index;
		};

	}

	public static VEventStore.EntityPopulator<VEventStore.ItemUid> itemUidPopulator() {
		return (rs, index, value) -> {
			value.itemUid = rs.getString(index++);
			return index;
		};

	}

}
