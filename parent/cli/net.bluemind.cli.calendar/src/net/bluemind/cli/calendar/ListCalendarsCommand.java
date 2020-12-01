/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
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

import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.directory.common.ListCommand;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import picocli.CommandLine.Command;

@Command(name = "list", description = "List user or whole domain calendars")
public class ListCalendarsCommand extends ListCommand {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("calendar");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ListCalendarsCommand.class;
		}
	}

	@Override
	public Kind[] getDirEntryKind() {
		return new Kind[] { Kind.CALENDAR, Kind.USER, Kind.RESOURCE };
	}

	@Override
	public String getContainerType() {
		return ICalendarUids.TYPE;
	}
}
