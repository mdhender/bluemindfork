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
package net.bluemind.todolist.service.helper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.todolist.adapter.VTodoAdapter;
import net.bluemind.todolist.api.VTodo;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.PeriodList;
import net.fortuna.ical4j.model.component.VEvent;

public class OccurrenceHelper {

	/**
	 * Returns a list of occurrences of a recurring event.
	 * 
	 * @param vtodo
	 *            the recurring event
	 * @param dtstart
	 *            the start date of the period
	 * @param dtend
	 *            the end date of the period
	 * @return a list of {@link VEvent}
	 */
	public static List<VTodo> list(VTodo vtodo, BmDateTime dtstart, BmDateTime dtend) {
		net.fortuna.ical4j.model.component.VToDo ical4j = VTodoAdapter.adaptTodo(null, vtodo);

		DateTime start = dateToDateTime(VTodoAdapter.convertToIcsDate(dtstart));
		DateTime end = dateToDateTime(VTodoAdapter.convertToIcsDate(dtend));

		Period period = new Period(start, end);

		PeriodList consumedTime = ical4j.calculateRecurrenceSet(period);

		ArrayList<VTodo> ret = new ArrayList<VTodo>(consumedTime.size());
		for (@SuppressWarnings("unchecked")
		Iterator<Period> i = consumedTime.iterator(); i.hasNext();) {
			Period p = i.next();
			VTodo copy = (VTodo) vtodo.copy();
			copy.dtstart = BmDateTimeWrapper.fromTimestamp(p.getStart().getTime(), copy.dtstart.timezone,
					copy.dtstart.precision);

			copy.due = BmDateTimeWrapper.fromTimestamp(p.getEnd().getTime(), copy.due.timezone, copy.due.precision);

			ret.add(copy);
		}
		return ret;
	}

	private static DateTime dateToDateTime(Date icsDate) {
		DateTime start;
		if (icsDate instanceof Date) {
			start = new DateTime(icsDate.getTime());
		} else {
			start = (DateTime) icsDate;
		}
		return start;
	}

	/**
	 * Returns an occurrence of a recurring event that occurs a specific date.
	 * 
	 * @param vtodo
	 *            the vtodo
	 * @param occurrenceDtstart
	 *            the expected occurrence date
	 * @return an {@link VEvent} if the recurring event occurs at
	 *         {@link BmDateTime} occurrenceDtstart. Returns null otherwise.
	 */
	public static VTodo getOccurrence(VTodo vtodo, BmDateTime occurrenceDtstart) {

		net.fortuna.ical4j.model.component.VToDo ical4j = VTodoAdapter.adaptTodo(null, vtodo);

		DateTime to = new DateTime(new BmDateTimeWrapper(occurrenceDtstart).toUTCTimestamp() + 84600000);

		net.fortuna.ical4j.model.DateTime from;
		if (occurrenceDtstart.precision == Precision.DateTime) {
			from = (DateTime) VTodoAdapter.convertToIcsDate(occurrenceDtstart);
		} else {
			from = new DateTime(VTodoAdapter.convertToIcsDate(occurrenceDtstart));
		}

		Period period = new Period(from, to);
		PeriodList periods = ical4j.calculateRecurrenceSet(period);

		if (periods.size() > 0) {
			for (@SuppressWarnings("unchecked")
			Iterator<Period> i = periods.iterator(); i.hasNext();) {
				final Period p = (Period) i.next();
				if (p.getStart().equals(from)) {
					VTodo copy = (VTodo) vtodo.copy();
					copy.dtstart = BmDateTimeWrapper.fromTimestamp(p.getStart().getTime(), copy.dtstart.timezone,
							copy.dtstart.precision);
					copy.due = BmDateTimeWrapper.fromTimestamp(p.getEnd().getTime(), copy.due.timezone,
							copy.due.precision);
					return copy;
				}
			}
		}

		return null;

	}
}
