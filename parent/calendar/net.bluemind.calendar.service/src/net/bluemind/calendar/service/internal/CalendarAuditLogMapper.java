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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.auditlogs.AuditLogEntry;
import net.bluemind.core.auditlogs.ContentElement;
import net.bluemind.core.auditlogs.ContentElement.ContentElementBuilder;
import net.bluemind.core.auditlogs.ILogMapperProvider;
import net.bluemind.core.container.model.ChangeLogEntry.Type;
import net.bluemind.core.container.model.Item;
import net.bluemind.icalendar.api.ICalendarElement;

public class CalendarAuditLogMapper implements ILogMapperProvider<VEventSeries> {

	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static Logger logger = LoggerFactory.getLogger(CalendarAuditLogMapper.class);

	@Override
	public AuditLogEntry enhanceAuditLogEntry(Item item, VEventSeries oldValue, VEventSeries newValue, Type action,
			AuditLogEntry auditLogEntry) {
		ContentElement content = buildContent(newValue);
		if (content != null) {
			auditLogEntry.content = content;
			if (oldValue != null) {
				auditLogEntry.updatemessage = computeUpdateDifference(oldValue, newValue);
			}
			return auditLogEntry;
		}
		return null;
	}

	private String computeUpdateDifference(VEventSeries oldValue, VEventSeries newValue) {
		StringBuilder sBuilder = new StringBuilder();
		VEvent oldEvent = (oldValue.main != null) ? oldValue.main : oldValue.occurrences.get(0);
		VEvent newEvent = (newValue.main != null) ? newValue.main : newValue.occurrences.get(0);
		if (oldEvent.dtend != null && newEvent.dtend != null && !oldEvent.dtend.equals(newEvent.dtend)) {
			sBuilder.append("event end date changed: '" + oldEvent.dtend + "' -> '" + newEvent.dtend + "'");
			sBuilder.append(';');
		}

		if (oldEvent.dtstart != null && newEvent.dtstart != null && !oldEvent.dtstart.equals(newEvent.dtstart)) {
			sBuilder.append("event start date changed: '" + oldEvent.dtstart + "' -> '" + newEvent.dtstart + "'");
			sBuilder.append(';');
		}

		if (oldEvent.attendees != null && newEvent.attendees != null) {

			List<ICalendarElement.Attendee> removedAttendees = ICalendarElement.diff(oldEvent.attendees,
					newEvent.attendees);
			if (!removedAttendees.isEmpty()) {
				String removed = removedAttendees.stream().map(a -> a.mailto).collect(Collectors.joining(","));
				sBuilder.append("removed attendees: '" + removed + "'");
				sBuilder.append(';');

			}
			List<ICalendarElement.Attendee> addedAttendees = ICalendarElement.diff(newEvent.attendees,
					oldEvent.attendees);
			if (!addedAttendees.isEmpty()) {
				String added = addedAttendees.stream().map(a -> a.mailto).collect(Collectors.joining(","));
				sBuilder.append("added attendees: '" + added + "'");
				sBuilder.append(';');

			}

		}

		if (newEvent.location != null && oldEvent.location != null && !newEvent.location.equals(oldEvent.location)) {
			sBuilder.append("event location changed: '" + oldEvent.location + "' -> '" + newEvent.location + "'");
			sBuilder.append(';');
		}

		if (newEvent.description != null && oldEvent.description != null
				&& !newEvent.description.equals(oldEvent.description)) {
			sBuilder.append(
					"event description changed: '" + oldEvent.description + "' -> '" + newEvent.description + "'");
			sBuilder.append(';');
		}

		return sBuilder.toString();
	}

	private ContentElement buildContent(VEventSeries value) {
		ContentElementBuilder builder = new ContentElement.ContentElementBuilder();
		builder.key(value.icsUid);
		if (value.main == null && value.occurrences.isEmpty()) {
			return null;
		}
		try {
			String source = objectMapper.writeValueAsString(value);
			builder.newValue(source);
		} catch (JsonProcessingException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}

		VEvent event = (value.main != null) ? value.main : value.occurrences.get(0);

		builder.description(event.summary);
		List<String> attendees = new ArrayList<>();
		Map<String, String> has = new HashMap<>();
		if (event.attendees != null) {
			attendees.addAll(event.attendees.stream().map(a -> a.mailto.trim()).toList());
			attendees.addAll(event.attendees.stream().map(a -> a.commonName.trim()).toList());
			has.put("partStatus",
					event.attendees.stream().map(a -> a.mailto.trim() + ": " + a.partStatus).toList().toString());
			builder.with(attendees);
		}
		if (event.organizer != null) {
			List<String> organizers = Arrays.asList(event.organizer.mailto.trim(), event.organizer.commonName.trim());
			builder.author(organizers);
			attendees.addAll(organizers);
		}

		Map<String, String> is = new HashMap<>();
		if (event.dtstart != null) {
			is.put("dtstart", (event.dtstart.toString()));
		}
		if (event.dtend != null) {
			is.put("dtendt", (event.dtend.toString()));
		}
		if (!is.isEmpty()) {
			builder.is(is);
		}
		if (!has.isEmpty()) {
			builder.has(has);
		}
		return builder.build();
	}

}
