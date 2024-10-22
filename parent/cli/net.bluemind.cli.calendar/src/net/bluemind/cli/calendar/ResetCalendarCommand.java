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

import java.util.Optional;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.Regex;
import net.bluemind.core.api.fault.ServerFault;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * This command is here to ensure our that the default maintenance op does
 * nothing
 *
 */
@Command(name = "reset", description = "Reset a calendar")
public class ResetCalendarCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("calendar");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ResetCalendarCommand.class;
		}

	}

	@Parameters(paramLabel = "<email>", description = "email address")
	public String email;

	@Option(names = "--calendarUid", description = "calendar uid, default value is user default calendar")
	public String calendarUid;

	@Option(names = "--dry", description = "Dry-run (do nothing)")
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

		String userUid = cliUtils.getUserUidByEmail(email);

		if (calendarUid == null) {
			calendarUid = ICalendarUids.defaultUserCalendar(userUid);
		}

		try {
			if (!dry) {
				ctx.adminApi().instance(ICalendar.class, calendarUid).reset();
				ctx.info("calendar {} of {} was reseted", calendarUid, email);
			} else {
				ctx.info("calendar {} of {} was (NOT: dry mode enable) reseted", calendarUid, email);
			}
		} catch (ServerFault e) {
			throw new CliException("ERROR resseting calendar for : " + email, e);
		}
	}

}
