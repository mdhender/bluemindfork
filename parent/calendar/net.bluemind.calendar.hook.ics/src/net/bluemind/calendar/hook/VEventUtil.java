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
package net.bluemind.calendar.hook;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.RRule;

public class VEventUtil {

	public static enum EventChanges {
		EVENT, URL, CONFERENCE, SUMMARY, RRULE, PRIORITY, LOCATION, DESCRIPTION, DTSTART, DTEND, TRANSPARENCY,
		CLASSIFICATION, ATTACHMENTS, ATTENDEES
	}

	public static <T extends VEvent> boolean eventChanged(T oldEvent, T newEvent) {
		return !eventChanges(oldEvent, newEvent).isEmpty();
	}

	public static <T extends VEvent> EnumSet<EventChanges> eventChanges(T oldEvent, T newEvent) {

		EnumSet<EventChanges> changes = EnumSet.noneOf(EventChanges.class);

		if (oldEvent == null && newEvent != null || oldEvent != null && newEvent == null) {
			LoggerFactory.getLogger(VEventUtil.class).info("CH 1");
			changes.add(EventChanges.EVENT);
			return changes;
		}

		if (oldEvent == null && newEvent == null) {
			changes.add(EventChanges.EVENT);
			return changes;
		}

		if (changed(oldEvent.sequence, newEvent.sequence)) {
			changes.add(EventChanges.EVENT);
		}

		Set<ICalendarElement.Attendee> attendeesDiff = new HashSet<>(
				ICalendarElement.diff(oldEvent.attendees, newEvent.attendees));
		if (!attendeesDiff.isEmpty()) {
			changes.add(EventChanges.ATTENDEES);
		}

		attendeesDiff = new HashSet<>(ICalendarElement.diff(newEvent.attendees, oldEvent.attendees));
		if (!attendeesDiff.isEmpty()) {
			changes.add(EventChanges.ATTENDEES);
		}

		if (changed(oldEvent.url, newEvent.url)) {
			changes.add(EventChanges.URL);
		}

		if (changed(oldEvent.conference, newEvent.conference)) {
			changes.add(EventChanges.CONFERENCE);
		}

		if (changed(oldEvent.summary, newEvent.summary)) {
			changes.add(EventChanges.SUMMARY);
		}

		if (rRuleChanged(oldEvent.rrule, newEvent.rrule)) {
			changes.add(EventChanges.RRULE);
		}

		if (changed(oldEvent.priority, newEvent.priority)) {
			changes.add(EventChanges.PRIORITY);
		}

		if (changed(oldEvent.location, newEvent.location)) {
			changes.add(EventChanges.LOCATION);
		}

		if (changed(oldEvent.description, newEvent.description)) {
			changes.add(EventChanges.DESCRIPTION);
		}

		if (changed(oldEvent.dtstart, newEvent.dtstart)) {
			changes.add(EventChanges.DTSTART);
		}

		if (changed(oldEvent.dtend, newEvent.dtend)) {
			changes.add(EventChanges.DTEND);
		}

		if (changed(oldEvent.transparency, newEvent.transparency)) {
			changes.add(EventChanges.TRANSPARENCY);
		}

		if (changed(oldEvent.classification, newEvent.classification)) {
			changes.add(EventChanges.CLASSIFICATION);
		}

		if (listChanged(oldEvent.attachments, newEvent.attachments)) {
			changes.add(EventChanges.ATTACHMENTS);
		}

		return changes;
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

}
