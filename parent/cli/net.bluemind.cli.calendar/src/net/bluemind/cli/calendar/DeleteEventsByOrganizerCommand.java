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

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.google.common.base.Strings;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEventQuery;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "delete-events-by-organizer", description = "Delete all events having a specific organizer")
public class DeleteEventsByOrganizerCommand implements ICmdLet, Runnable {

	private CliContext ctx;

	@Option(names = "--domain", required = true, description = "domain")
	public String domain;

	@Option(names = "--dry", required = false, description = "list events without deleting them")
	public boolean dry = false;

	@Parameters(paramLabel = "<email>", description = "Organizer email")
	public String organizerEmail;

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("calendar");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return DeleteEventsByOrganizerCommand.class;
		}
	}

	@Override
	public void run() {
		if (Strings.isNullOrEmpty(organizerEmail)) {
			ctx.error("Organizer email is required");
			System.exit(1);
		}
		checkCalendars(this::checkCalendar);
	}

	private void checkCalendars(Consumer<ContainerDescriptor> handler) {
		ListResult<ItemValue<DirEntry>> entries = ctx.adminApi().instance(IDirectory.class, domain)
				.search(DirEntryQuery.filterKind(Kind.USER, Kind.RESOURCE));
		IContainers containerService = ctx.adminApi().instance(IContainers.class);
		entries.values.forEach(entry -> {
			List<ContainerDescriptor> calendars = containerService
					.all(ContainerQuery.ownerAndType(entry.uid, ICalendarUids.TYPE));
			calendars.forEach(handler::accept);
		});
	}

	private void checkCalendar(ContainerDescriptor calendar) {
		ICalendar calendarService = ctx.adminApi().instance(ICalendar.class, calendar.uid);
		ListResult<ItemValue<VEventSeries>> events = calendarService.search(VEventQuery.create(organizerEmail));
		List<ItemValue<VEventSeries>> matchingEvents = events.values.stream().filter(evt -> evt.value.main != null
				&& evt.value.main.organizer.mailto != null && evt.value.main.organizer.mailto.equals(organizerEmail))
				.toList();
		if (!matchingEvents.isEmpty()) {
			ctx.info("Found " + matchingEvents.size() + " events in calendar " + calendar.uid + " of "
					+ calendar.ownerDisplayname);
			for (ItemValue<VEventSeries> matchingEvent : matchingEvents) {
				ctx.info("Event: " + matchingEvent.value.main.summary);
				if (!dry) {
					calendarService.delete(matchingEvent.uid, false);
				}
			}
		}
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}
}
