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

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import net.bluemind.calendar.api.CalendarsVEventQuery;
import net.bluemind.calendar.api.ICalendars;
import net.bluemind.calendar.api.Reminder;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventQuery;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.service.AbstractCalendarTests;
import net.bluemind.calendar.service.internal.CalendarsService;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeHelper;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemContainerValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;

public class CalendarsServiceTests extends AbstractCalendarTests {

	protected ICalendars getCalendarsService(SecurityContext context) {
		return new CalendarsService(new BmTestContext(context));
	}

	@Test
	public void testSimpleReminder() throws ServerFault {
		System.err.println("test starts");
		VEventSeries event = defaultVEvent();
		event.main.dtstart = BmDateTimeHelper.time(LocalDateTime.of(2014, 1, 1, 8, 0, 0, 0));
		event.main.alarm = new ArrayList<VAlarm>(1);
		event.main.alarm.add(VAlarm.create(-600));
		event.main.alarm.add(VAlarm.create(60));
		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		LocalDateTime dtalarm = LocalDateTime.of(2014, 1, 1, 6, 50, 0, 0);
		List<Reminder> reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		assertEquals(-600, reminder.get(0).valarm.trigger.intValue());

		dtalarm = LocalDateTime.of(2014, 1, 1, 7, 1, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		assertEquals(60, reminder.get(0).valarm.trigger.intValue());

		dtalarm = LocalDateTime
				.ofInstant(Instant.ofEpochMilli(new BmDateTimeWrapper(event.main.dtstart).toUTCTimestamp()), utcTz);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());
	}

	@Test
	public void testDailyReminder() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.dtstart = BmDateTimeHelper.time(LocalDateTime.of(2013, 1, 1, 11, 0, 0, 0));
		event.main.alarm = new ArrayList<VAlarm>(1);
		event.main.alarm.add(VAlarm.create(-600));
		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.DAILY;
		rrule.interval = 1;
		event.main.rrule = rrule;

		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		LocalDateTime dtalarm = LocalDateTime.ofInstant(
				Instant.ofEpochMilli(new BmDateTimeWrapper(event.main.dtstart).toUTCTimestamp() - 600 * 1000), utcTz);
		List<Reminder> reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
        
		assertEquals(1, reminder.size());

		// Thu 14 Aug
		dtalarm = LocalDateTime.of(2014, 8, 14, 10, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		LocalDateTime expectedDate = LocalDateTime.of(2014, 8, 14, 11, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		// Fri 15 Aug
		dtalarm = LocalDateTime.of(2014, 8, 15, 10, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		expectedDate = LocalDateTime.of(2014, 8, 15, 11, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		// Sat 16 Aug
		dtalarm = LocalDateTime.of(2014, 8, 16, 10, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		expectedDate = LocalDateTime.of(2014, 8, 16, 11, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		dtalarm = LocalDateTime.of(2014, 8, 4, 11, 0, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());
	}

	@Test
	public void testEvery14DaysReminder() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.summary = "Fin de sprint";
		event.main.dtstart = BmDateTimeHelper.time(LocalDateTime.of(2014, 3, 4, 14, 0, 0, 0));
		event.main.alarm = new ArrayList<VAlarm>(1);
		event.main.alarm.add(VAlarm.create(-600));

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.DAILY;
		rrule.interval = 14;
		event.main.rrule = rrule;

		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		LocalDateTime dtalarm = LocalDateTime.ofInstant(
				Instant.ofEpochMilli(new BmDateTimeWrapper(event.main.dtstart).toUTCTimestamp() - 600 * 1000), utcTz);
		List<Reminder> reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());

		// Thu 2 Sep
		dtalarm = LocalDateTime.of(2014, 9, 2, 13, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		LocalDateTime expectedDate = LocalDateTime.of(2014, 9, 2, 14, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		// Thu 9 Sep
		dtalarm = LocalDateTime.of(2014, 9, 9, 13, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());

		// Thu 16 Sep
		dtalarm = LocalDateTime.of(2014, 9, 16, 13, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		expectedDate = LocalDateTime.of(2014, 9, 16, 14, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		// Thu 23 Sep
		dtalarm = LocalDateTime.of(2014, 9, 23, 13, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());

	}

	@Test
	public void testWeeklyReminder() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.summary = "DS";
		event.main.dtstart = BmDateTimeHelper.time(LocalDateTime.of(2013, 1, 1, 11, 0, 0, 0));
		event.main.alarm = new ArrayList<VAlarm>(1);
		event.main.alarm.add(VAlarm.create(-600));

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.WEEKLY;
		List<VEvent.RRule.WeekDay> weekDay = new ArrayList<VEvent.RRule.WeekDay>(4);
		weekDay.add(VEvent.RRule.WeekDay.MO);
		weekDay.add(VEvent.RRule.WeekDay.TU);
		weekDay.add(VEvent.RRule.WeekDay.TH);
		weekDay.add(VEvent.RRule.WeekDay.FR);
		rrule.byDay = weekDay;

		event.main.rrule = rrule;

		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);
		LocalDateTime dtalarm = LocalDateTime.ofInstant(
				Instant.ofEpochMilli(new BmDateTimeWrapper(event.main.dtstart).toUTCTimestamp() - 600 * 1000), utcTz);
		List<Reminder> reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());

		// Thu 14 Aug
		dtalarm = LocalDateTime.of(2014, 8, 14, 10, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		LocalDateTime expectedDate = LocalDateTime.of(2014, 8, 14, 11, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		// Fri 15 Aug
		dtalarm = LocalDateTime.of(2014, 8, 15, 10, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		expectedDate = LocalDateTime.of(2014, 8, 15, 11, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		// Sat 16 Aug
		dtalarm = LocalDateTime.of(2014, 8, 16, 10, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());

		dtalarm = LocalDateTime.of(2014, 8, 4, 11, 0, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());
	}

	@Test
	public void testEvery2WeeksReminder() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.summary = "Fin de sprint";
		event.main.dtstart = BmDateTimeHelper.time(LocalDateTime.of(2014, 3, 4, 14, 0, 0, 0));
		event.main.alarm = new ArrayList<VAlarm>(1);
		event.main.alarm.add(VAlarm.create(-600));

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.WEEKLY;
		List<VEvent.RRule.WeekDay> weekDay = new ArrayList<VEvent.RRule.WeekDay>(1);
		weekDay.add(VEvent.RRule.WeekDay.TU);
		rrule.byDay = weekDay;
		rrule.interval = 2;

		event.main.rrule = rrule;

		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		LocalDateTime dtalarm = LocalDateTime.ofInstant(
				Instant.ofEpochMilli(new BmDateTimeWrapper(event.main.dtstart).toUTCTimestamp() - 600 * 1000), utcTz);
		List<Reminder> reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());

		// Thu 2 Sep
		dtalarm = LocalDateTime.of(2014, 9, 2, 13, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		LocalDateTime expectedDate = LocalDateTime.of(2014, 9, 2, 14, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		// Thu 9 Sep
		dtalarm = LocalDateTime.of(2014, 9, 9, 13, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());

		// Thu 16 Sep
		dtalarm = LocalDateTime.of(2014, 9, 16, 13, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		expectedDate = LocalDateTime.of(2014, 9, 16, 14, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		// Thu 23 Sep
		dtalarm = LocalDateTime.of(2014, 9, 23, 13, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());
	}

	@Test
	public void testMonthlyReminder() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.dtstart = BmDateTimeHelper.time(LocalDateTime.of(2014, 1, 1, 8, 0, 0, 0));
		event.main.alarm = new ArrayList<VAlarm>(1);
		event.main.alarm.add(VAlarm.create(-600));

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.MONTHLY;
		rrule.interval = 1;
		event.main.rrule = rrule;

		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		LocalDateTime dtalarm = LocalDateTime.ofInstant(
				Instant.ofEpochMilli(new BmDateTimeWrapper(event.main.dtstart).toUTCTimestamp() - 600 * 1000), utcTz);
		List<Reminder> reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());

		// 1 feb
		dtalarm = LocalDateTime.of(2014, 2, 1, 7, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		LocalDateTime expectedDate = LocalDateTime.of(2014, 2, 1, 8, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		// 2 feb
		dtalarm = LocalDateTime.of(2014, 2, 2, 7, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());

		// 1 mar
		dtalarm = LocalDateTime.of(2014, 3, 1, 7, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		expectedDate = LocalDateTime.of(2014, 3, 1, 8, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		// 2 mar
		dtalarm = LocalDateTime.of(2014, 3, 2, 7, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());
	}

	@Test
	public void testMonthlyByDayReminder() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.alarm = new ArrayList<VAlarm>(1);
		event.main.alarm.add(VAlarm.create(-600));
		event.main.dtstart = BmDateTimeHelper.time(LocalDateTime.of(2010, 2, 4, 8, 0, 0, 0));

		// Every _1st_ thurday
		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.MONTHLY;
		List<VEvent.RRule.WeekDay> weekDay = new ArrayList<VEvent.RRule.WeekDay>(1);
		weekDay.add(new VEvent.RRule.WeekDay("TH", 1));
		rrule.byDay = weekDay;
		rrule.interval = 1;
		event.main.rrule = rrule;

		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		LocalDateTime dtalarm = LocalDateTime.ofInstant(
				Instant.ofEpochMilli(new BmDateTimeWrapper(event.main.dtstart).toUTCTimestamp() - 600 * 1000), utcTz);
		List<Reminder> reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());

		dtalarm = LocalDateTime.of(2011, 1, 6, 7, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		LocalDateTime expectedDate = LocalDateTime.of(2011, 1, 6, 8, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());
		dtalarm = LocalDateTime.of(2011, 1, 7, 7, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());

		dtalarm = LocalDateTime.of(2011, 2, 3, 7, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		expectedDate = LocalDateTime.of(2011, 2, 3, 8, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());
		dtalarm = LocalDateTime.of(2011, 2, 4, 7, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());

		dtalarm = LocalDateTime.of(2011, 3, 3, 7, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		expectedDate = LocalDateTime.of(2011, 3, 3, 8, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());
		dtalarm = LocalDateTime.of(2011, 3, 4, 7, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());
	}

	@Test
	public void testYearlyReminder() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.dtstart = BmDateTimeHelper.time(LocalDateTime.of(2013, 1, 1, 8, 0, 0, 0));
		event.main.alarm = new ArrayList<VAlarm>(1);
		event.main.alarm.add(VAlarm.create(-600));

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.YEARLY;
		rrule.interval = 1;
		event.main.rrule = rrule;

		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		LocalDateTime dtalarm = LocalDateTime.ofInstant(
				Instant.ofEpochMilli(new BmDateTimeWrapper(event.main.dtstart).toUTCTimestamp() - 600 * 1000), utcTz);
		List<Reminder> reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());

		// 2014
		dtalarm = LocalDateTime.of(2014, 1, 1, 7, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		LocalDateTime expectedDate = LocalDateTime.of(2014, 1, 1, 8, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		dtalarm = LocalDateTime.of(2014, 1, 2, 7, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());

		// 2015
		dtalarm = LocalDateTime.of(2015, 1, 1, 7, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		expectedDate = LocalDateTime.of(2015, 1, 1, 8, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		dtalarm = LocalDateTime.of(2015, 1, 2, 7, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());
	}

	@Test
	public void testUntilReminder() throws ServerFault {

		ICalendars service = getCalendarsService(userSecurityContext);

		VEventSeries event = defaultVEvent();
		event.main.dtstart = BmDateTimeHelper.time(LocalDateTime.of(2013, 1, 1, 11, 0, 0, 0));
		event.main.alarm = new ArrayList<VAlarm>(1);
		event.main.alarm.add(VAlarm.create(-600));
		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.DAILY;
		rrule.interval = 1;
		rrule.until = BmDateTimeWrapper.create(LocalDateTime.of(2014, 8, 16, 0, 0, 0, 0), Precision.DateTime);
		event.main.rrule = rrule;

		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		LocalDateTime dtalarm = LocalDateTime.ofInstant(
				Instant.ofEpochMilli(new BmDateTimeWrapper(event.main.dtstart).toUTCTimestamp() - 600 * 1000), utcTz);
		List<Reminder> reminder = service.getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());

		// Tue 1 Jan 2013
		dtalarm = LocalDateTime.of(2013, 1, 1, 10, 50, 0, 0);
		reminder = service.getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		LocalDateTime expectedDate = LocalDateTime.of(2013, 1, 1, 11, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		// Mon 11 Aug
		dtalarm = LocalDateTime.of(2014, 8, 11, 10, 50, 0, 0);
		reminder = service.getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		expectedDate = LocalDateTime.of(2014, 8, 11, 11, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		// Tue 12 Aug
		dtalarm = LocalDateTime.of(2014, 8, 12, 10, 50, 0, 0);
		reminder = service.getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		expectedDate = LocalDateTime.of(2014, 8, 12, 11, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		// Wed 13 Aug
		dtalarm = LocalDateTime.of(2014, 8, 13, 10, 50, 0, 0);
		reminder = service.getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		expectedDate = LocalDateTime.of(2014, 8, 13, 11, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		// Thu 14 Aug
		dtalarm = LocalDateTime.of(2014, 8, 14, 10, 50, 0, 0);
		reminder = service.getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		expectedDate = LocalDateTime.of(2014, 8, 14, 11, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		// Fri 15 Aug
		dtalarm = LocalDateTime.of(2014, 8, 15, 10, 50, 0, 0);
		reminder = service.getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		expectedDate = LocalDateTime.of(2014, 8, 15, 11, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		// Sat 16 Aug
		dtalarm = LocalDateTime.of(2014, 8, 16, 10, 50, 0, 0);
		reminder = service.getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());

		dtalarm = LocalDateTime.of(2014, 8, 4, 11, 0, 0, 0);
		reminder = service.getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());
	}

	@Test
	public void testCountReminder() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.dtstart = BmDateTimeHelper.time(LocalDateTime.of(2013, 1, 1, 11, 0, 0, 0));
		event.main.alarm = new ArrayList<VAlarm>(1);
		event.main.alarm.add(VAlarm.create(-600));
		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.DAILY;
		rrule.interval = 1;
		rrule.count = 3;
		event.main.rrule = rrule;

		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		LocalDateTime dtalarm = LocalDateTime.ofInstant(
				Instant.ofEpochMilli(new BmDateTimeWrapper(event.main.dtstart).toUTCTimestamp() - 600 * 1000), utcTz);
		List<Reminder> reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());

		dtalarm = LocalDateTime.of(2013, 1, 2, 10, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		LocalDateTime expectedDate = LocalDateTime.of(2013, 1, 2, 11, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		dtalarm = LocalDateTime.of(2013, 1, 3, 10, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		expectedDate = LocalDateTime.of(2013, 1, 3, 11, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		dtalarm = LocalDateTime.of(2013, 1, 4, 10, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());
	}

	@Test
	public void testFutureReminder() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.dtstart = BmDateTimeHelper.time(LocalDateTime.of(2022, 1, 1, 11, 0, 0, 0));
		event.main.alarm = new ArrayList<VAlarm>(1);
		event.main.alarm.add(VAlarm.create(-600));
		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.DAILY;
		rrule.interval = 1;
		rrule.count = 3;
		event.main.rrule = rrule;

		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		LocalDateTime dtalarm = LocalDateTime.ofInstant(
				Instant.ofEpochMilli(new BmDateTimeWrapper(event.main.dtstart).toUTCTimestamp() - 600 * 1000), utcTz);
		List<Reminder> reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());

		dtalarm = LocalDateTime.of(2013, 1, 2, 10, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());

		dtalarm = LocalDateTime.of(2013, 1, 3, 10, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());

		dtalarm = LocalDateTime.of(2013, 1, 4, 10, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());
	}

	@Test
	public void testSearch() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.summary = "toto";

		String uid = "test_" + System.nanoTime();
		VEventQuery eventQuery = VEventQuery.create("value.summary:toto");

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		CalendarsVEventQuery query = CalendarsVEventQuery.create(eventQuery, Arrays.asList(userCalendarContainer.uid));

		List<ItemContainerValue<VEventSeries>> res = getCalendarsService(userSecurityContext).search(query);

		assertEquals(1, res.size());
		VEvent found = res.get(0).value.main;
		assertEquals(event.main.summary, found.summary);

		query = CalendarsVEventQuery.create(eventQuery, testUser.uid);

		res = getCalendarsService(userSecurityContext).search(query);

		assertEquals(1, res.size());
		found = res.get(0).value.main;
		assertEquals(event.main.summary, found.summary);

	}

	@Test
	public void testMultiSimpleReminder() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.dtstart = BmDateTimeHelper.time(LocalDateTime.of(2014, 1, 1, 8, 0, 0, 0));
		event.main.alarm = new ArrayList<VAlarm>(1);
		event.main.alarm.add(VAlarm.create(-600));
		event.main.alarm.add(VAlarm.create(-30));
		event.main.alarm.add(VAlarm.create(60));
		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		LocalDateTime dtalarm = LocalDateTime.of(2014, 1, 1, 6, 50, 0, 0);
		List<Reminder> reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());

		dtalarm = LocalDateTime.of(2014, 1, 1, 6, 59, 30, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());

		dtalarm = LocalDateTime.of(2014, 1, 1, 7, 1, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());

		dtalarm = LocalDateTime
				.ofInstant(Instant.ofEpochMilli(new BmDateTimeWrapper(event.main.dtstart).toUTCTimestamp()), utcTz);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());
	}

	@Test
	public void testDailyMultiReminder() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.dtstart = BmDateTimeHelper.time(LocalDateTime.of(2013, 1, 1, 11, 0, 0, 0));
		event.main.alarm = new ArrayList<VAlarm>(1);
		event.main.alarm.add(VAlarm.create(-600));
		event.main.alarm.add(VAlarm.create(-60));
		event.main.alarm.add(VAlarm.create(60));

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.DAILY;
		rrule.interval = 1;
		event.main.rrule = rrule;

		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		LocalDateTime dtalarm = LocalDateTime.ofInstant(
				Instant.ofEpochMilli(new BmDateTimeWrapper(event.main.dtstart).toUTCTimestamp() - 600 * 1000), utcTz);
		List<Reminder> reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));

		assertEquals(1, reminder.size());

		// Thu 14 Aug, 10min before
		dtalarm = LocalDateTime.of(2014, 8, 14, 10, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		LocalDateTime expectedDate = LocalDateTime.of(2014, 8, 14, 11, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());
		assertEquals(-600, reminder.get(0).valarm.trigger.intValue());

		// Thu 14 Aug, 1min before
		dtalarm = LocalDateTime.of(2014, 8, 14, 10, 59, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		expectedDate = LocalDateTime.of(2014, 8, 14, 11, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());
		assertEquals(-60, reminder.get(0).valarm.trigger.intValue());

		// Thu 14 Aug, 1min after
		dtalarm = LocalDateTime.of(2014, 8, 14, 11, 1, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		expectedDate = LocalDateTime.of(2014, 8, 14, 11, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());
		assertEquals(60, reminder.get(0).valarm.trigger.intValue());

		// Fri 15 Aug, 10min before
		dtalarm = LocalDateTime.of(2014, 8, 15, 10, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		expectedDate = LocalDateTime.of(2014, 8, 15, 11, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		// Fri 15 Aug, 1min before
		dtalarm = LocalDateTime.of(2014, 8, 15, 10, 59, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		expectedDate = LocalDateTime.of(2014, 8, 15, 11, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		// Fri 15 Aug, 1min after
		dtalarm = LocalDateTime.of(2014, 8, 15, 11, 1, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		expectedDate = LocalDateTime.of(2014, 8, 15, 11, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		// Sat 16 Aug
		dtalarm = LocalDateTime.of(2014, 8, 16, 10, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		expectedDate = LocalDateTime.of(2014, 8, 16, 11, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		dtalarm = LocalDateTime.of(2014, 8, 4, 11, 0, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());
	}

	@Test
	public void testMultiReminder() throws Exception {
		VEventSeries event = defaultVEvent();
		event.main.summary = "rec";
		event.main.dtstart = BmDateTimeHelper.time(LocalDateTime.of(2013, 1, 1, 11, 0, 0, 0));
		event.main.alarm = new ArrayList<VAlarm>(1);
		event.main.alarm.add(VAlarm.create(-600));
		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.DAILY;
		rrule.interval = 1;
		event.main.rrule = rrule;
		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		VEventSeries event2 = defaultVEvent();
		event2.main.summary = "normal";
		event2.main.dtstart = BmDateTimeHelper.time(LocalDateTime.of(2014, 8, 14, 11, 0, 0, 0));
		event2.main.alarm = new ArrayList<VAlarm>(1);
		event2.main.alarm.add(VAlarm.create(-600));
		String uid2 = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid2, event2, sendNotifications);

		// Thu 14 Aug, 10min before
		LocalDateTime dtalarm = LocalDateTime.of(2014, 8, 14, 8, 50, 0, 0);
		List<Reminder> reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));

		for (Reminder r : reminder) {
			System.err.println(r.vevent.value.summary);
		}

		assertEquals(2, reminder.size());
		LocalDateTime expectedDate = LocalDateTime.of(2014, 8, 14, 11, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(1).vevent.value.dtstart).toDateTime());

	}

	@Test
	public void testUntilReminderRDATE() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.dtstart = BmDateTimeHelper.time(LocalDateTime.of(2013, 1, 1, 11, 0, 0, 0));
		event.main.alarm = new ArrayList<VAlarm>(1);
		event.main.alarm.add(VAlarm.create(-600));
		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.DAILY;
		rrule.interval = 1;
		rrule.until = BmDateTimeWrapper.create(LocalDateTime.of(2014, 8, 16, 0, 0, 0, 0), Precision.DateTime);
		event.main.rrule = rrule;

		Set<net.bluemind.core.api.date.BmDateTime> rdate = new HashSet<>(1);
		BmDateTime rDate1 = BmDateTimeHelper.time(LocalDateTime.of(2015, 6, 13, 12, 0, 0, 0));
		rdate.add(rDate1);

		BmDateTime rDate2 = BmDateTimeHelper.time(LocalDateTime.of(2015, 7, 14, 16, 0, 0, 0));
		rdate.add(rDate2);

		event.main.rdate = rdate;

		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		// rdate1
		LocalDateTime dtalarm = LocalDateTime.of(2015, 6, 13, 9, 50, 0, 0);
		List<Reminder> reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		LocalDateTime expectedDate = LocalDateTime.of(2015, 6, 13, 12, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		// rdate2
		dtalarm = LocalDateTime.of(2015, 7, 14, 13, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		expectedDate = LocalDateTime.of(2015, 7, 14, 16, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		// rdate2, dtalarm 8min
		dtalarm = LocalDateTime.of(2015, 7, 14, 13, 52, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());

		// ramdom, dtalarm 10min
		dtalarm = LocalDateTime.of(2015, 7, 15, 13, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());

		// ramdom, dtalarm 10min
		dtalarm = LocalDateTime.of(2015, 7, 13, 13, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());
	}

	@Test
	public void testMonthlyReminderRDATE() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.dtstart = BmDateTimeHelper.time(LocalDateTime.of(2014, 1, 1, 8, 0, 0, 0));
		event.main.alarm = new ArrayList<VAlarm>(1);
		event.main.alarm.add(VAlarm.create(-600));

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.MONTHLY;
		rrule.interval = 1;
		event.main.rrule = rrule;

		Set<net.bluemind.core.api.date.BmDateTime> rdate = new HashSet<>(1);
		BmDateTime rDate1 = BmDateTimeHelper.time(LocalDateTime.of(2015, 6, 13, 12, 0, 0, 0));
		rdate.add(rDate1);

		BmDateTime rDate2 = BmDateTimeHelper.time(LocalDateTime.of(2015, 7, 14, 16, 0, 0, 0));
		rdate.add(rDate2);

		event.main.rdate = rdate;

		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		// rdate1
		LocalDateTime dtalarm = LocalDateTime.of(2015, 6, 13, 11, 50, 0, 0);
		List<Reminder> reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		LocalDateTime expectedDate = LocalDateTime.of(2015, 6, 13, 12, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		// rdate2
		dtalarm = LocalDateTime.of(2015, 7, 14, 15, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		expectedDate = LocalDateTime.of(2015, 7, 14, 16, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		// rdate2, dtalarm 8min
		dtalarm = LocalDateTime.of(2015, 7, 14, 15, 52, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());

		// ramdom, dtalarm 10min
		dtalarm = LocalDateTime.of(2015, 7, 15, 15, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());

		// ramdom, dtalarm 10min
		dtalarm = LocalDateTime.of(2015, 7, 13, 15, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());
	}

	@Test
	public void testEvery14DaysReminderRDATE() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.summary = "Fin de sprint";
		event.main.dtstart = BmDateTimeHelper.time(LocalDateTime.of(2014, 3, 4, 14, 0, 0, 0));
		event.main.alarm = new ArrayList<VAlarm>(1);
		event.main.alarm.add(VAlarm.create(-600));

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.DAILY;
		rrule.interval = 14;
		event.main.rrule = rrule;

		Set<net.bluemind.core.api.date.BmDateTime> rdate = new HashSet<>(1);
		BmDateTime rDate1 = BmDateTimeHelper.time(LocalDateTime.of(2015, 6, 13, 12, 0, 0, 0));
		rdate.add(rDate1);

		BmDateTime rDate2 = BmDateTimeHelper.time(LocalDateTime.of(2015, 7, 14, 16, 0, 0, 0));
		rdate.add(rDate2);

		event.main.rdate = rdate;

		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		// rdate1
		LocalDateTime dtalarm = LocalDateTime.of(2015, 6, 13, 11, 50, 0, 0);
		List<Reminder> reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		LocalDateTime expectedDate = LocalDateTime.of(2015, 6, 13, 12, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		// rdate2
		dtalarm = LocalDateTime.of(2015, 7, 14, 15, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		expectedDate = LocalDateTime.of(2015, 7, 14, 16, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		// rdate2, dtalarm 8min
		dtalarm = LocalDateTime.of(2015, 7, 14, 15, 52, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());
	}

	@Test
	public void testDailyReminderRDATE() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.dtstart = BmDateTimeHelper.time(LocalDateTime.of(2013, 1, 1, 11, 0, 0, 0));
		event.main.alarm = new ArrayList<VAlarm>(1);
		event.main.alarm.add(VAlarm.create(-600));
		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.DAILY;
		rrule.interval = 1;
		event.main.rrule = rrule;

		Set<net.bluemind.core.api.date.BmDateTime> rdate = new HashSet<>(1);
		BmDateTime rDate1 = BmDateTimeHelper.time(LocalDateTime.of(2014, 6, 13, 12, 0, 0, 0));
		rdate.add(rDate1);

		BmDateTime rDate2 = BmDateTimeHelper.time(LocalDateTime.of(2014, 7, 14, 16, 0, 0, 0));
		rdate.add(rDate2);

		event.main.rdate = rdate;

		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		// rdate1
		LocalDateTime dtalarm = LocalDateTime.of(2014, 6, 13, 11, 50, 0, 0);
		List<Reminder> reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		LocalDateTime expectedDate = LocalDateTime.of(2014, 6, 13, 12, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		// rdate2
		dtalarm = LocalDateTime.of(2014, 7, 14, 15, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
		expectedDate = LocalDateTime.of(2014, 7, 14, 16, 0, 0, 0);
		assertEquals(expectedDate, new BmDateTimeWrapper(reminder.get(0).vevent.value.dtstart).toDateTime());

		// rdate2, dtalarm 8min
		dtalarm = LocalDateTime.of(2014, 7, 14, 15, 52, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());
	}

	@Test
	public void testDailyReminderWithRecurIds() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.dtstart = BmDateTimeHelper.time(LocalDateTime.of(2013, 1, 1, 11, 0, 0, 0));
		event.main.alarm = new ArrayList<VAlarm>(1);
		event.main.alarm.add(VAlarm.create(-600));
		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.DAILY;
		rrule.interval = 1;
		event.main.rrule = rrule;
		String uid = "test_" + System.nanoTime();

		VEventOccurrence event2 = recurringVEvent();
		event2.dtstart = BmDateTimeHelper.time(LocalDateTime.of(2014, 8, 14, 15, 0, 0, 0));
		event2.alarm = new ArrayList<VAlarm>(1);
		event2.alarm.add(VAlarm.create(-600));
		event2.recurid = BmDateTimeHelper.time(LocalDateTime.of(2014, 8, 14, 11, 0, 0, 0));
		event2.rrule = new VEvent.RRule();
		event2.rrule.frequency = VEvent.RRule.Frequency.DAILY;
		event2.rrule.interval = 1;
		event.occurrences = Arrays.asList(event2);

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		LocalDateTime dtalarm = LocalDateTime.of(2014, 8, 13, 10, 50, 0, 0);
		List<Reminder> reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());

		// Thu 14 Aug
		dtalarm = LocalDateTime.of(2014, 8, 14, 10, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(0, reminder.size());
		dtalarm = LocalDateTime.of(2014, 8, 14, 14, 50, 0, 0);
		reminder = getCalendarsService(userSecurityContext).getReminder(BmDateTimeHelper.time(dtalarm));
		assertEquals(1, reminder.size());
	}
}
