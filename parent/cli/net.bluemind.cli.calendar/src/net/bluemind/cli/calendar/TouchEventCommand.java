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

import java.util.List;
import java.util.Optional;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.Regex;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "touch", description = "Increment the version of a calendar event")
public class TouchEventCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("calendar");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return TouchEventCommand.class;
		}

	}

	@Parameters(paramLabel = "<email>", description = "Calendar owner email address")
	public String email;

	@Option(names = "--calendarUid", required = false, description = "Calendar uid, default value is user default calendar")
	public String calendarUid;

	@Option(names = "--icsUid", required = true, description = "ICS uid of the event to touch")
	public String icsUid;

	@Option(names = "--dry", required = false, description = "Dry-run (do nothing)")
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
			ICalendar calendarService = ctx.adminApi().instance(ICalendar.class, calendarUid);
			List<ItemValue<VEventSeries>> allSeries = calendarService.getByIcsUid(icsUid);
			if (allSeries.isEmpty()) {
				ctx.warn("Calendar {} doesn't contain ics uid '{}'", calendarUid, icsUid);
			} else {
				allSeries.stream().forEach(series -> {
					ctx.info("Incrementing version of event with id:{} uid:{}", series.internalId, series.uid);
					if (!dry) {
						calendarService.touch(series.uid);
					}
				});
			}
		} catch (ServerFault e) {
			throw new CliException("Fail to touch events with ics uid '" + icsUid + "' in calendar " + calendarUid, e);
		}
	}

}
