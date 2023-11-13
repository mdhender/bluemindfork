/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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

package net.bluemind.calendar.service.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlogs.AuditLogUpdateStatus;
import net.bluemind.core.auditlogs.ContentElement;
import net.bluemind.core.auditlogs.ContentElement.ContentElementBuilder;
import net.bluemind.core.auditlogs.ILogMapperProvider;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.RRule;
import net.bluemind.icalendar.api.ICalendarElement.RRule.WeekDay;

public class CalendarAuditLogMapper implements ILogMapperProvider<VEventSeries> {
	private static final String CRLF = "\r\n";
	private static final Logger logger = LoggerFactory.getLogger(CalendarAuditLogMapper.class);

	@Override
	public ContentElement createContentElement(VEventSeries newValue) {
		return buildContent(newValue);
	}

	@Override
	public AuditLogUpdateStatus createUpdateMessage(VEventSeries oldValue, VEventSeries newValue) {
		if (oldValue != null) {
			return new AuditLogUpdateStatus(computeUpdateDifference(oldValue, newValue));
		}
		return new AuditLogUpdateStatus();
	}

	private String computeUpdateDifference(VEventSeries oldValue, VEventSeries newValue) {
		StringBuilder sBuilder = new StringBuilder();
		VEvent oldEvent = (oldValue.main != null) ? oldValue.main : oldValue.occurrences.get(0);
		VEvent newEvent = (newValue.main != null) ? newValue.main : newValue.occurrences.get(0);
		if (oldEvent.dtend != null && newEvent.dtend != null && !oldEvent.dtend.equals(newEvent.dtend)) {
			sBuilder.append("event end date changed: '" + oldEvent.dtend.iso8601 + "' -> '" + newEvent.dtend.iso8601
					+ "'" + CRLF);
		}

		if (oldEvent.dtstart != null && newEvent.dtstart != null && !oldEvent.dtstart.equals(newEvent.dtstart)) {
			sBuilder.append("event start date changed: '" + oldEvent.dtstart.iso8601 + "' -> '"
					+ newEvent.dtstart.iso8601 + "'" + CRLF);
		}

		if (oldEvent.attendees != null && newEvent.attendees != null) {

			List<ICalendarElement.Attendee> removedAttendees = ICalendarElement.diff(oldEvent.attendees,
					newEvent.attendees);
			if (!removedAttendees.isEmpty()) {
				String removed = removedAttendees.stream().map(a -> a.mailto).collect(Collectors.joining(","));
				sBuilder.append("removed attendees: '" + removed + "'" + CRLF);

			}
			List<ICalendarElement.Attendee> addedAttendees = ICalendarElement.diff(newEvent.attendees,
					oldEvent.attendees);
			if (!addedAttendees.isEmpty()) {
				String added = addedAttendees.stream().map(a -> a.mailto).collect(Collectors.joining(","));
				sBuilder.append("added attendees: '" + added + "'" + CRLF);

			}

			// Track attendees partStatus change
			List<Attendee> sameAttendees = ICalendarElement.same(newEvent.attendees, oldEvent.attendees);
			for (Attendee att : sameAttendees) {
				ICalendarElement.ParticipationStatus oldPartStatus = ICalendarElement.get(oldEvent.attendees,
						att).partStatus;
				ICalendarElement.ParticipationStatus newPartStatus = ICalendarElement.get(newEvent.attendees,
						att).partStatus;
				if (oldPartStatus != newPartStatus) {
					sBuilder.append(att.mailto + ": participation status changed from '" + oldPartStatus + "' to '"
							+ newPartStatus + "'" + CRLF);
				}
			}
		}

		if (newEvent.location != null && oldEvent.location != null && !newEvent.location.equals(oldEvent.location)) {
			sBuilder.append(
					"event location changed: '" + oldEvent.location + "' -> '" + newEvent.location + "'" + CRLF);
		}

		if (newEvent.description != null && oldEvent.description != null
				&& !newEvent.description.equals(oldEvent.description)) {
			sBuilder.append("event description changed: '" + oldEvent.description + "' -> '" + newEvent.description
					+ "'" + CRLF);
			sBuilder.append(';');
		}

		if (newEvent.hasRecurrence() || oldEvent.hasRecurrence()) {
			String reccurenceMessage = createReccurenceUpdateMessage(oldEvent.rrule, newEvent.rrule);
			sBuilder.append(reccurenceMessage);
		}

		return sBuilder.toString();
	}

	private ContentElement buildContent(VEventSeries value) {
		ContentElementBuilder builder = new ContentElement.ContentElementBuilder();
		builder.key(value.icsUid);
		if (value.main == null && value.occurrences.isEmpty()) {
			return null;
		}

		VEvent event = (value.main != null) ? value.main : value.occurrences.get(0);

		builder.description(event.summary);
		List<String> attendees = new ArrayList<>();
		List<String> has = new ArrayList<>();
		if (event.attendees != null) {
			attendees.addAll(event.attendees.stream().map(a -> a.mailto.trim()).toList());
			attendees.addAll(event.attendees.stream().map(a -> a.commonName.trim()).toList());
			builder.with(attendees);
		}
		if (event.organizer != null) {
			List<String> organizers = Arrays.asList(event.organizer.mailto.trim(), event.organizer.commonName.trim());
			builder.author(organizers);
			attendees.addAll(organizers);
		}

		List<String> is = new ArrayList<>();
		if (event.dtstart != null) {
			is.add("dtstart:" + (event.dtstart.toString()));
		}
		if (event.dtend != null) {
			is.add("dtendt:" + (event.dtend.toString()));
		}
		if (!is.isEmpty()) {
			builder.is(is);
		}
		if (!has.isEmpty()) {
			builder.has(has);
		}

		try {
			String source = JsonUtils.asString(value);
			builder.newValue(source);
		} catch (ServerFault e) {
			logger.error(e.getMessage());
			e.printStackTrace();

		}
		return builder.build();
	}

	private String createReccurenceUpdateMessage(RRule oldRules, RRule newRules) {

		if (oldRules == null && newRules == null) {
			return "";
		}

		StringBuilder sBuilder = new StringBuilder();
		if (oldRules == null && newRules != null) {
			sBuilder.append("Added reccurence rules: " + newRules);
			return sBuilder.toString();
		}

		if (oldRules != null && newRules == null) {
			sBuilder.append("Removed reccurence rules: " + newRules);
			return sBuilder.toString();
		}

		if (!oldRules.frequency.equals(newRules.frequency)) {
			sBuilder.append("Changed event occurence frequency: '" + oldRules.frequency + "' -> '" + newRules.frequency
					+ "'" + CRLF);
		}

		if (oldRules.count != null && newRules.count != null && !oldRules.count.equals(newRules.count)) {
			sBuilder.append(
					"Changed event occurence count: '" + oldRules.count + "' -> '" + newRules.count + "'" + CRLF);
		}
		if (oldRules.until != newRules.until) {
			String oldValueUntil = (oldRules.until != null && oldRules.until.iso8601 != null) ? oldRules.until.iso8601
					: "null";
			String newValueUntil = (newRules.until != null && newRules.until.iso8601 != null) ? newRules.until.iso8601
					: "null";
			sBuilder.append(
					"Changed event occurence until date: '" + oldValueUntil + "' -> '" + newValueUntil + "'" + CRLF);
		}
		if (oldRules.interval != null && newRules.interval != null && !oldRules.interval.equals(newRules.interval)) {
			sBuilder.append("Changed event occurence interval: '" + oldRules.interval + "' -> '" + newRules.interval
					+ "'" + CRLF);
		}

		if (oldRules.byDay == null && newRules.byDay != null) {
			sBuilder.append("Added event occurence day: '" + newRules.byDay + "'" + CRLF);
		}
		if (oldRules.byDay != null && newRules.byDay == null) {
			sBuilder.append("Removed event occurence day: '" + newRules.byDay + "'" + CRLF);
		}

		if (oldRules.byDay != null && newRules.byDay != null) {
			List<WeekDay> differences = newRules.byDay.stream().filter(element -> !oldRules.byDay.contains(element))
					.collect(Collectors.toList());
			if (!differences.isEmpty()) {
				sBuilder.append(
						"Changed event occurence day: '" + oldRules.byDay + " -> " + newRules.byDay + "'" + CRLF);
			}
		}

		if (oldRules.byMinute != null && newRules.byMinute != null) {
			List<Integer> differences = newRules.byMinute.stream()
					.filter(element -> !oldRules.byMinute.contains(element)).collect(Collectors.toList());
			if (!differences.isEmpty()) {
				sBuilder.append("Changed event occurence minute: '" + oldRules.byMinute + " -> " + newRules.byMinute
						+ "'" + CRLF);
			}
		}

		if (oldRules.byHour != null && newRules.byHour != null) {
			List<Integer> differences = newRules.byHour.stream().filter(element -> !oldRules.byHour.contains(element))
					.collect(Collectors.toList());
			if (!differences.isEmpty()) {
				sBuilder.append(
						"Changed event occurence hours: '" + oldRules.byHour + " -> " + newRules.byHour + "'" + CRLF);
			}
		}

		if (oldRules.byMonthDay != null && newRules.byMonthDay != null) {
			List<Integer> differences = newRules.byMonthDay.stream()
					.filter(element -> !oldRules.byMonthDay.contains(element)).collect(Collectors.toList());
			if (!differences.isEmpty()) {
				sBuilder.append("Changed event occurence month days: '" + oldRules.byMonthDay + " -> "
						+ newRules.byMonthDay + "'" + CRLF);
			}
		}

		if (oldRules.byYearDay != null && newRules.byYearDay != null) {
			List<Integer> differences = newRules.byYearDay.stream()
					.filter(element -> !oldRules.byYearDay.contains(element)).collect(Collectors.toList());
			if (!differences.isEmpty()) {
				sBuilder.append("Changed event occurence year days: '" + oldRules.byYearDay + " -> "
						+ newRules.byYearDay + "'" + CRLF);
			}
		}

		if (oldRules.byWeekNo != null && newRules.byWeekNo != null) {
			List<Integer> differences = newRules.byWeekNo.stream()
					.filter(element -> !oldRules.byWeekNo.contains(element)).collect(Collectors.toList());
			if (!differences.isEmpty()) {
				sBuilder.append("Changed event occurence week numbers: '" + oldRules.byWeekNo + " -> "
						+ newRules.byWeekNo + "'" + CRLF);
			}
		}

		if (oldRules.byMonth != null && newRules.byMonth != null) {
			List<Integer> differences = newRules.byMonth.stream().filter(element -> !oldRules.byMonth.contains(element))
					.collect(Collectors.toList());
			if (!differences.isEmpty()) {
				sBuilder.append(
						"Changed event occurence month: '" + oldRules.byMonth + " -> " + newRules.byMonth + "'" + CRLF);
			}
		}
		return sBuilder.toString();

	}

}