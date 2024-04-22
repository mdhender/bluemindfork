/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
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
package net.bluemind.imap.endpoint.cmd;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import net.bluemind.imap.endpoint.EndpointRuntimeException;

public class ImapDateParser {

	@SuppressWarnings("serial")
	public static class DateParseException extends EndpointRuntimeException {

		public DateParseException(String string) {
			super(string);
		}

	}

	private static final Map<String, Integer> MONTH_NUMBER;
	static {
		ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
		String[] names = { "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC" };
		for (int i = 0; i < names.length; i++) {
			builder.put(names[i], i);
		}
		MONTH_NUMBER = builder.build();
	}

	private ImapDateParser() {
	}

	public static Date readDateTime(String dateStr) throws DateParseException {
		return readDate(dateStr, true);
	}

	public static Date readDate(String dateStr) throws DateParseException {
		return readDate(dateStr, false);
	}

	private static Date readDate(String dateStr, boolean datetime) throws DateParseException {
		if (dateStr.length() < (datetime ? 26 : 10)) {
			if (dateStr.length() == 25) {
				// "7-Feb-1995 22:42:04 -0800" when " 7-Feb-1995 22:42:04 -0800" is expected
				// in APPEND.
				dateStr = " " + dateStr;
			} else {
				throw new EndpointRuntimeException("invalid date format");
			}
		}
		Calendar cal = new GregorianCalendar();
		cal.clear();

		int pos = 0;
		int count;
		if (datetime && dateStr.charAt(0) == ' ') {
			pos++;
		}
		count = 2 - pos - (datetime || dateStr.charAt(1) != '-' ? 0 : 1);
		validateDigits(dateStr, pos, count, cal, Calendar.DAY_OF_MONTH);
		pos += count;
		validateChar(dateStr, pos, '-');
		pos++;
		validateMonth(dateStr, pos, cal);
		pos += 3;
		validateChar(dateStr, pos, '-');
		pos++;
		validateDigits(dateStr, pos, 4, cal, Calendar.YEAR);
		pos += 4;

		if (datetime) {
			validateChar(dateStr, pos, ' ');
			pos++;
			validateDigits(dateStr, pos, 2, cal, Calendar.HOUR);
			pos += 2;
			validateChar(dateStr, pos, ':');
			pos++;
			validateDigits(dateStr, pos, 2, cal, Calendar.MINUTE);
			pos += 2;
			validateChar(dateStr, pos, ':');
			pos++;
			validateDigits(dateStr, pos, 2, cal, Calendar.SECOND);
			pos += 2;
			validateChar(dateStr, pos, ' ');
			pos++;
			boolean zonesign = dateStr.charAt(pos) == '+';
			validateChar(dateStr, pos, zonesign ? '+' : '-');
			pos++;
			int zonehrs = validateDigits(dateStr, pos, 2, cal, -1);
			pos += 2;
			int zonemins = validateDigits(dateStr, pos, 2, cal, -1);
			pos += 2;
			cal.set(Calendar.ZONE_OFFSET, (zonesign ? 1 : -1) * (60 * zonehrs + zonemins) * 60000);
			cal.set(Calendar.DST_OFFSET, 0);
		}

		if (pos != dateStr.length()) {
			throw new DateParseException("excess characters at end of date string");
		}
		return cal.getTime();
	}

	private static int validateDigits(String str, int pos, int count, Calendar cal, int field)
			throws EndpointRuntimeException {
		if (str.length() < pos + count) {
			throw new DateParseException("unexpected end of date string");
		}
		int value = 0;
		for (int i = 0; i < count; i++) {
			char c = str.charAt(pos + i);
			if (c < '0' || c > '9') {
				throw new DateParseException("invalid digit in date string");
			}
			value = value * 10 + (c - '0');
		}

		if (field >= 0) {
			cal.set(field, value);
		}
		return value;
	}

	private static void validateChar(String str, int pos, char c) throws EndpointRuntimeException {
		if (str.length() < pos + 1) {
			throw new DateParseException("unexpected end of date string");
		}
		if (str.charAt(pos) != c) {
			throw new DateParseException("unexpected character in date string");
		}
	}

	private static void validateMonth(String str, int pos, Calendar cal) throws EndpointRuntimeException {
		Integer month = MONTH_NUMBER.get(str.substring(pos, pos + 3).toUpperCase());
		if (month == null) {
			throw new DateParseException("invalid month string");
		}
		cal.set(Calendar.MONTH, month);
	}

}
