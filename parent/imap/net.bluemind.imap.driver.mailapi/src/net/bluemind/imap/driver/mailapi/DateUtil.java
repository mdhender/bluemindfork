/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package net.bluemind.imap.driver.mailapi;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public final class DateUtil {

	private static final String[] MONTH_NAME = new String[] { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug",
			"Sep", "Oct", "Nov", "Dec" };

	private static final ThreadLocal<Calendar> LOCAL_CAL = new ThreadLocal<>();

	private DateUtil() {

	}

	private static Calendar currentCal() {
		Calendar local = LOCAL_CAL.get();
		if (local == null) {
			local = new GregorianCalendar(TimeZone.getDefault());
			LOCAL_CAL.set(local);
		}
		return local;
	}

	public static String toImapDateTime(Date date) {
		return toImapDateTime(currentCal(), date);
	}

	private static String toImapDateTime(Calendar cal, Date date) {
		cal.setTime(date);

		StringBuilder sb = new StringBuilder(40);
		append2DigitNumber(sb, cal.get(Calendar.DAY_OF_MONTH)).append('-');
		sb.append(MONTH_NAME[cal.get(Calendar.MONTH)]).append('-');
		sb.append(cal.get(Calendar.YEAR)).append(' ');

		append2DigitNumber(sb, cal.get(Calendar.HOUR_OF_DAY)).append(':');
		append2DigitNumber(sb, cal.get(Calendar.MINUTE)).append(':');
		append2DigitNumber(sb, cal.get(Calendar.SECOND)).append(' ');

		sb.append(getTimezoneString(cal));
		return sb.toString();
	}

	private static String getTimezoneString(Calendar cal) {
		int tzoffset = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / 60000;
		char tzsign = tzoffset > 0 ? '+' : '-';
		tzoffset = Math.abs(tzoffset);

		StringBuilder sb = new StringBuilder(5);
		sb.append(tzsign);
		append2DigitNumber(sb, tzoffset / 60);
		append2DigitNumber(sb, tzoffset % 60);
		return sb.toString();
	}

	private static StringBuilder append2DigitNumber(StringBuilder sb, int number) {
		return sb.append((char) ('0' + number / 10)).append((char) ('0' + number % 10));
	}

}
