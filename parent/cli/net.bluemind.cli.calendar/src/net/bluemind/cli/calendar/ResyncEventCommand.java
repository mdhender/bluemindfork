/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.cli.calendar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirEntryPath;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.Organizer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "resync", description = "Resync an event")
public class ResyncEventCommand implements ICmdLet, Runnable {

	private CliContext ctx;
	private CliUtils cliUtils;

	@Option(names = "--organizerEmail", description = "Organizer email")
	public String organizerEmail;

	@Option(names = "--icsUid", description = "Event ICS UID")
	public String icsUid;

	private String domain;

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("calendar");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ResyncEventCommand.class;
		}

	}

	@Override
	public void run() {
		this.domain = cliUtils.getDomainUidByEmail(organizerEmail);
		ctx.info("Resync event {} of organizer {}", icsUid, organizerEmail);

		ICalendar service = ctx.adminApi().instance(ICalendar.class,
				ICalendarUids.defaultUserCalendar(cliUtils.getUserUidByEmail(organizerEmail)));
		List<ItemValue<VEventSeries>> seriesList = findEvent(service, organizerEmail, icsUid);
		if (!seriesList.isEmpty()) {
			ItemValue<VEventSeries> series = seriesList.get(0);
			if (!series.value.meeting()) {
				ctx.info("Event is not a meeting");
				System.exit(0);
			}

			if (!isOrganizerVersion(series)) {
				ctx.warn("Series is not the organizer version");
				System.exit(1);
			}

			syncSeriesState(series);
			syncEventState(series);
		} else {
			ctx.info("Event not found in organizers calendar");
			System.exit(0);
		}

	}

	private boolean isOrganizerVersion(ItemValue<VEventSeries> series) {
		if (series.value.main == null) {
			ctx.warn("Organizer series contains no main event");
			System.exit(1);
		}
		Organizer organizer = series.value.main.organizer;

		String eventOrganizerPath = organizer.dir.substring("bm://".length());
		return IDirEntryPath.getDomain(eventOrganizerPath).equals(domain)
				&& IDirEntryPath.getEntryUid(eventOrganizerPath).equals(cliUtils.getUserUidByEmail(organizerEmail));
	}

	private void syncSeriesState(ItemValue<VEventSeries> series) {
		for (Attendee attendee : series.value.main.attendees) {
			if (attendsToSeries(series.value, attendee)) {
				updateAttendeeSeries(series.value, attendee);
			}
		}

	}

	private void syncEventState(ItemValue<VEventSeries> series) {
		VEvent master = series.value.main;
		List<Attendee> seriesAttendees = master != null
				? master.attendees.stream().filter(a -> attendsToSeries(series.value, a)).toList()
				: Collections.emptyList();
		List<VEvent> events = series.value.flatten();

		Map<Attendee, VEventSeries> attendeeMap = new HashMap<>();

		for (VEvent evt : events) {
			for (Attendee attendee : evt.attendees) {
				if (!seriesAttendees.contains(attendee)) {
					VEventSeries attendeeSeries = new VEventSeries();
					boolean isMain = !(evt instanceof VEventOccurrence);
					if (isMain) {
						attendeeSeries.main = evt.copy();
					} else {
						attendeeSeries.occurrences = Arrays.asList((VEventOccurrence) evt.copy());
					}

					attendeeMap.merge(attendee, attendeeSeries, (existing, current) -> {
						if (isMain) {
							existing.main = current.main;
						} else {
							List<VEventOccurrence> occurrences = existing.occurrences == null ? new ArrayList<>()
									: new ArrayList<>(existing.occurrences);
							occurrences.add(current.occurrences.get(0));
							existing.occurrences = occurrences;
						}
						return existing;
					});
				}
			}

		}

		attendeeMap.forEach((attendee, attendeeSeries) -> {
			if (attendeeSeries.main != null) {
				// EXDATES
				for (VEvent evt : events) {
					if (!evt.attendees.contains(attendee)) {
						Set<BmDateTime> exdates = attendeeSeries.main.exdate != null
								? new HashSet<>(attendeeSeries.main.exdate)
								: new HashSet<>();
						BmDateTime exdate = null;
						if (evt instanceof VEventOccurrence occurrence) {
							exdate = occurrence.recurid != null ? occurrence.recurid : occurrence.dtstart;
						} else {
							exdate = evt.dtstart;
						}
						exdates.add(exdate);
						attendeeSeries.main.exdate = exdates;
					}
				}

			}
			updateAttendeeSeries(attendeeSeries, attendee);
		});

	}

	private void updateAttendeeSeries(VEventSeries series, Attendee attendee) {

		StringBuilder sb = new StringBuilder();
		ctx.info("---------------------------------------");
		sb.append("Attendee: " + attendee.dir + ":" + attendee.mailto + "\r\n");
		if (series.main != null && series.main.exdate != null) {
			for (BmDateTime exdate : series.main.exdate) {
				sb.append("EXDATE: " + exdate.toString() + "\r\n");
			}
		}
		if (series.occurrences != null) {
			for (VEventOccurrence occ : series.occurrences) {
				sb.append("Occurrence: DTSTART: " + occ.dtstart.toString() + " --> RECURID: "
						+ (occ.recurid == null ? "null" : occ.recurid.toString()) + "\r\n");
			}
		}

		ctx.info(sb.substring(0, sb.length() - 2));

		DirEntry dirEntry = resolve(attendee.dir, attendee.mailto);
		if (dirEntry == null) {
			ctx.info("Cannot resolve attendee {}:{}", attendee.dir, attendee.mailto);
			ctx.info("---------------------------------------");
			return;
		}

		ICalendar service = dirEntry.kind == Kind.USER
				? ctx.adminApi().instance(ICalendar.class, ICalendarUids.defaultUserCalendar(dirEntry.entryUid))
				: ctx.adminApi().instance(ICalendar.class, ICalendarUids.getResourceCalendar(dirEntry.entryUid));

		List<VEventSeries> resolvedSeries = new ArrayList<>();
		boolean orphanOccurrenceUpdate = false;

		if (series.main == null) {
			orphanOccurrenceUpdate = true;
			List<ItemValue<VEventSeries>> event = findEvent(service, dirEntry.email, icsUid);
			String userUidByEmail = cliUtils.getUserUidByEmail(dirEntry.email);
			String containerUid = ICalendarUids.defaultUserCalendar(userUidByEmail);
			event.forEach(evt -> {
				ctx.adminApi().instance(ICalendar.class, containerUid).delete(evt.uid, false);
			});
			for (VEventOccurrence occurrence : series.occurrences) {
				VEventSeries newSeries = new VEventSeries();
				newSeries.icsUid = icsUid;
				newSeries.occurrences = Arrays.asList(occurrence);
				resolvedSeries.add(newSeries);
			}
		} else {
			resolvedSeries.add(series);
		}

		for (VEventSeries resolved : resolvedSeries) {
			if (orphanOccurrenceUpdate) {
				ctx.info("Creating series of attendee {}:{}", attendee.dir, attendee.mailto);
				service.create(UUID.randomUUID().toString(), resolved, false);
			} else {
				List<ItemValue<VEventSeries>> event = findEvent(service, dirEntry.email, icsUid);
				if (!event.isEmpty()) {
					ctx.info("Updating series of attendee {}:{}", attendee.dir, attendee.mailto);
					service.update(event.get(0).uid, resolved, false);
				} else {
					ctx.info("Creating series of attendee {}:{}", attendee.dir, attendee.mailto);
					service.create(UUID.randomUUID().toString(), resolved, false);
				}
			}
		}

		ctx.info("---------------------------------------");
	}

	private DirEntry resolve(String dir, String mailto) throws ServerFault {
		IDirectory directory = ctx.adminApi().instance(IDirectory.class, domain);

		if (dir != null && dir.startsWith("bm://")) {
			return directory.getEntry(dir.substring("bm://".length()));
		}

		if (mailto != null) {
			return directory.getByEmail(mailto);
		}

		return null;
	}

	private boolean attendsToSeries(VEventSeries series, Attendee attendee) {
		boolean attends = true;
		List<VEvent> flatten = series.flatten();
		for (VEvent vEvent : flatten) {
			attends = attends && userAttends(vEvent, attendee);
		}
		return attends;
	}

	private boolean userAttends(VEvent event, Attendee attendee) {
		for (Attendee att : event.attendees) {
			if ((att.dir != null && att.dir.equals(attendee.dir))
					|| (att.mailto != null && att.mailto.equals(attendee.mailto))) {
				return true;
			}
		}
		return false;
	}

	private List<ItemValue<VEventSeries>> findEvent(ICalendar service, String email, String ics) {
		List<ItemValue<VEventSeries>> byIcsUid = service.getByIcsUid(ics);
		if (!byIcsUid.isEmpty()) {
			ctx.info("Found event in calendar of entry {}", email);
			return byIcsUid;
		} else {
			ctx.info("Cannot find event in calendar of entry {}", email);
		}

		return Collections.emptyList();
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

}
