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
package net.bluemind.calendar;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.helper.mail.CalendarMailHelper;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.RRule;

public class VEventUtil {
	private static Logger logger = LoggerFactory.getLogger(VEventUtil.class);

	public static <T extends VEvent> boolean eventChanged(T oldEvent, T newEvent) {
		return eventChanges(oldEvent, newEvent).hasChanged();
	}

	public static <T extends VEvent> EventChanges eventChanges(T oldEvent, T newEvent) {
		EnumSet<EventChanges.Type> changes = EnumSet.noneOf(EventChanges.Type.class);
		if (oldEvent == null && newEvent != null || oldEvent != null && newEvent == null) {
			changes.add(EventChanges.Type.EVENT);
			return new EventChanges(changes);
		}
		if (oldEvent == null && newEvent == null) {
			changes.add(EventChanges.Type.EVENT);
			return new EventChanges(changes);
		}

		if (changed(oldEvent.sequence, newEvent.sequence)) {
			changes.add(EventChanges.Type.EVENT);
		}

		Set<ICalendarElement.Attendee> attendeesDiff = new HashSet<>(
				ICalendarElement.diff(oldEvent.attendees, newEvent.attendees));
		if (!attendeesDiff.isEmpty()) {
			changes.add(EventChanges.Type.ATTENDEES);
		}

		attendeesDiff = new HashSet<>(ICalendarElement.diff(newEvent.attendees, oldEvent.attendees));
		if (!attendeesDiff.isEmpty()) {
			changes.add(EventChanges.Type.ATTENDEES);
		}

		if (changed(oldEvent.url, newEvent.url)) {
			changes.add(EventChanges.Type.URL);
		}

		if (changed(oldEvent.conference, newEvent.conference)) {
			changes.add(EventChanges.Type.CONFERENCE);
		}

		if (changed(oldEvent.summary, newEvent.summary)) {
			changes.add(EventChanges.Type.SUMMARY);
		}

		if (rRuleChanged(oldEvent.rrule, newEvent.rrule)) {
			changes.add(EventChanges.Type.RRULE);
		}

		if (changed(oldEvent.priority, newEvent.priority)) {
			changes.add(EventChanges.Type.PRIORITY);
		}

		if (changed(oldEvent.location, newEvent.location)) {
			changes.add(EventChanges.Type.LOCATION);
		}

		if (changed(oldEvent.description, newEvent.description)) {
			changes.add(EventChanges.Type.DESCRIPTION);
		}

		if (changed(oldEvent.dtstart, newEvent.dtstart)) {
			changes.add(EventChanges.Type.DTSTART);
		}

		if (changed(oldEvent.dtend, newEvent.dtend)) {
			changes.add(EventChanges.Type.DTEND);
		}

		if (changed(oldEvent.transparency, newEvent.transparency)) {
			changes.add(EventChanges.Type.TRANSPARENCY);
		}

		if (changed(oldEvent.classification, newEvent.classification)) {
			changes.add(EventChanges.Type.CLASSIFICATION);
		}

		if (listChanged(oldEvent.attachments, newEvent.attachments)) {
			changes.add(EventChanges.Type.ATTACHMENTS);
		}

		return new EventChanges(changes);
	}

	private static boolean rRuleChanged(RRule rule1, RRule rule2) {
		if (rule1 == null && rule2 == null) {
			return false;
		} else {
			if (rule1 == null || rule2 == null) {
				return true;
			} else {
				return rulechanged(rule1, rule2);
			}
		}
	}

	private static boolean rulechanged(RRule rule1, RRule rule2) {
		if (changed(rule1.frequency, rule2.frequency)) {
			return true;
		}

		if (changed(rule1.count, rule2.count)) {
			return true;
		}

		if (changed(rule1.until, rule2.until)) {
			return true;
		}

		if (changed(rule1.interval, rule2.interval)) {
			return true;
		}

		if (changed(rule1.interval, rule2.interval)) {
			return true;
		}

		if (listChanged(rule1.bySecond, rule2.bySecond)) {
			return true;
		}

		if (listChanged(rule1.byMinute, rule2.byMinute)) {
			return true;
		}

		if (listChanged(rule1.byHour, rule2.byHour)) {
			return true;
		}

		if (listChanged(rule1.byDay, rule2.byDay)) {
			return true;
		}

		if (listChanged(rule1.byMonthDay, rule2.byMonthDay)) {
			return true;
		}

		if (listChanged(rule1.byYearDay, rule2.byYearDay)) {
			return true;
		}

		if (listChanged(rule1.byWeekNo, rule2.byWeekNo)) {
			return true;
		}

		if (listChanged(rule1.byMonth, rule2.byMonth)) {
			return true;
		}

		return false;
	}

	private static boolean changed(Object elem1, Object elem2) {
		if (elem1 instanceof String asString && asString.isEmpty()) {
			elem1 = null;
		}
		if (elem2 instanceof String asString && asString.isEmpty()) {
			elem2 = null;
		}
		if (elem1 == null && elem2 == null) {
			return false;
		} else {
			if (elem1 == null || elem2 == null) {
				return true;
			} else {
				return !elem1.equals(elem2);
			}
		}
	}

	private static <T> boolean listChanged(List<T> list1, List<T> list2) {
		if (list1 == null && list2 == null) {
			return false;
		} else {
			if (list1 == null) {
				list1 = Collections.emptyList();
			}

			if (list2 == null) {
				list2 = Collections.emptyList();
			}
			return !list1.equals(list2);
		}
	}

	public static void addPreviousEventInfos(VEventSeries oldSeries, VEvent event, Map<String, Object> data) {
		if (oldSeries == null) {
			data.put("deleted_attendees", Collections.emptySet());
			data.put("added_attendees", Collections.emptySet());
			return;
		}

		VEvent findCorrespondingEvent = findOrCalculateCorrespondingEvent(oldSeries, event);
		Map<String, Object> old = null != findCorrespondingEvent
				? new CalendarMailHelper().extractVEventData(findCorrespondingEvent)
				: new HashMap<>();
		for (Entry<String, Object> e : old.entrySet()) {
			data.put("old_" + e.getKey(), e.getValue());
		}

		if (findCorrespondingEvent != null) {
			List<String> deletedList = new HashSet<>(
					ICalendarElement.diff(findCorrespondingEvent.attendees, event.attendees)).stream()
					.map(CalendarMailHelper::attendeeDisplayName).toList();
			data.put("deleted_attendees", deletedList);

			List<String> addedAttendees = ICalendarElement.diff(event.attendees, findCorrespondingEvent.attendees)
					.stream().map(CalendarMailHelper::attendeeDisplayName).toList();
			data.put("added_attendees", addedAttendees);

			if (data.containsKey("attendees")) {
				List<String> list = new ArrayList<>((List<String>) data.get("attendees"));
				list.removeAll(addedAttendees);
				data.put("attendees", list);
			}
		} else {
			data.put("deleted_attendees", Collections.emptySet());
			data.put("added_attendees", Collections.emptySet());
		}

		// Fix highlight new location
		if (!data.containsKey("old_location")) {
			data.put("old_location", "");
		}

		// Fix highlight new description
		if (!data.containsKey("old_description")) {
			data.put("old_description", "");
		}

		// Fix highlight new url
		if (!data.containsKey("old_url")) {
			data.put("old_url", "");
		}

		// Fix highlight new conference url
		if (!data.containsKey("old_conference")) {
			data.put("old_conference", "");
		}
	}

	public static VEvent findCorrespondingEvent(VEventSeries otherSeries, VEvent evt) {
		VEvent match = null;
		if (evt.exception()) {
			match = otherSeries.occurrence(((VEventOccurrence) evt).recurid);
		} else {
			match = otherSeries.main;
		}
		if (match != null && match.draft) {
			return null;
		}
		return match;
	}

	public static VEvent findOrCalculateCorrespondingEvent(VEventSeries otherSeries, VEvent evt) {
		VEvent match = findCorrespondingEvent(otherSeries, evt);
		if (match == null && evt.exception()) {
			match = calculateOldEventOfException(otherSeries, evt);
		}
		return match;

	}

	private static VEvent calculateOldEventOfException(VEventSeries oldSeries, VEvent evt) {
		if (oldSeries.main == null) {
			return new VEvent();
		}
		VEvent oldEvent = oldSeries.main.copy();
		oldEvent.rrule = null;
		oldEvent.dtstart = ((VEventOccurrence) evt).recurid;
		ZonedDateTime start = new BmDateTimeWrapper(oldEvent.dtstart).toDateTime();
		Long duration = oldSeries.main.dtend != null
				? (new BmDateTimeWrapper(oldSeries.main.dtend).toUTCTimestamp()
						- new BmDateTimeWrapper(oldSeries.main.dtstart).toUTCTimestamp())
				: 30000;
		oldEvent.dtend = BmDateTimeWrapper
				.fromTimestamp(start.plus(duration, ChronoUnit.MILLIS).toInstant().toEpochMilli());
		return oldEvent;
	}

}
