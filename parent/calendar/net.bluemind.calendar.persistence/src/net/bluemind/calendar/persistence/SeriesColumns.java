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

import java.util.Collections;
import java.util.Map;

import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.jdbc.Columns;

public class SeriesColumns {

	public static final Columns cols = Columns.create() //
			.col("ics_uid") //
			.col("properties");

	public static VEventStore.StatementValues<VEventSeries> values(long itemId) {
		return (conn, statement, index, currentRow, value) -> {
			statement.setString(index++, value.icsUid);
			statement.setObject(index++, value.properties);
			statement.setLong(index++, itemId);
			return index;

		};
	}

	public static VEventStore.EntityPopulator<VEventSeries> populator() {
		return (rs, index, value) -> {

			value.icsUid = rs.getString(index++);
			@SuppressWarnings("unchecked")
			Map<String, String> props = (Map<String, String>) rs.getObject(index++);
			if (props != null) {
				value.properties = props;
			} else {
				value.properties = Collections.emptyMap();
			}
			return index;
		};

	}
}