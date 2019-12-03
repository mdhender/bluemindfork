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
package net.bluemind.calendar.occurrence;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.helper.ical4j.VEventServiceHelper;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.container.model.ItemValue;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateRange;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.PeriodList;

public class OccurrenceHelper {

	/**
	 * Returns a list of occurrences of a recurring event.
	 * 
	 * @param event   the recurring event
	 * @param dtstart the start date of the period
	 * @param dtend   the end date of the period
	 * @return a list of {@link VEvent}
	 */
	public static List<VEvent> list(ItemValue<VEventSeries> event, BmDateTime dtstart, BmDateTime dtend) {

		Date start = VEventServiceHelper.convertToIcsDate(dtstart);
		Date end = VEventServiceHelper.convertToIcsDate(dtend);

		if (end.before(start)) {
			return Collections.emptyList();
		}

		Period period = new Period(new DateTime(start), new DateTime(end));
		List<VEvent> ret = new LinkedList<VEvent>();
		ret.addAll(getOccurrences(event, period, o -> period(o.dtstart, o.dtend)));
		ret.sort((e1, e2) -> {
			return new BmDateTimeWrapper(e1.dtstart).toDateTime()
					.compareTo(new BmDateTimeWrapper(e2.dtstart).toDateTime());
		});
		return ret;
	}

	/**
	 * Returns an occurrence of a recurring event that occurs a specific date.
	 * 
	 * @param event   the event
	 * @param dtstart the expected occurrence date
	 * @return an {@link VEvent} if the recurring event occurs at {@link BmDateTime}
	 *         occurrenceDtstart. Returns null otherwise.
	 */
	public static VEventOccurrence getOccurrence(ItemValue<VEventSeries> event, BmDateTime dtstart) {
		Period period = instant(dtstart);
		Optional<VEventOccurrence> occurrence = getOccurrences(event, period, o -> instant(o.dtstart)).stream()
				.filter(o -> dtstart.equals(o.dtstart)).findFirst();
		return occurrence.isPresent() ? occurrence.get() : null;
	}

	/**
	 * Returns an occurrence of a recurring event with a specific recurid.
	 * 
	 * @param event   the event
	 * @param recurid the expected occurrence date
	 * @return an {@link VEvent} if the recurring event occurs at {@link BmDateTime}
	 *         occurrenceDtstart. Returns null otherwise.
	 */
	public static Optional<VEventOccurrence> getOccurrenceByRecurId(ItemValue<VEventSeries> event, BmDateTime recurid) {
		Period period = day(recurid);
		Optional<VEventOccurrence> occurrence = getOccurrences(event, period, o -> instant(o.recurid)).stream()
				.findFirst();
		return occurrence;
	}

	private static Period period(BmDateTime dtstart, BmDateTime dtend) {
		DateTime from = new DateTime(VEventServiceHelper.convertToIcsDate(dtstart));
		DateTime to = new DateTime(VEventServiceHelper.convertToIcsDate(dtend));
		return new Period(from, to);
	}

	private static Period instant(BmDateTime date) {
		DateTime to = new DateTime(new BmDateTimeWrapper(date).toUTCTimestamp() + 1000);
		DateTime from = new DateTime(VEventServiceHelper.convertToIcsDate(date));
		return new Period(from, to);
	}

	private static Period day(BmDateTime date) {
		ZonedDateTime zonedDate = new BmDateTimeWrapper(date).toDateTime().truncatedTo(ChronoUnit.DAYS);
		DateTime from = new DateTime(Date.from(zonedDate.toInstant()));
		DateTime to = new DateTime(DateTime.from(zonedDate.plusDays(1).toInstant()));
		return new Period(from, to);
	}

	private static List<VEventOccurrence> getOccurrences(ItemValue<VEventSeries> event, Period period,
			Function<VEventOccurrence, Period> criterion) {

		List<VEventOccurrence> occurrences = getExplicitOccurrences(event, period, criterion);
		occurrences.addAll(getCalculatedOccurrences(event, period, criterion));
		return occurrences;

	}

	private static List<VEventOccurrence> getExplicitOccurrences(ItemValue<VEventSeries> event, Period period,
			Function<VEventOccurrence, Period> criterion) {
		return event.value.occurrences.stream().filter(o -> period.intersects(criterion.apply(o)))
				.collect(Collectors.toList());

	}

	@SuppressWarnings("unchecked")
	private static List<VEventOccurrence> getCalculatedOccurrences(ItemValue<VEventSeries> event, Period period,
			Function<VEventOccurrence, Period> criterion) {
		if (event.value.main == null) {
			return ImmutableList.of();
		}
		if (event.value.main.rrule == null) {
			VEventOccurrence mainOccurrence = VEventOccurrence.fromEvent(event.value.main, event.value.main.dtstart);
			if (period.intersects(criterion.apply(mainOccurrence))) {
				return ImmutableList.of(mainOccurrence);
			}
			return ImmutableList.of();
		}

		PeriodList periods = calculatedSeriesOccurrences(event, period);
		return (List<VEventOccurrence>) periods.stream().filter(o -> period.intersects((Period) o)).map(o -> {
			Period p = (Period) o;
			BmDateTime recurid = BmDateTimeWrapper.fromTimestamp(p.getStart().getTime(),
					event.value.main.dtstart.timezone, event.value.main.dtstart.precision);
			VEventOccurrence copy = VEventOccurrence.fromEvent(event.value.main, recurid);
			copy.dtstart = BmDateTimeWrapper.fromTimestamp(p.getStart().getTime(), copy.dtstart.timezone,
					copy.dtstart.precision);
			copy.dtend = BmDateTimeWrapper.fromTimestamp(p.getEnd().getTime(), copy.dtend.timezone,
					copy.dtend.precision);
			return copy;

		}).collect(Collectors.toList());
	}

	private static PeriodList calculatedSeriesOccurrences(ItemValue<VEventSeries> event, Period period) {
		net.fortuna.ical4j.model.component.VEvent ical4jVEvent = null;
		if (!event.value.occurrences.isEmpty()) {
			VEventSeries copy = event.value.copy();
			copy.main.exdate = copy.occurrences.stream().map(o -> o.recurid).collect(Collectors.toSet());
			if (event.value.main.exdate != null) {
				copy.main.exdate.addAll(event.value.main.exdate);
			}
			copy.occurrences = Collections.emptyList();
			ical4jVEvent = VEventServiceHelper.convertToIcal4jVEvent(ItemValue.create(event, copy)).get(0);
		} else {
			ical4jVEvent = VEventServiceHelper.convertToIcal4jVEvent(event).get(0);
		}
		PeriodList periods = ical4jVEvent.calculateRecurrenceSet(period);
		return periods;
	}

	/**
	 * Returns the first occurrence of {event} occurring after {start} or
	 * {Optional.Empty} if there is none.
	 * 
	 * @param start
	 * @param event
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Optional<VEventOccurrence> getNextOccurrence(BmDateTime start, VEvent event) {
		Date from = VEventServiceHelper.convertToIcsDate(start);
		Date to = null;
		if (event.rrule.until != null) {
			to = VEventServiceHelper.convertToIcsDate(event.rrule.until);
		} else {
			LocalDateTime now = LocalDateTime.now();
			LocalDateTime twoYears = now.plusYears(2);
			to = new net.fortuna.ical4j.model.DateTime(
					twoYears.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
		}
		Period period = new Period(new DateTime(from), new DateTime(to));
		net.fortuna.ical4j.model.component.VEvent ical4jVEvent = VEventServiceHelper.parse(null, event);
		PeriodList periods = ical4jVEvent.calculateRecurrenceSet(period);

		return periods.stream().filter(o -> period.includes(((Period) o).getStart(), DateRange.INCLUSIVE_END))
				.findFirst().map(op -> {
					Period p = (Period) op;
					BmDateTime recurid = BmDateTimeWrapper.fromTimestamp(p.getStart().getTime(), event.dtstart.timezone,
							event.dtstart.precision);
					VEventOccurrence copy = VEventOccurrence.fromEvent(event, recurid);
					copy.dtstart = BmDateTimeWrapper.fromTimestamp(p.getStart().getTime(), copy.dtstart.timezone,
							copy.dtstart.precision);
					copy.dtend = BmDateTimeWrapper.fromTimestamp(p.getEnd().getTime(), copy.dtend.timezone,
							copy.dtend.precision);
					return copy;
				});
	}

}
