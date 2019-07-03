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
package net.bluemind.core.jdbc.convert;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;

public final class DateTimeType {

	private static final Logger logger = LoggerFactory.getLogger(DateTimeType.class);
	private static final String FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
	public static final int LENGTH = 3;

	public static void setDateTime(PreparedStatement statement, int index, BmDateTime value) throws SQLException {

		if (value == null) {
			statement.setNull(index++, Types.TIMESTAMP);
			statement.setNull(index++, Types.VARCHAR);
			statement.setNull(index++, Types.VARCHAR);
			return;
		}

		statement.setTimestamp(index++, DateTimeType.asTimestamp(value));
		if (value.timezone != null) {
			LoggerFactory.getLogger(DateTimeType.class).debug("TZ: {}", value.timezone);

			statement.setString(index++, value.timezone);
		} else {
			statement.setNull(index++, Types.VARCHAR);
		}
		statement.setString(index++, value.precision.name());
	}

	public static BmDateTime getDateTime(ResultSet rs, int index) throws SQLException {
		Timestamp ts = rs.getTimestamp(index++);

		if (ts == null) {
			return null;
		}

		String tz = rs.getString(index++);
		BmDateTime dt = fromTimestamp(ts, tz, rs.getString(index++));
		return dt;
	}

	public static void setDateTimeArray(Connection conn, PreparedStatement statement, int index, Set<BmDateTime> values)
			throws SQLException {

		if (values == null || values.isEmpty()) {
			statement.setNull(index++, Types.ARRAY);
			statement.setNull(index++, Types.ARRAY);
			statement.setNull(index++, Types.ARRAY);
			return;
		}

		Timestamp[] timestamps = new Timestamp[values.size()];
		/** There is only one time zone / precision per time stamp array */
		String timezone = null;
		BmDateTime.Precision precision = null;
		int i = 0;
		for (BmDateTime dt : values) {
			timestamps[i++] = DateTimeType.asTimestamp(dt);
			if (precision == null) {
				timezone = dt.timezone;
				precision = dt.precision;
			}
		}

		statement.setArray(index++, conn.createArrayOf("timestamp", timestamps));
		if (timezone != null) {
			statement.setString(index++, timezone);
		} else {
			statement.setNull(index++, Types.VARCHAR);
		}
		if (precision != null) {
			statement.setString(index++, precision.name());
		}
	}

	public static Set<BmDateTime> getDateTimes(ResultSet rs, int index) throws SQLException {
		Array array = rs.getArray(index++);
		if (array == null) {
			return null;
		}
		Set<BmDateTime> set = Collections.emptySet();

		Timestamp[] dates = (Timestamp[]) array.getArray();
		String timezone = rs.getString(index++);
		String precision = rs.getString(index++);

		set = new HashSet<BmDateTime>(dates.length);
		for (int i = 0; i < dates.length; i++) {
			BmDateTime dateWithTimeZone = fromTimestamp(dates[i], timezone, precision);
			set.add(dateWithTimeZone);
		}
		return set;
	}

	public static BmDateTime fromTimestamp(Timestamp date, String timezone, String precision) {
		String iso8601 = date.toLocalDateTime().toString();
		return BmDateTimeWrapper.create(iso8601, timezone, Precision.valueOf(precision));
	}

	public static Timestamp asTimestamp(BmDateTime value) {
		String formatted = new BmDateTimeWrapper(value).format(FORMAT);
		try {
			return Timestamp.valueOf(formatted);
		} catch (IllegalArgumentException e) {
			logger.error("illegal argument: '{}' for '{}'", e.getMessage(), formatted);
			throw new ServerFault(e.getMessage(), ErrorCode.SQL_ERROR);
		}
	}

}
