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
package net.bluemind.calendar.service.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTimeValidator;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.validator.IValidator;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.RRule.Frequency;
import net.bluemind.icalendar.api.ICalendarElement.RRule.WeekDay;

public class VEventValidator implements IValidator<VEventSeries> {

	private BmDateTimeValidator bmDateTimeValidator;

	public VEventValidator() {
		bmDateTimeValidator = new BmDateTimeValidator();
	}

	/**
	 * @param vevent
	 * @throws ServerFault
	 */
	public void validate(VEventSeries vevent) throws ServerFault {
		if (vevent == null || (vevent.main == null && (vevent.occurrences == null || vevent.occurrences.isEmpty()))) {
			throw new ServerFault("VEvent is null", ErrorCode.EVENT_ERROR);
		}

		if (null != vevent.main) {
			validate(vevent.main);
		}
		vevent.occurrences.forEach(this::validate);
	}

	/**
	 * @param vevent
	 * @throws ServerFault
	 */
	public void validate(VEvent vevent) throws ServerFault {
		if (vevent == null) {
			throw new ServerFault("VEvent is null", ErrorCode.EVENT_ERROR);
		}

		// DTStart
		if (vevent.dtstart == null) {
			throw new ServerFault("VEvent.dtstart is mandatory", ErrorCode.NO_EVENT_DATE);
		}

		bmDateTimeValidator.validate(vevent.dtstart);
		bmDateTimeValidator.validate(vevent.dtend);
		if (vevent.exdate != null) {
			for (BmDateTime exdate : vevent.exdate) {
				bmDateTimeValidator.validate(exdate);
			}
		}

		// DTEnd must be > DTStart
		if (new BmDateTimeWrapper(vevent.dtend).isBefore(vevent.dtstart)) {
			throw new ServerFault("VEvent.dtend must be greater than VEvent.dtstart ", ErrorCode.EVENT_ERROR);
		}

		// RRule
		if (vevent.rrule != null) {
			VEvent.RRule rrule = vevent.rrule;
			if (rrule.frequency == null) {
				throw new ServerFault("VEvent.RRule.frequency is null", ErrorCode.EVENT_ERROR);
			}

			bmDateTimeValidator.validate(rrule.until);

			if (rrule.until != null && new BmDateTimeWrapper(rrule.until).isBefore(vevent.dtstart)) {
				throw new ServerFault("RRule.until is prior to event date",
						ErrorCode.EVENT_ENDREPEAT_PRIOR_TO_EVENT_DATE);
			}
			// BM-8206
			if (rrule.frequency == Frequency.WEEKLY && (rrule.byDay == null || rrule.byDay.isEmpty())) {
				rrule.byDay = new ArrayList<ICalendarElement.RRule.WeekDay>();
				BmDateTimeWrapper tw = new BmDateTimeWrapper(vevent.dtstart);
				switch (tw.format("E")) {
				case "Sun":
					rrule.byDay.add(WeekDay.SU);
					break;
				case "Mon":
					rrule.byDay.add(WeekDay.MO);
					break;
				case "Tue":
					rrule.byDay.add(WeekDay.TU);
					break;
				case "Wed":
					rrule.byDay.add(WeekDay.WE);
					break;
				case "Thu":
					rrule.byDay.add(WeekDay.TH);
					break;
				case "Fri":
					rrule.byDay.add(WeekDay.FR);
					break;
				case "Sat":
					rrule.byDay.add(WeekDay.SA);
					break;
				}
			}

			checkIntegerList(rrule.bySecond, 0, 59);
			checkIntegerList(rrule.byMinute, 0, 59);
			checkIntegerList(rrule.byHour, 0, 23);
			checkIntegerList(rrule.byMonthDay, 1, 31);
			checkIntegerList(rrule.byYearDay, 1, 366);
			checkIntegerList(rrule.byWeekNo, 1, 53);
			checkIntegerList(rrule.byMonth, 1, 12);
		}

		// FIXME allow empty title?
		if (StringUtils.isEmpty(vevent.summary)) {
			throw new ServerFault("Event title is empty", ErrorCode.EMPTY_EVENT_TITLE);
		}

	}

	private void checkIntegerList(List<Integer> intList, int min, int max) throws ServerFault {
		if (intList == null) {
			return;

		}

		List<Integer> inError = intList.stream().filter(i -> {
			return i == null || i < min || i > max;
		}).collect(Collectors.toList());

		if (inError.size() > 0) {
			throw new ServerFault("value should be between " + min + " and " + max + " : " + inError,
					ErrorCode.EVENT_ERROR);
		}

	}

	@Override
	public void create(VEventSeries obj) throws ServerFault {
		validate(obj);
	}

	@Override
	public void update(VEventSeries oldValue, VEventSeries newValue) throws ServerFault {
		validate(newValue);
	}
}
