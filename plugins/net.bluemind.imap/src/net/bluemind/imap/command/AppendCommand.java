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
package net.bluemind.imap.command;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import net.bluemind.imap.FlagsList;
import net.bluemind.imap.impl.IMAPResponse;
import net.bluemind.utils.FileUtils;

public final class AppendCommand extends Command<Integer> {

	private final InputStream in;
	private final String mailbox;
	private final FlagsList flags;
	private final Date delivery;

	public AppendCommand(String mailbox, InputStream message, FlagsList flags) {
		this(mailbox, message, flags, null);
	}

	public AppendCommand(String mailbox, InputStream message, FlagsList flags, Date delivery) {
		this.mailbox = mailbox;
		this.in = message;
		this.flags = flags;
		this.delivery = delivery;
	}

	@Override
	protected CommandArgument buildCommand() {
		StringBuilder cmd = new StringBuilder(50);
		cmd.append("APPEND ");
		cmd.append(toUtf7(mailbox));
		cmd.append(" ");
		if (!flags.isEmpty()) {
			cmd.append(flags.toString());
			cmd.append(" ");
		}

		if (delivery != null) {
			cmd.append("\"");
			TimeZone tz = TimeZone.getTimeZone("GMT");
			Calendar cal = GregorianCalendar.getInstance(tz);
			cal.setTimeInMillis(delivery.getTime());
			int year = cal.get(Calendar.YEAR);
			if (year < 100) {
				cal.set(Calendar.YEAR, year + 2000);
			}
			DateFormat df = new SimpleDateFormat("d-MMM-yyyy HH:mm:ss Z", Locale.ENGLISH);
			df.setTimeZone(tz);
			String formatted = df.format(cal.getTime());
			if (cal.get(Calendar.DAY_OF_MONTH) < 10) {
				cmd.append(" ");
			}
			cmd.append(formatted);
			cmd.append("\" ");
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			FileUtils.transfer(in, out, true);
		} catch (IOException e) {
			logger.error("Cannot create tmp buffer for append command", e);
		}

		byte[] data = out.toByteArray();
		cmd.append("{");
		cmd.append(data.length);
		// don't literal+ for stuff bigger than 1MB
		if (data.length > 1 * 1024 * 1024) {
			cmd.append("}");
		} else {
			cmd.append("+}");
		}
		String s = cmd.toString();
		logger.debug("C: {}", s);
		return new CommandArgument(s, data);
	}

	@Override
	public void responseReceived(List<IMAPResponse> rs) {
		IMAPResponse r = rs.get(rs.size() - 1);
		if (r.isOk()) {
			String s = r.getPayload();
			int idx = s.lastIndexOf(']');
			if (idx > 0) {
				int space = s.lastIndexOf(' ', idx - 1);
				String uidParsed = s.substring(space + 1, idx);
				data = Integer.parseInt(uidParsed);
			} else {
				errorOut(rs, 0);
			}
		} else {
			errorOut(rs, -1);
		}
	}

	private final void errorOut(List<IMAPResponse> rs, int error) {
		data = error;
		for (IMAPResponse resp : rs) {
			logger.warn("S: '" + resp.getPayload() + "'");
		}
	}

}
