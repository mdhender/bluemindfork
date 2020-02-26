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
import java.util.HashSet;
import java.util.List;

import com.google.common.collect.Sets;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.icalendar.api.ICalendarElement.RRule;

public class VEventUtil {

	public static <T extends VEvent> boolean eventChanged(T oldEvent, T newEvent) {
		
		if (changed(oldEvent.sequence, newEvent.sequence)) {
			return true;
		}
		
		if (changed(oldEvent.url, newEvent.url)) {
			return true;
		}

		if (changed(oldEvent.summary, newEvent.summary)) {
			return true;
		}

		if (rRuleChanged(oldEvent.rrule, newEvent.rrule)) {
			return true;
		}

		if (changed(oldEvent.priority, newEvent.priority)) {
			return true;
		}

		if (changed(oldEvent.location, newEvent.location)) {
			return true;
		}

		if (changed(oldEvent.description, newEvent.description)) {
			return true;
		}

		if (changed(oldEvent.dtstart, newEvent.dtstart)) {
			return true;
		}

		if (changed(oldEvent.dtend, newEvent.dtend)) {
			return true;
		}

		if (changed(oldEvent.transparency, newEvent.transparency)) {
			return true;
		}

		if (changed(oldEvent.classification, newEvent.classification)) {
			return true;
		}

		if (listChanged(oldEvent.attachments, newEvent.attachments)) {
			return true;
		}

		return false;
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
			return !Sets.difference(new HashSet<T>(list1), new HashSet<T>(list2)).isEmpty();
		}
	}

}
