/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.core.api.date;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;

import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;

public class BmDateTimeValidator {

	public void validate(BmDateTime date) {
		if (date == null) {
			return;
		}

		if (StringUtils.isEmpty(date.iso8601)) {
			throw new ServerFault("Date is null or empty", ErrorCode.INVALID_PARAMETER);
		}

		if (date.precision == null) {
			throw new ServerFault("Precision is null", ErrorCode.INVALID_PARAMETER);
		}

		if (date.precision == Precision.DateTime) {
			validateDateTime(date);
		} else {
			validateDate(date);
		}

	}

	private void validateDateTime(BmDateTime date) {

		if (StringUtils.isEmpty(date.timezone)) {
			try {
				LocalDateTime.parse(date.iso8601, DateTimeFormatter.ISO_DATE_TIME);
			} catch (DateTimeParseException e) {
				throw new ServerFault("Fail to parse datetime ISO8601: " + date.iso8601, ErrorCode.INVALID_PARAMETER);
			}
			return;
		}

		ZonedDateTime zonedDateTime = null;
		try {
			zonedDateTime = ZonedDateTime.parse(date.iso8601, DateTimeFormatter.ISO_DATE_TIME);
		} catch (DateTimeParseException e) {
			throw new ServerFault("Fail to parse datetime ISO8601: " + date.iso8601, ErrorCode.INVALID_PARAMETER);
		}

		TimeZone zonedDateTimeTZ = TimeZone.getTimeZone(zonedDateTime.getZone());

		String tz = date.timezone;
		String translatedTz = TimezoneExtensions.translate(tz);
		if (translatedTz != null) {
			tz = translatedTz;
		}

		if (tz.startsWith("+") || tz.startsWith("-")) {
			tz = "GMT" + tz;
		}

		TimeZone dateTZ = TimeZone.getTimeZone(tz);

		long ts = Timestamp.from(zonedDateTime.toInstant()).getTime();

		if (zonedDateTimeTZ.getOffset(ts) != dateTZ.getOffset(ts)) {
			throw new ServerFault("Invalid timezone: " + date.iso8601 + " tz is not " + date.timezone,
					ErrorCode.INVALID_PARAMETER);
		}

	}

	private void validateDate(BmDateTime date) {
		try {
			LocalDate.parse(date.iso8601, DateTimeFormatter.ISO_DATE);
		} catch (DateTimeParseException e) {
			throw new ServerFault("Fail to parse date ISO8601: " + date.iso8601, ErrorCode.INVALID_PARAMETER);
		}

		if (StringUtils.isNotEmpty(date.timezone)) {
			throw new ServerFault("Precision is Date but timezone is set", ErrorCode.INVALID_PARAMETER);
		}

	}

}
