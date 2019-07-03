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
package net.bluemind.calendar.persistance;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.icalendar.persistence.AttendeeColumns;
import net.bluemind.icalendar.persistence.ICalendarElementColumns;
import net.bluemind.icalendar.persistence.RRuleColumns;
import net.bluemind.icalendar.persistence.VAlarmColumns;

public class VEventColumns {

	public static final Columns ALL = Columns.create() //
			.cols(ICalendarElementColumns.cols) //
			.cols(VAlarmColumns.cols) //
			.cols(EventColumns.cols) //
			.cols(AttendeeColumns.cols)//
			.cols(RRuleColumns.cols);

	public static void appendNames(String prefix, StringBuilder query) {
		ALL.appendNames(prefix, query);
	}

	public static void appendValues(StringBuilder query) {
		ALL.appendValues(query);
	}

	public static VEventStore.StatementValues<VEvent> values(long id) {
		return (conn, statement, index, currentRow, value) -> {
			index = ICalendarElementColumns.values().setValues(conn, statement, index, currentRow, value);
			index = VAlarmColumns.values().setValues(conn, statement, index, currentRow, value);
			index = EventColumns.values().setValues(conn, statement, index, currentRow, value);
			index = AttendeeColumns.values().setValues(conn, statement, index, currentRow, value);
			index = RRuleColumns.values().setValues(conn, statement, index, currentRow, value);
			statement.setLong(index++, id);
			return index;

		};

	}

	public static VEventStore.EntityPopulator<VEvent> populator() {
		return (rs, index, value) -> {
			index = ICalendarElementColumns.populator().populate(rs, index, value);
			index = VAlarmColumns.populator().populate(rs, index, value);
			index = EventColumns.populator().populate(rs, index, value);
			index = AttendeeColumns.populator().populate(rs, index, value);
			index = RRuleColumns.populator().populate(rs, index, value);
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
