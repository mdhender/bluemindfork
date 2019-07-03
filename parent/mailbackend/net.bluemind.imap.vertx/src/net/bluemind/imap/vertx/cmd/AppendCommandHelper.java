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
package net.bluemind.imap.vertx.cmd;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class AppendCommandHelper {

	private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

	public static StringBuilder deliveryDate(StringBuilder cmd, Date delivery) {
		if (delivery == null) {
			return cmd;
		}
		cmd.append("\"");
		Calendar cal = GregorianCalendar.getInstance(GMT);
		cal.setTimeInMillis(delivery.getTime());
		int year = cal.get(Calendar.YEAR);
		if (year < 100) {
			cal.set(Calendar.YEAR, year + 2000);
		}
		DateFormat df = new SimpleDateFormat("d-MMM-yyyy HH:mm:ss Z", Locale.ENGLISH);
		df.setTimeZone(GMT);
		String formatted = df.format(cal.getTime());
		if (cal.get(Calendar.DAY_OF_MONTH) < 10) {
			cmd.append(" ");
		}
		cmd.append(formatted);
		cmd.append("\" ");
		return cmd;
	}

	public static StringBuilder flags(StringBuilder cmd, Collection<String> flags) {
		if (flags == null || flags.isEmpty()) {
			return cmd;
		}
		cmd.append("(").append(String.join(" ", flags)).append(") ");
		return cmd;
	}

}
