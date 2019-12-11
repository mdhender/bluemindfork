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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.RRule.Frequency;
import net.bluemind.tests.defaultdata.BmDateTimeHelper;

public class OccurrenceHelperTest {
	private final ZoneId tz = ZoneId.of("Asia/Ho_Chi_Minh");
	ZonedDateTime refBeginDate = ZonedDateTime.of(2022, 2, 13, 1, 0, 0, 0, tz);
	ZonedDateTime refEndDate = ZonedDateTime.of(2022, 2, 13, 2, 0, 0, 0, tz);

	ZonedDateTime date1 = ZonedDateTime.of(2022, 2, 18, 0, 0, 0, 0, tz);
	ZonedDateTime date2 = ZonedDateTime.of(2022, 2, 13, 0, 0, 0, 0, tz);
	ZonedDateTime date3 = ZonedDateTime.of(2022, 2, 14, 0, 0, 0, 0, tz);

	@Test
	public void testListNoRecurring() {
		ItemValue<VEventSeries> serie = defaultVEvent();

		List<VEvent> res = OccurrenceHelper.list(serie,
				BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 13, 0, 0, 0, 0, tz)), BmDateTimeHelper.time(date1));
		assertEquals(1, res.size());

		res = OccurrenceHelper.list(serie, BmDateTimeHelper.time(date3), BmDateTimeHelper.time(date1));
		assertEquals(0, res.size());
	}

	@Test
	public void testRecurringNoException() {
		ItemValue<VEventSeries> serie = defaultVEvent();

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = Frequency.DAILY;
		serie.value.main.rrule = rrule;

		List<VEvent> res = OccurrenceHelper.list(serie, BmDateTimeHelper.time(date2), BmDateTimeHelper.time(date1));
		assertEquals(5, res.size());

		res = OccurrenceHelper.list(serie, BmDateTimeHelper.time(date3), BmDateTimeHelper.time(date1));
		assertEquals(4, res.size());
	}

	@Test
	public void testRecurringByMonthNoException() {
		ItemValue<VEventSeries> serie = defaultVEvent();
		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = Frequency.MONTHLY;
		rrule.interval = 1;
		serie.value.main.rrule = rrule;

		List<VEvent> res = OccurrenceHelper.list(serie, BmDateTimeHelper.time(date2),
				BmDateTimeHelper.time(ZonedDateTime.of(2023, 2, 13, 0, 0, 0, 0, tz)));
		assertEquals(12, res.size());
	}

	@Test
	public void testRecurringNoExceptionWithExDate() {
		ItemValue<VEventSeries> serie = defaultVEvent();

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = Frequency.DAILY;

		serie.value.main.rrule = rrule;
		serie.value.main.exdate = ImmutableSet.of(BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 1, 0, 0, 0, tz)));

		List<VEvent> res = OccurrenceHelper.list(serie, BmDateTimeHelper.time(date2), BmDateTimeHelper.time(date1));
		assertEquals(4, res.size());
	}

	@Test
	public void testRecurringWithException() {
		ItemValue<VEventSeries> serie = defaultVEvent();

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = Frequency.MONTHLY;
		rrule.interval = 1;
		serie.value.main.rrule = rrule;
		VEventOccurrence occurence = VEventOccurrence.fromEvent(serie.value.main,
				BmDateTimeHelper.time(refBeginDate.plusMonths(1)));
		occurence.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2022, 3, 13, 12, 0, 0, 0, tz));
		occurence.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2022, 3, 13, 13, 0, 0, 0, tz));
		serie.value.occurrences = Arrays.asList(occurence);
		List<VEvent> res = OccurrenceHelper.list(serie, BmDateTimeHelper.time(date2),
				BmDateTimeHelper.time(ZonedDateTime.of(2023, 2, 13, 0, 0, 0, 0, tz)));
		assertEquals(12, res.size());

		VEvent occ = res.get(0);
		assertEquals(occ.dtstart, serie.value.main.dtstart);
		assertEquals(occ.dtend, serie.value.main.dtend);

		occ = res.get(1);
		assertEquals(occurence.dtstart, occ.dtstart);
		assertEquals(occurence.dtend, occ.dtend);

		occ = res.get(2);
		assertEquals(BmDateTimeHelper.time(refBeginDate.plusMonths(2)), occ.dtstart);
		assertEquals(BmDateTimeHelper.time(refEndDate.plusMonths(2)), occ.dtend);
	}

	@Test
	public void testRecurringDailyWithException() {
		ItemValue<VEventSeries> serie = defaultVEvent();

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = Frequency.DAILY;
		serie.value.main.rrule = rrule;
		VEventOccurrence occurence = VEventOccurrence.fromEvent(serie.value.main,
				BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 1, 0, 0, 0, tz)));
		occurence.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 12, 0, 0, 0, tz));
		occurence.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 13, 0, 0, 0, tz));
		serie.value.occurrences = Arrays.asList(occurence);
		List<VEvent> res = OccurrenceHelper.list(serie, BmDateTimeHelper.time(date2), BmDateTimeHelper.time(date1));
		assertEquals(5, res.size());

		VEvent occ = res.get(0);
		assertEquals(occ.dtstart, serie.value.main.dtstart);
		assertEquals(occ.dtend, serie.value.main.dtend);

		occ = res.get(1);
		assertEquals(BmDateTimeHelper.time(refBeginDate.plusDays(1)), occ.dtstart);
		assertEquals(BmDateTimeHelper.time(refEndDate.plusDays(1)), occ.dtend);

		occ = res.get(2);
		assertEquals(occurence.dtstart, occ.dtstart);
		assertEquals(occurence.dtend, occ.dtend);

		occ = res.get(3);
		assertEquals(BmDateTimeHelper.time(refBeginDate.plusDays(3)), occ.dtstart);
		assertEquals(BmDateTimeHelper.time(refEndDate.plusDays(3)), occ.dtend);

		occ = res.get(4);
		assertEquals(BmDateTimeHelper.time(refBeginDate.plusDays(4)), occ.dtstart);
		assertEquals(BmDateTimeHelper.time(refEndDate.plusDays(4)), occ.dtend);

	}

	@Test
	public void testRecurringWeeklyWithException() {
		ItemValue<VEventSeries> serie = defaultVEvent();

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = Frequency.WEEKLY;
		rrule.interval = 1;
		rrule.byDay = Arrays.asList(ICalendarElement.RRule.WeekDay.SU);

		serie.value.main.rrule = rrule;
		VEventOccurrence occurence = VEventOccurrence.fromEvent(serie.value.main,
				BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 20, 1, 0, 0, 0, tz)));
		occurence.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 22, 12, 0, 0, 0, tz));
		occurence.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 22, 13, 0, 0, 0, tz));
		serie.value.occurrences = Arrays.asList(occurence);
		List<VEvent> res = OccurrenceHelper.list(serie,
				BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 19, 0, 0, 0, 0, tz)),
				BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 25, 0, 0, 0, 0, tz)));
		assertEquals(1, res.size());

		VEvent occ = res.get(0);
		assertEquals(occ.dtstart, occurence.dtstart);

		res = OccurrenceHelper.list(serie, BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 22, 12, 30, 0, 0, tz)),
				BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 22, 12, 45, 0, 0, tz)));
		assertEquals(1, res.size());
	}

	@Test
	public void testRecurringWithoutMaster() {
		ItemValue<VEventSeries> serie = defaultVEvent();

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = Frequency.DAILY;
		serie.value.main.rrule = rrule;

		VEventOccurrence occurence = VEventOccurrence.fromEvent(serie.value.main,
				BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 1, 0, 0, 0, tz)));

		occurence.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 12, 0, 0, 0, tz));
		occurence.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 13, 0, 0, 0, tz));
		serie.value.occurrences = Arrays.asList(occurence);
		serie.value.main = null;
		List<VEvent> res = OccurrenceHelper.list(serie, BmDateTimeHelper.time(date2), BmDateTimeHelper.time(date1));
		assertEquals(1, res.size());

		VEvent occ = res.get(0);
		assertEquals(occurence.dtstart, occ.dtstart);
		assertEquals(occurence.dtend, occ.dtend);
	}

	@Test
	public void testGetStandardOccurrenceByDateTimeStart() {
		ItemValue<VEventSeries> series = defaultVEvent();
		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = Frequency.DAILY;
		rrule.interval = 1;
		series.value.main.rrule = rrule;

		BmDateTime dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 1, 0, 0, 0, tz));
		VEventOccurrence occ = OccurrenceHelper.getOccurrence(series, dtstart);
		assertNotNull(occ);
		assertEquals(dtstart, occ.dtstart);

		occ = OccurrenceHelper.getOccurrence(series,
				BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 0, 0, 0, 0, tz)));
		assertNull(occ);

		occ = OccurrenceHelper.getOccurrence(series,
				BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 2, 0, 0, 0, tz)));
		assertNull(occ);

		BmDateTime exdate = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 17, 1, 0, 0, 0, tz));
		occ = OccurrenceHelper.getOccurrence(series, exdate);
		assertNotNull(occ);
		series.value.main.exdate = ImmutableSet.of(exdate);
		occ = OccurrenceHelper.getOccurrence(series, exdate);
		assertNull(occ);
	}

	@Test
	public void testGetStandardOccurrenceByDateStart() {
		ItemValue<VEventSeries> series = defaultVEvent();
		series.value.main.dtstart = BmDateTimeHelper
				.time(ZonedDateTime.of(2022, 2, 13, 0, 0, 0, 0, ZoneId.systemDefault()));
		series.value.main.dtend = BmDateTimeHelper
				.time(ZonedDateTime.of(2022, 2, 14, 0, 0, 0, 0, ZoneId.systemDefault()));
		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = Frequency.DAILY;
		rrule.interval = 2;
		series.value.main.rrule = rrule;
		BmDateTime dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 0, 0, 0, 0, ZoneId.systemDefault()));
		VEventOccurrence occ = OccurrenceHelper.getOccurrence(series, dtstart);
		assertNotNull(occ);
		assertEquals(dtstart, occ.dtstart);

		occ = OccurrenceHelper.getOccurrence(series,
				BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 16, 0, 0, 0, 0, ZoneId.systemDefault())));
		assertNull(occ);

		occ = OccurrenceHelper.getOccurrence(series,
				BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 12, 0, 0, 0, 0, ZoneId.systemDefault())));
		assertNull(occ);

		BmDateTime exdate = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 17, 0, 0, 0, 0, ZoneId.systemDefault()));
		occ = OccurrenceHelper.getOccurrence(series, exdate);
		assertNotNull(occ);
		series.value.main.exdate = ImmutableSet.of(exdate);
		occ = OccurrenceHelper.getOccurrence(series, exdate);
		assertNull(occ);

	}

	@Test
	public void testGetExceptionByDateTimeStart() {
		ItemValue<VEventSeries> series = defaultVEvent();
		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = Frequency.DAILY;
		rrule.interval = 2;
		series.value.main.rrule = rrule;

		VEventOccurrence occurence = VEventOccurrence.fromEvent(series.value.main,
				BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 19, 1, 0, 0, 0, tz)));
		occurence.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 20, 12, 0, 0, 0, tz));
		occurence.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 20, 13, 0, 0, 0, tz));
		series.value.occurrences = new ArrayList<VEventOccurrence>();
		series.value.occurrences.add(occurence);

		VEventOccurrence occ = OccurrenceHelper.getOccurrence(series, occurence.dtstart);
		assertNotNull(occ);
		assertEquals(occurence.dtstart, occ.dtstart);
		assertEquals(occurence.recurid, occ.recurid);

		occ = OccurrenceHelper.getOccurrence(series, occurence.recurid);
		assertNull(occ);

		// Same day as a standard occurrence
		occurence.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 21, 12, 0, 0, 0, tz));
		occurence.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 21, 13, 0, 0, 0, tz));
		occ = OccurrenceHelper.getOccurrence(series, occurence.dtstart);
		assertNotNull(occ);
		assertEquals(occurence.recurid, occ.recurid);
		occ = OccurrenceHelper.getOccurrence(series,
				BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 21, 1, 0, 0, 0, tz)));
		assertNotNull(occ);
		assertEquals(occ.dtstart, occ.recurid);

		// Two exception on same day occurrence
		VEventOccurrence another = VEventOccurrence.fromEvent(series.value.main,
				BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 1, 0, 0, 0, tz)));
		another.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 21, 20, 0, 0, 0, tz));
		another.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 21, 21, 0, 0, 0, tz));
		series.value.occurrences.add(another);
		occ = OccurrenceHelper.getOccurrence(series, occurence.dtstart);
		assertNotNull(occ);
		assertEquals(occurence.dtstart, occ.dtstart);
		assertEquals(occurence.recurid, occ.recurid);
		occ = OccurrenceHelper.getOccurrence(series, another.dtstart);
		assertNotNull(occ);
		assertEquals(another.dtstart, occ.dtstart);
		assertEquals(another.recurid, occ.recurid);
	}

	@Test
	public void testGetExceptionByDateStart() {
		ItemValue<VEventSeries> series = defaultVEvent();
		series.value.main.dtstart = BmDateTimeHelper
				.time(ZonedDateTime.of(2022, 2, 13, 0, 0, 0, 0, ZoneId.systemDefault()));
		series.value.main.dtend = BmDateTimeHelper
				.time(ZonedDateTime.of(2022, 2, 14, 0, 0, 0, 0, ZoneId.systemDefault()));
		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = Frequency.DAILY;
		rrule.interval = 2;
		series.value.main.rrule = rrule;

		VEventOccurrence occurence = VEventOccurrence.fromEvent(series.value.main,
				BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 19, 0, 0, 0, 0, ZoneId.systemDefault())));
		ZonedDateTime date = ZonedDateTime.of(2022, 2, 20, 0, 0, 0, 0, ZoneId.systemDefault());
		occurence.dtstart = BmDateTimeHelper.time(date);
		occurence.dtend = BmDateTimeHelper.time(date);
		series.value.occurrences = Arrays.asList(occurence);

		VEventOccurrence occ = OccurrenceHelper.getOccurrence(series, occurence.dtstart);
		assertNotNull(occ);
		assertEquals(occurence.dtstart, occ.dtstart);
		assertEquals(occurence.recurid, occ.recurid);

		occ = OccurrenceHelper.getOccurrence(series, occurence.recurid);
		assertNull(occ);

	}

	@Test
	public void testGetStandardOccurrenceByRecurid() {
		ItemValue<VEventSeries> series = defaultVEvent();
		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = Frequency.DAILY;
		rrule.interval = 2;
		series.value.main.rrule = rrule;

		BmDateTime recurid = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 1, 0, 0, 0, tz));
		Optional<VEventOccurrence> occ = OccurrenceHelper.getOccurrenceByRecurId(series, recurid);
		assertTrue(occ.isPresent());
		assertEquals(recurid, occ.get().recurid);

		occ = OccurrenceHelper.getOccurrenceByRecurId(series,
				BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 14, 1, 0, 0, 0, tz)));
		assertFalse(occ.isPresent());

		BmDateTime exdate = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 17, 1, 0, 0, 0, tz));
		series.value.main.exdate = ImmutableSet.of(exdate);
		occ = OccurrenceHelper.getOccurrenceByRecurId(series, exdate);
		assertFalse(occ.isPresent());

		series.value.main.dtstart = BmDateTimeHelper
				.time(ZonedDateTime.of(2022, 2, 13, 0, 0, 0, 0, ZoneId.systemDefault()));
		series.value.main.dtend = BmDateTimeHelper
				.time(ZonedDateTime.of(2022, 2, 14, 0, 0, 0, 0, ZoneId.systemDefault()));

		recurid = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 0, 0, 0, 0, ZoneId.systemDefault()));
		occ = OccurrenceHelper.getOccurrenceByRecurId(series, recurid);
		assertTrue(occ.isPresent());
		assertEquals(recurid, occ.get().recurid);

		occ = OccurrenceHelper.getOccurrenceByRecurId(series,
				BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 14, 0, 0, 0, 0, ZoneId.systemDefault())));
		assertFalse(occ.isPresent());

		exdate = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 17, 0, 0, 0, 0, ZoneId.systemDefault()));
		series.value.main.exdate = ImmutableSet.of(exdate);
		occ = OccurrenceHelper.getOccurrenceByRecurId(series, exdate);
		assertFalse(occ.isPresent());
	}

	@Test
	public void testGetExceptionByRecurid() {
		ItemValue<VEventSeries> series = defaultVEvent();
		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = Frequency.DAILY;
		rrule.interval = 2;
		series.value.main.rrule = rrule;

		VEventOccurrence occurence = VEventOccurrence.fromEvent(series.value.main,
				BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 19, 1, 0, 0, 0, tz)));
		occurence.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 20, 12, 0, 0, 0, tz));
		occurence.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 20, 13, 0, 0, 0, tz));
		series.value.occurrences = new ArrayList<VEventOccurrence>();
		series.value.occurrences.add(occurence);

		Optional<VEventOccurrence> occ = OccurrenceHelper.getOccurrenceByRecurId(series, occurence.recurid);
		assertTrue(occ.isPresent());
		assertEquals(occurence.dtstart, occ.get().dtstart);
		assertEquals(occurence.recurid, occ.get().recurid);

		occ = OccurrenceHelper.getOccurrenceByRecurId(series, occurence.dtstart);
		assertFalse(occ.isPresent());

		// Same day as a standard occurrence
		occurence.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 21, 12, 0, 0, 0, tz));
		occurence.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 21, 13, 0, 0, 0, tz));
		occ = OccurrenceHelper.getOccurrenceByRecurId(series, occurence.recurid);
		assertTrue(occ.isPresent());
		assertEquals(occurence.recurid, occ.get().recurid);
		assertEquals(occurence.recurid, occ.get().recurid);

		series.value.main.dtstart = BmDateTimeHelper
				.time(ZonedDateTime.of(2022, 2, 13, 0, 0, 0, 0, ZoneId.systemDefault()));
		series.value.main.dtend = BmDateTimeHelper
				.time(ZonedDateTime.of(2022, 2, 14, 0, 0, 0, 0, ZoneId.systemDefault()));

		occurence = VEventOccurrence.fromEvent(series.value.main,
				BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 19, 0, 0, 0, 0, ZoneId.systemDefault())));
		occurence.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 20, 0, 0, 0, 0, ZoneId.systemDefault()));
		occurence.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 20, 0, 0, 0, 0, ZoneId.systemDefault()));
		series.value.occurrences = Arrays.asList(occurence);

		occ = OccurrenceHelper.getOccurrenceByRecurId(series, occurence.recurid);
		assertTrue(occ.isPresent());
		assertEquals(occurence.dtstart, occ.get().dtstart);
		assertEquals(occurence.recurid, occ.get().recurid);

		occ = OccurrenceHelper.getOccurrenceByRecurId(series, occurence.dtstart);
		assertFalse(occ.isPresent());

	}

	@Test
	public void testGetOccurrenceByRecurIdWithADate() {
		ItemValue<VEventSeries> series = defaultVEvent();
		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = Frequency.DAILY;
		rrule.interval = 2;
		series.value.main.rrule = rrule;

		// Standard
		BmDateTime recurid = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 0, 0, 0, 0, tz));
		Optional<VEventOccurrence> occ = OccurrenceHelper.getOccurrenceByRecurId(series, recurid);
		assertTrue(occ.isPresent());

		VEventOccurrence occurence = VEventOccurrence.fromEvent(series.value.main,
				BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 1, 0, 0, 0, tz)));
		occurence.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 12, 0, 0, 0, tz));
		occurence.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 13, 0, 0, 0, tz));
		series.value.occurrences = Arrays.asList(occurence);

		recurid = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 0, 0, 0, 0, tz));
		occ = OccurrenceHelper.getOccurrenceByRecurId(series, recurid);
		assertTrue(occ.isPresent());

		recurid = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 16, 0, 0, 0, 0, tz));
		occ = OccurrenceHelper.getOccurrenceByRecurId(series, recurid);
		assertFalse(occ.isPresent());

		series.value.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 13, 0, 30, 0, 0, tz));
		series.value.main.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 13, 0, 30, 0, 0, tz));
		series.value.occurrences = Collections.emptyList();
		recurid = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 0, 0, 0, 0, tz));
		occ = OccurrenceHelper.getOccurrenceByRecurId(series, recurid);
		assertTrue(occ.isPresent());

		recurid = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 16, 0, 0, 0, 0, tz));
		occ = OccurrenceHelper.getOccurrenceByRecurId(series, recurid);
		assertFalse(occ.isPresent());

		occurence = VEventOccurrence.fromEvent(series.value.main,
				BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 0, 30, 0, 0, tz)));
		occurence.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 12, 0, 0, 0, tz));
		occurence.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 13, 0, 0, 0, tz));
		series.value.occurrences = Arrays.asList(occurence);

		recurid = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 0, 0, 0, 0, tz));
		occ = OccurrenceHelper.getOccurrenceByRecurId(series, recurid);
		assertTrue(occ.isPresent());

		series.value.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 13, 23, 30, 0, 0, tz));
		series.value.main.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 14, 0, 0, 0, 0, tz));
		series.value.occurrences = Collections.emptyList();
		recurid = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 0, 0, 0, 0, tz));
		occ = OccurrenceHelper.getOccurrenceByRecurId(series, recurid);
		assertTrue(occ.isPresent());

		recurid = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 16, 0, 0, 0, 0, tz));
		occ = OccurrenceHelper.getOccurrenceByRecurId(series, recurid);
		assertFalse(occ.isPresent());

		occurence = VEventOccurrence.fromEvent(series.value.main,
				BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 23, 30, 0, 0, tz)));
		occurence.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 12, 0, 0, 0, tz));
		occurence.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 13, 0, 0, 0, tz));
		series.value.occurrences = Arrays.asList(occurence);

		recurid = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 0, 0, 0, 0, tz));
		occ = OccurrenceHelper.getOccurrenceByRecurId(series, recurid);
		assertTrue(occ.isPresent());
	}

	@Test
	public void getNextOccurrenceTest() {
		ZonedDateTime eventDate = ZonedDateTime.now();
		ZonedDateTime startDate = eventDate.plusSeconds(1);

		VEvent main = new VEvent();
		main.dtstart = BmDateTimeWrapper.create(eventDate, Precision.DateTime);
		main.dtend = BmDateTimeWrapper.create(eventDate.plusHours(2), Precision.DateTime);

		main.rrule = new VEvent.RRule();
		main.rrule.frequency = VEvent.RRule.Frequency.YEARLY;
		main.rrule.count = 2;

		VEventSeries event = new VEventSeries();
		event.main = main;
		ItemValue<VEventSeries> eventIV = ItemValue.create(UUID.randomUUID().toString(), event);

		List<VEvent> occs = OccurrenceHelper.list(eventIV, BmDateTimeWrapper.create(startDate, Precision.DateTime),
				BmDateTimeWrapper.create(startDate.plusYears(3), Precision.DateTime));
		Optional<VEventOccurrence> occ = OccurrenceHelper
				.getNextOccurrence(BmDateTimeWrapper.create(startDate, Precision.DateTime), main);
		assertEquals(occs.get(1).dtstart, occ.get().dtstart);
	}

	@Test
	public void getNextOccurrenceAllDayTest() {
		ZonedDateTime eventDate = ZonedDateTime.now();
		VEvent main = new VEvent();
		main.dtstart = BmDateTimeWrapper.create(eventDate, Precision.Date);
		main.dtend = BmDateTimeWrapper.create(eventDate.plusDays(1), Precision.Date);

		main.rrule = new VEvent.RRule();
		main.rrule.frequency = VEvent.RRule.Frequency.YEARLY;
		main.rrule.count = 2;

		Optional<VEventOccurrence> nextYearOccurrence = OccurrenceHelper
				.getNextOccurrence(BmDateTimeWrapper.create(eventDate, Precision.DateTime), main);
		assertEquals(BmDateTimeWrapper.create(eventDate.plusYears(1), Precision.Date),
				nextYearOccurrence.get().dtstart);

		Optional<VEventOccurrence> nextOccurrence = OccurrenceHelper
				.getNextOccurrence(BmDateTimeWrapper.create(eventDate, Precision.Date), main);
		assertEquals(nextYearOccurrence.get().dtstart, nextOccurrence.get().dtstart);
	}

	protected ItemValue<VEventSeries> defaultVEvent() {

		VEventSeries series = new VEventSeries();
		VEvent event = new VEvent();
		event.dtstart = BmDateTimeHelper.time(refBeginDate);
		event.dtend = BmDateTimeHelper.time(refEndDate);
		event.summary = "event " + System.currentTimeMillis();
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.transparency = VEvent.Transparency.Opaque;
		event.classification = VEvent.Classification.Private;
		event.status = VEvent.Status.Confirmed;
		event.priority = 3;

		series.main = event;
		return ItemValue.create(UUID.randomUUID().toString(), series);
	}
}
