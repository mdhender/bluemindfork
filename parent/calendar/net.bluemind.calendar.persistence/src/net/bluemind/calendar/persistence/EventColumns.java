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

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.core.jdbc.Columns;
import net.bluemind.core.jdbc.convert.DateTimeType;

public class EventColumns {

	public static final Columns cols = Columns.create()
			//
			.col("dtend_timestamp")
			//
			.col("dtend_timezone")
			//
			.col("dtend_precision", "e_datetime_precision")
			//
			.col("transparency", "t_calendar_transparency")
			//
			.col("conference")
			//
			.col("conference_id")
			//
			.col("conference_configuration");

	public static VEventStore.StatementValues<VEvent> values() {
		return (conn, statement, index, currentRow, value) -> {

			DateTimeType.setDateTime(statement, index, value.dtend);
			index += DateTimeType.LENGTH;

			if (value.transparency != null) {
				statement.setString(index++, value.transparency.name());
			} else {
				statement.setNull(index++, Types.VARCHAR);
			}

			statement.setString(index++, value.conference);
			statement.setString(index++, value.conferenceId);
			statement.setObject(index++, value.conferenceConfiguration);

			return index;

		};
	}

	@SuppressWarnings("unchecked")
	public static VEventStore.EntityPopulator<VEvent> populator() {
		return (rs, index, value) -> {

			value.dtend = DateTimeType.getDateTime(rs, index);
			index += DateTimeType.LENGTH;

			String transparency = rs.getString(index++);
			if (transparency != null) {
				value.transparency = VEvent.Transparency.valueOf(transparency);
			}
			value.conference = rs.getString(index++);
			value.conferenceId = rs.getString(index++);
			value.conferenceConfiguration = new HashMap<String, String>();
			Map<String, String> config = (Map<String, String>) rs.getObject(index++);
			if (config != null) {
				value.conferenceConfiguration.putAll(config);
			}

			return index;
		};

	}
}