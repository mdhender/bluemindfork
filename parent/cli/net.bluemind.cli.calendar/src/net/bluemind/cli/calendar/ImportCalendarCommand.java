/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.cli.calendar;

import java.io.File;
import java.util.Optional;

import io.airlift.airline.Arguments;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.IVEvent;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.Regex;

@Command(name = "import", description = "import an ICS File")
public class ImportCalendarCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("calendar");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ImportCalendarCommand.class;
		}

	}

	@Arguments(required = true, description = "email address")
	public String email;

	@Option(required = true, name = "--ics-file-path", description = "The path of the ics file. ex: </tmp/my_calendar.ics>")
	public String icsFilePath;

	@Option(name = "--calendarUid", description = "calendar uid, default value is user default calendar")
	public String calendarUid;

	@Option(name = "--dry", description = "Dry-run (do nothing)")
	public boolean dry = false;

	private CliContext ctx;
	protected CliUtils cliUtils;

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

	@Override
	public void run() {
		if (!Regex.EMAIL.validate(email)) {
			throw new CliException("invalid email : " + email);
		}

		String userUid = cliUtils.getUserUidFromEmail(email);

		File file = new File(icsFilePath);
		if (!file.exists() || file.isDirectory()) {
			throw new CliException("File " + icsFilePath + " doesn't exist.");
		}

		if (calendarUid == null) {
			calendarUid = ICalendarUids.defaultUserCalendar(userUid);
		}

		if (!dry) {
			ctx.adminApi().instance(IVEvent.class, calendarUid).importIcs(cliUtils.getStreamFromFile(icsFilePath));
			ctx.info("calendar " + calendarUid + " of " + email + " was imported");
		} else {
			ctx.info("DRY : calendar " + calendarUid + " of " + email + " was imported");

		}

	}

}
