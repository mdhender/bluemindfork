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

import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.IVEvent;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.directory.common.ExportCommand;
import net.bluemind.core.rest.base.GenericStream;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "export", description = "Export a calendar to an ICS file")
public class ExportCalendarCommand extends ExportCommand {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("calendar");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ExportCalendarCommand.class;
		}
	}

	@Option(names = "--calendarUid", description = "calendar uid, export all calendars if not specified")
	public String calendarUid;

	@Override
	public String getcontainerUid() {
		return calendarUid;
	}

	@Override
	public String getcontainerType() {
		return ICalendarUids.TYPE;
	}

	@Override
	public String getFileExtension() {
		return ".ics";
	}

	@Override
	public void writeFile(File outputFile, String containerUid) {
		GenericStream.streamToFile(ctx.adminApi().instance(IVEvent.class, containerUid).exportAll(), outputFile);

	}
}
