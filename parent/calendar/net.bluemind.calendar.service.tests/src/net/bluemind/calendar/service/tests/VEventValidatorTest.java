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
package net.bluemind.calendar.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;

import org.junit.Test;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventCounter;
import net.bluemind.calendar.api.VEventCounter.CounterOriginator;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.service.internal.VEventValidator;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.Organizer;
import net.bluemind.icalendar.api.ICalendarElement.RRule.Frequency;
import net.bluemind.icalendar.api.ICalendarElement.RRule.WeekDay;
import net.bluemind.icalendar.api.ICalendarElement.Role;

public class VEventValidatorTest {

	private VEventValidator validator = new VEventValidator();
	private ZoneId defaultTz = ZoneId.systemDefault();

	@Test
	public void testValidate() {
		VEvent vevent = null;
		ErrorCode err = null;

		// vevent null
		try {
			validator.validate(vevent);
		} catch (ServerFault e) {
			err = e.getCode();
		}
		assertTrue(ErrorCode.EVENT_ERROR == err);

		// dtstart null
		vevent = new VEvent();
		err = null;
		try {
			validator.validate(vevent);
		} catch (ServerFault e) {
			err = e.getCode();
		}
		assertTrue(ErrorCode.NO_EVENT_DATE == err);

		// dtstart != null
		vevent.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2015, 05, 01, 0, 0, 0, 0, defaultTz),
				Precision.Date);
		vevent.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2015, 05, 01, 1, 0, 0, 0, defaultTz), Precision.Date);

		err = null;
		try {
			validator.validate(vevent);
		} catch (ServerFault e) {
			err = e.getCode();
		}
		assertTrue(ErrorCode.EMPTY_EVENT_TITLE == err);

		// summary != null
		vevent.summary = "event " + System.currentTimeMillis();
		err = null;
		try {
			validator.validate(vevent);
		} catch (ServerFault e) {
			err = e.getCode();
		}
		assertNull(err);

		// rrule
		VEvent.RRule rrule = new VEvent.RRule();
		vevent.rrule = rrule;
		try {
			validator.validate(vevent);
		} catch (ServerFault e) {
			err = e.getCode();
		}
		assertTrue(ErrorCode.EVENT_ERROR == err);

		rrule.frequency = VEvent.RRule.Frequency.DAILY;
		vevent.rrule = rrule;
		err = null;
		try {
			validator.validate(vevent);
		} catch (ServerFault e) {
			err = e.getCode();
		}
		assertNull(err);

	}

	@Test
	public void testList() throws ServerFault {
		VEvent vevent = new VEvent();
		vevent.summary = "check";
		vevent.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2015, 05, 01, 0, 0, 0, 0, defaultTz),
				Precision.Date);
		vevent.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2015, 05, 01, 1, 0, 0, 0, defaultTz), Precision.Date);
		vevent.rrule = new VEvent.RRule();
		vevent.rrule.frequency = Frequency.DAILY;

		validator.validate(vevent);

		try {
			vevent.rrule.bySecond = Arrays.asList(1, 20, 70);
			validator.validate(vevent);
		} catch (ServerFault e) {
			assertEquals(ErrorCode.EVENT_ERROR, e.getCode());
		}
		vevent.rrule.bySecond = Arrays.asList(1, 20, 59);
		validator.validate(vevent);

		try {
			vevent.rrule.byMinute = Arrays.asList(1, 20, 70);
			validator.validate(vevent);
		} catch (ServerFault e) {
			assertEquals(ErrorCode.EVENT_ERROR, e.getCode());
		}

		vevent.rrule.byMinute = Arrays.asList(1, 20, 59);
		validator.validate(vevent);

		try {
			vevent.rrule.byHour = Arrays.asList(1, 20, 24);
			validator.validate(vevent);
		} catch (ServerFault e) {
			assertEquals(ErrorCode.EVENT_ERROR, e.getCode());
		}
		vevent.rrule.byHour = Arrays.asList(1, 20, 23);
		validator.validate(vevent);

		try {
			vevent.rrule.byMonthDay = Arrays.asList(1, 20, 24, 32);
			validator.validate(vevent);
		} catch (ServerFault e) {
			assertEquals(ErrorCode.EVENT_ERROR, e.getCode());
		}
		vevent.rrule.byMonthDay = Arrays.asList(1, 20, 24, 31);
		validator.validate(vevent);

		try {
			vevent.rrule.byYearDay = Arrays.asList(1, 20, 24, 367);
			validator.validate(vevent);
		} catch (ServerFault e) {
			assertEquals(ErrorCode.EVENT_ERROR, e.getCode());
		}
		vevent.rrule.byYearDay = Arrays.asList(1, 20, 24, 366);
		validator.validate(vevent);

		try {
			vevent.rrule.byWeekNo = Arrays.asList(1, 20, 24, 54);
			validator.validate(vevent);
		} catch (ServerFault e) {
			assertEquals(ErrorCode.EVENT_ERROR, e.getCode());
		}
		vevent.rrule.byWeekNo = Arrays.asList(1, 20, 24, 53);
		validator.validate(vevent);

		try {
			vevent.rrule.byMonth = Arrays.asList(1, 2, 5, 13);
			validator.validate(vevent);
		} catch (ServerFault e) {
			assertEquals(ErrorCode.EVENT_ERROR, e.getCode());
		}
		vevent.rrule.byMonth = Arrays.asList(1, 12);
		validator.validate(vevent);

	}

	@Test
	public void testWeeklyWithoutDays() {
		VEvent vevent = new VEvent();
		vevent.summary = "check";
		vevent.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2015, 05, 01, 0, 0, 0, 0, defaultTz),
				Precision.Date);
		vevent.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2015, 05, 01, 1, 0, 0, 0, defaultTz), Precision.Date);
		vevent.rrule = new VEvent.RRule();
		vevent.rrule.frequency = Frequency.WEEKLY;

		try {
			validator.validate(vevent);
			assertEquals(1, vevent.rrule.byDay.size());
			assertEquals(WeekDay.fr(), vevent.rrule.byDay.get(0));
		} catch (ServerFault e) {
			fail();
		}

	}

	@Test
	public void testDtEnd() {
		VEvent vevent = new VEvent();
		vevent.summary = "bang";

		// DtEnd < DtStart
		vevent.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2017, 05, 17, 8, 0, 0, 0, defaultTz),
				Precision.DateTime);
		vevent.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2017, 05, 17, 7, 0, 0, 0, defaultTz),
				Precision.DateTime);

		try {
			validator.validate(vevent);
			fail("dtend is prior to dtstart");
		} catch (ServerFault e) {
		}

		// DtEnd > DtStart
		vevent.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2017, 05, 17, 8, 0, 0, 0, defaultTz),
				Precision.DateTime);
		vevent.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2017, 05, 17, 9, 0, 0, 0, defaultTz),
				Precision.DateTime);

		try {
			validator.validate(vevent);
		} catch (ServerFault e) {
			fail();
		}

		// DtEnd == DtStart
		vevent.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2017, 05, 17, 8, 0, 0, 0, defaultTz),
				Precision.DateTime);
		vevent.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2017, 05, 17, 8, 0, 0, 0, defaultTz),
				Precision.DateTime);

		try {
			validator.validate(vevent);
		} catch (ServerFault e) {
			fail();
		}
	}

	@Test
	public void testCounterPropositionValidation() {
		VEvent vevent = new VEvent();
		vevent.summary = "bang";

		// DtEnd < DtStart
		vevent.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2017, 05, 17, 8, 0, 0, 0, defaultTz),
				Precision.DateTime);
		vevent.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2017, 05, 17, 9, 0, 0, 0, defaultTz),
				Precision.DateTime);
		VEventSeries series = new VEventSeries();
		VEventCounter counter = new VEventCounter();
		counter.originator = new CounterOriginator();
		counter.counter = VEventOccurrence.fromEvent(vevent, null);
		Attendee attendee = new Attendee();
		attendee.role = Role.RequiredParticipant;
		attendee.mailto = "part@test.loc";
		counter.counter.attendees = Arrays.asList(attendee);
		counter.counter.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2017, 05, 17, 9, 0, 0, 0, defaultTz),
				Precision.DateTime);
		vevent.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2017, 05, 17, 10, 0, 0, 0, defaultTz),
				Precision.DateTime);
		vevent.organizer = new Organizer("test@test.loc");

		series.main = vevent;
		series.counters = Arrays.asList(counter);
		series.acceptCounters = true;

		validator.validate(series);

		series.acceptCounters = false;
		try {
			validator.validate(series);
			fail();
		} catch (Exception e) {
		}

		counter.counter.attendees.get(0).role = Role.NonParticipant; // added attendee, does not trigger counter check
		validator.validate(series);
	}
}
