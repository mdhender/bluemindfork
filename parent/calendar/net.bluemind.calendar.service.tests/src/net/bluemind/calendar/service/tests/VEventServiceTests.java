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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import net.bluemind.attachment.api.AttachedFile;
import net.bluemind.calendar.api.IVEvent;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEvent.Transparency;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.service.AbstractCalendarTests;
import net.bluemind.calendar.service.internal.VEventService;
import net.bluemind.core.api.ImportStats;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.api.TaskStatus.State;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm.Action;
import net.bluemind.tests.defaultdata.BmDateTimeHelper;
import net.bluemind.utils.FileUtils;

public class VEventServiceTests extends AbstractCalendarTests {

	/**
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	private Stream getIcsFromFile(String filename) throws IOException {
		InputStream in = this.getClass().getClassLoader().getResourceAsStream("ics/" + filename);
		String ics = FileUtils.streamString(in, true);
		in.close();
		return GenericStream.simpleValue(ics, s -> s.getBytes());
	}

	ZoneId hoChiMinhTz = ZoneId.of("Asia/Ho_Chi_Minh");

	@Test
	public void testExportOne() throws ServerFault {
		VEventSeries vevent = defaultVEvent();

		VEvent.Attendee john = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.RequiredParticipant,
				VEvent.ParticipationStatus.NeedsAction, false, "", "", "", "John Bang", "", "", null,
				"john.bang@domain.lan");
		vevent.main.attendees.add(john);

		VEvent.Attendee jane = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.RequiredParticipant,
				VEvent.ParticipationStatus.NeedsAction, true, "", "", "", "Jane Bang", "", "", null,
				"jane.bang@domain.lan");
		vevent.main.attendees.add(jane);

		vevent.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(1983, 2, 13, 2, 0, 0, 0, hoChiMinhTz));
		vevent.main.summary = "testExport à 4€";
		vevent.main.alarm = new ArrayList<>(1);

		vevent.main.alarm.add(ICalendarElement.VAlarm.create(Action.Email, -600, "alarm", 30, 0, "yaaaaay"));

		Set<net.bluemind.core.api.date.BmDateTime> exdate = new HashSet<>(3);
		ZonedDateTime exDate = ZonedDateTime.of(1983, 2, 13, 10, 0, 0, 0, hoChiMinhTz);
		exdate.add(BmDateTimeHelper.time(exDate));
		ZonedDateTime exDate2 = ZonedDateTime.of(2012, 3, 31, 8, 30, 0, 0, hoChiMinhTz);
		exdate.add(BmDateTimeHelper.time(exDate2));
		ZonedDateTime exDate3 = ZonedDateTime.of(2014, 7, 14, 1, 2, 3, 0, hoChiMinhTz);
		exdate.add(BmDateTimeHelper.time(exDate3));

		// add duplicate
		exdate.add(BmDateTimeHelper.time(exDate3));
		ZonedDateTime exDate4 = ZonedDateTime.of(2014, 7, 14, 1, 2, 3, 0, hoChiMinhTz);
		exdate.add(BmDateTimeHelper.time(exDate4));

		vevent.main.exdate = exdate;

		// RDATES
		Set<net.bluemind.core.api.date.BmDateTime> rdates = new HashSet<>(2);
		BmDateTime rdate = BmDateTimeHelper.time(ZonedDateTime.of(1983, 2, 13, 13, 0, 0, 0, hoChiMinhTz));
		rdates.add(rdate);
		BmDateTime rdate2 = BmDateTimeHelper.time(ZonedDateTime.of(1983, 2, 22, 13, 0, 0, 0, hoChiMinhTz));
		rdates.add(rdate2);
		// add twice
		rdates.add(rdate2);

		vevent.main.rdate = rdates;

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.WEEKLY;
		rrule.interval = 2;
		rrule.until = BmDateTimeHelper.time(ZonedDateTime.of(2022, 12, 25, 13, 30, 0, 0, hoChiMinhTz));

		rrule.bySecond = Arrays.asList(10, 20);

		rrule.byMinute = Arrays.asList(1, 2, 3);

		rrule.byHour = Arrays.asList(2, 22);

		List<VEvent.RRule.WeekDay> weekDay = new ArrayList<VEvent.RRule.WeekDay>(4);
		weekDay.add(VEvent.RRule.WeekDay.MO);
		weekDay.add(VEvent.RRule.WeekDay.TU);
		weekDay.add(VEvent.RRule.WeekDay.TH);
		weekDay.add(VEvent.RRule.WeekDay.FR);
		rrule.byDay = weekDay;

		rrule.byMonthDay = Arrays.asList(2, 3);

		rrule.byYearDay = Arrays.asList(8, 13, 42, 200);

		rrule.byWeekNo = Arrays.asList(8, 13, 42);

		rrule.byMonth = Arrays.asList(8);

		vevent.main.rrule = rrule;

		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, vevent, sendNotifications);

		// test anonymous
		try {
			getVEventService(SecurityContext.ANONYMOUS, userCalendarContainer).exportIcs(uid);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		String export = getVEventService(userSecurityContext, userCalendarContainer).exportIcs(uid);
		System.err.println(export);
		assertTrue(export.contains("BEGIN:VCALENDAR"));
		assertTrue(export.contains("PRODID:-//BlueMind//BlueMind Calendar//FR"));
		assertTrue(export.contains("VERSION:2.0"));
		assertTrue(export.contains("CALSCALE:GREGORIAN"));

		assertTrue(export.contains("BEGIN:VTIMEZONE"));
		assertTrue(export.contains("TZID:Asia/Ho_Chi_Minh"));
		assertTrue(export.contains("TZURL:http://tzurl.org/zoneinfo-outlook/Asia/Ho_Chi_Minh"));
		assertTrue(export.contains("X-LIC-LOCATION:Asia/Ho_Chi_Minh"));
		assertTrue(export.contains("BEGIN:STANDARD"));
		assertTrue(export.contains("TZOFFSETFROM:+0700"));
		assertTrue(export.contains("TZOFFSETTO:+0700"));
		assertTrue(export.contains("TZNAME:ICT"));
		assertTrue(export.contains("DTSTART:19700101T000000"));
		assertTrue(export.contains("END:STANDARD"));
		assertTrue(export.contains("END:VTIMEZONE"));

		assertTrue(export.contains("BEGIN:VEVENT"));
		// do not care about dtstamp value
		assertTrue(export.contains("DTSTAMP:"));
		assertTrue(export.contains("VERSION:2.0"));
		assertTrue(export.contains("UID:" + uid));
		assertTrue(export.contains("DTSTART;TZID=Asia/Ho_Chi_Minh:19830213T020000"));
		assertTrue(export.contains("SUMMARY:" + vevent.main.summary));
		assertTrue(export.contains("CLASS:PRIVATE"));
		assertTrue(export.contains("TRANSP:OPAQUE"));
		assertTrue(export.contains("DESCRIPTION:Lorem ipsum"));
		assertTrue(export.contains("LOCATION:Toulouse"));
		assertTrue(export.contains("PRIORITY:3"));
		assertTrue(export.contains("STATUS:CONFIRMED"));
		assertTrue(export.contains(
				"ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=CHAIR;PARTSTAT=ACCEPTED;RSVP=TRUE;CN=osef:MAILTO:external@attendee.lan"));
		assertTrue(export.contains(
				"ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=FALSE;CN=John Bang:MAILTO:john.bang@domain.lan"));
		assertTrue(export.contains(
				"ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=TRUE;CN=Jane Bang:MAILTO:jane.bang@domain.lan"));
		assertTrue(export.contains("ORGANIZER;CN=" + testUser.value.contactInfos.identification.formatedName.value
				+ ":mailto:" + testUser.value.login + "@bm.lan"));
		assertTrue(export.contains("EXDATE;TZID=Asia/Ho_Chi_Minh:19830213T100000,20120331T083000,20140714T010203"));
		assertTrue(export.contains("RDATE;TZID=Asia/Ho_Chi_Minh:19830213T130000,19830222T130000"));
		assertTrue(export.contains("CATEGORIES:tag1,tag2") || export.contains("CATEGORIES:tag2,tag1"));
		assertTrue(export.contains(
				"RRULE:FREQ=WEEKLY;UNTIL=20221225T133000;INTERVAL=2;BYMONTH=8;BYWEEKNO=8,13,42;BYYEARDAY=8,13,42,200;BYMONTHDAY=2,3;BYDAY=MO,TU,TH,FR;BYHOUR=2,22;BYMINUTE=1,2,3;BYSECOND=10,20"));
		assertTrue(export.contains("BEGIN:VALARM"));
		assertTrue(export.contains("TRIGGER;VALUE=DURATION:-PT600S"));
		assertTrue(export.contains("ACTION:EMAIL"));
		assertTrue(export.contains("DESCRIPTION:alarm"));
		assertTrue(export.contains("SUMMARY:yaaaaay"));
		assertTrue(export.contains("DURATION:PT30S"));
		assertTrue(export.contains("REPEAT:0"));
		assertTrue(export.contains("END:VALARM"));
		assertTrue(export.contains("END:VEVENT"));
		assertTrue(export.contains("END:VCALENDAR"));

		assertTrue(export.contains("X-MICROSOFT-DISALLOW-COUNTER:TRUE"));
		assertTrue(export.contains("X-MICROSOFT-CDO-BUSYSTATUS:BUSY"));
		assertTrue(export.contains("X-MOZ-LASTACK:"));

		assertTrue(export.contains("ATTACH;X-FILE-NAME=test.gif:http://somewhere/1"));
		assertTrue(export.contains("ATTACH;X-FILE-NAME=test.png:http://somewhere/2"));

	}

	@Test
	public void testExportMultipleVAlarm() throws Exception {
		VEventSeries vevent = defaultVEvent();

		VEvent.Attendee john = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.RequiredParticipant,
				VEvent.ParticipationStatus.NeedsAction, false, "", "", "", "John Bang", "", "", null,
				"john.bang@domain.lan");
		vevent.main.attendees.add(john);

		vevent.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(1983, 2, 13, 2, 0, 0, 0, tz));
		vevent.main.summary = "testExportMultipleVAlarm";
		vevent.main.alarm = new ArrayList<>(1);

		vevent.main.alarm.add(ICalendarElement.VAlarm.create(Action.Email, -600, "email alarm", 30, 0, "AA"));
		vevent.main.alarm.add(ICalendarElement.VAlarm.create(Action.Display, -60, "display alarm", -10, 0, "BB"));
		vevent.main.alarm.add(ICalendarElement.VAlarm.create(Action.Audio, 200, "audio alarm", 5, 5, "CC"));

		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, vevent, sendNotifications);
		String export = getVEventService(userSecurityContext, userCalendarContainer).exportIcs(uid);
		System.err.println(export);

		assertTrue(export.contains("BEGIN:VALARM"));
		assertTrue(export.contains("TRIGGER;VALUE=DURATION:-PT600S"));
		assertTrue(export.contains("ACTION:EMAIL"));
		assertTrue(export.contains("DESCRIPTION:email alarm"));
		assertTrue(export.contains("SUMMARY:AA"));
		assertTrue(export.contains("DURATION:PT30S"));
		assertTrue(export.contains("REPEAT:0"));
		assertTrue(export.contains("END:VALARM"));

		assertTrue(export.contains("BEGIN:VALARM"));
		assertTrue(export.contains("TRIGGER;VALUE=DURATION:-PT60S"));
		assertTrue(export.contains("ACTION:DISPLAY"));
		assertTrue(export.contains("DESCRIPTION:display alarm"));
		assertTrue(export.contains("SUMMARY:BB"));
		assertTrue(export.contains("DURATION:-PT10S"));
		assertTrue(export.contains("REPEAT:0"));
		assertTrue(export.contains("END:VALARM"));

		assertTrue(export.contains("BEGIN:VALARM"));
		assertTrue(export.contains("TRIGGER;VALUE=DURATION:PT200S"));
		assertTrue(export.contains("ACTION:AUDIO"));
		assertTrue(export.contains("DESCRIPTION:audio alarm"));
		assertTrue(export.contains("SUMMARY:CC"));
		assertTrue(export.contains("DURATION:PT5S"));
		assertTrue(export.contains("REPEAT:5"));
		assertTrue(export.contains("END:VALARM"));
	}

	@Test
	public void testExportMonthlyByDay() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2010, 2, 4, 17, 0, 0, 0, defaultTz));
		event.main.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2010, 2, 4, 18, 0, 0, 0, defaultTz));

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

		String export = getVEventService(userSecurityContext, userCalendarContainer).exportIcs(uid);
		assertTrue(export.contains("RRULE:FREQ=MONTHLY;INTERVAL=1;BYDAY=1TH"));

		event = defaultVEvent();
		event.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2010, 2, 4, 17, 0, 0, 0, defaultTz));
		event.main.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2010, 2, 4, 18, 0, 0, 0, defaultTz));

		// Every _LAST_ monday
		rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.MONTHLY;
		weekDay = new ArrayList<VEvent.RRule.WeekDay>(1);
		weekDay.add(new VEvent.RRule.WeekDay("MO", -1));
		rrule.byDay = weekDay;
		rrule.interval = 1;
		event.main.rrule = rrule;

		uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		export = getVEventService(userSecurityContext, userCalendarContainer).exportIcs(uid);
		assertTrue(export.contains("RRULE:FREQ=MONTHLY;INTERVAL=1;BYDAY=-1MO"));
	}

	@Test
	public void testExportAll() throws ServerFault {
		VEventSeries vevent1 = defaultVEvent();
		vevent1.main.summary = "Event 1";

		vevent1.main.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2005, 1, 1, 5, 0, 0, 0, tz),
				Precision.DateTime);
		vevent1.main.priority = 2;
		vevent1.main.description = "yé 1 €uro";
		vevent1.main.location = "Là";
		String uid1 = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid1, vevent1, sendNotifications);

		VEventSeries vevent2 = defaultVEvent();
		vevent2.main.summary = "Event 2";
		// DTSTART;TZID=Europe/Paris:19860616T000000"
		vevent2.main.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2011, 6, 16, 0, 0, 0, 0, tz),
				Precision.DateTime);
		vevent2.main.alarm = new ArrayList<ICalendarElement.VAlarm>(1);
		ICalendarElement.VAlarm alarm = ICalendarElement.VAlarm.create(-600);
		vevent2.main.alarm.add(alarm);

		String uid2 = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid2, vevent2, sendNotifications);

		// test anonymous
		try {
			GenericStream
					.streamToString(getVEventService(SecurityContext.ANONYMOUS, userCalendarContainer).exportAll());
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		String export = GenericStream
				.streamToString(getVEventService(userSecurityContext, userCalendarContainer).exportAll());

		System.out.println(export);
		assertTrue(export.contains("BEGIN:VCALENDAR"));
		assertTrue(export.contains("PRODID:-//BlueMind//BlueMind Calendar//FR"));
		assertTrue(export.contains("VERSION:2.0"));
		assertTrue(export.contains("CALSCALE:GREGORIAN"));

		// vevent1
		assertTrue(export.contains("BEGIN:VEVENT"));
		// do not care about dtstamp value
		assertTrue(export.contains("DTSTAMP:"));
		assertTrue(export.contains("VERSION:2.0"));
		assertTrue(export.contains("UID:" + uid1));
		assertTrue(export.contains("DTSTART;TZID=Europe/Paris:20050101T050000"));
		assertTrue(export.contains("SUMMARY:" + vevent1.main.summary));
		assertTrue(export.contains("CLASS:PRIVATE"));
		assertTrue(export.contains("TRANSP:OPAQUE"));
		assertTrue(export.contains("DESCRIPTION:" + vevent1.main.description));
		assertTrue(export.contains("LOCATION:" + vevent1.main.location));
		assertTrue(export.contains("PRIORITY:2"));
		assertTrue(export.contains("STATUS:CONFIRMED"));
		assertTrue(export.contains(
				"ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=CHAIR;PARTSTAT=ACCEPTED;RSVP=TRUE;CN=osef:MAILTO:external@attendee.lan"));
		assertTrue("expected " + export,
				export.contains("ORGANIZER;CN=" + testUser.value.contactInfos.identification.formatedName.value
						+ ":mailto:" + testUser.value.login + "@bm.lan"));
		assertTrue(export.contains("END:VEVENT"));

		// vevent2
		assertTrue(export.contains("BEGIN:VEVENT"));
		// do not care about dtstamp value
		assertTrue(export.contains("DTSTAMP:"));
		assertTrue(export.contains("VERSION:2.0"));
		assertTrue(export.contains("UID:" + uid2));
		assertTrue(export.contains("DTSTART;TZID=Europe/Paris:20110616T000000"));
		assertTrue(export.contains("SUMMARY:" + vevent2.main.summary));
		assertTrue(export.contains("CLASS:PRIVATE"));
		assertTrue(export.contains("TRANSP:OPAQUE"));
		assertTrue(export.contains("DESCRIPTION:" + vevent2.main.description));
		assertTrue(export.contains("LOCATION:" + vevent2.main.location));
		assertTrue(export.contains("PRIORITY:2"));
		assertTrue(export.contains("STATUS:CONFIRMED"));
		assertTrue(export.contains(
				"ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=CHAIR;PARTSTAT=ACCEPTED;RSVP=TRUE;CN=osef:MAILTO:external@attendee.lan"));
		assertTrue(export.contains("ORGANIZER;CN=" + testUser.value.contactInfos.identification.formatedName.value
				+ ":mailto:" + testUser.value.login + "@bm.lan"));
		assertTrue(export.contains("BEGIN:VALARM"));
		assertTrue(export.contains("TRIGGER;VALUE=DURATION:-PT600S"));
		assertTrue(export.contains("ACTION:EMAIL"));
		assertTrue(export.contains("END:VEVENT"));

		assertTrue(export.contains("END:VCALENDAR"));

	}

	@Test
	public void testExportAll_EmptyCalendar() throws ServerFault {
		String export = GenericStream
				.streamToString(getVEventService(userSecurityContext, userCalendarContainer).exportAll());

		System.out.println(export);
		assertTrue(export.contains("BEGIN:VCALENDAR"));
		assertTrue(export.contains("PRODID:-//BlueMind//BlueMind Calendar//FR"));
		assertTrue(export.contains("VERSION:2.0"));
		assertTrue(export.contains("CALSCALE:GREGORIAN"));
		assertTrue(export.contains("END:VCALENDAR"));
	}

	@Test
	public void testSimpleImport() throws ServerFault, IOException {
		Stream ics = getIcsFromFile("testSimpleImport.ics");

		TaskRef taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		ImportStats stats = waitImportEnd(taskRef);
		assertNotNull(stats);
		assertEquals(1, stats.importedCount());

		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete("95c659b1-eaf8-4145-a314-9cb4566636b8");

		VEvent vevent = item.value.occurrences.get(0);
		assertNotNull(vevent);

		ZoneId tz = ZoneId.of("Pacific/Noumea");
		ZonedDateTime dtstart = ZonedDateTime.of(1983, 2, 13, 2, 0, 0, 0, tz);

		assertEquals(dtstart.toInstant().toEpochMilli(), new BmDateTimeWrapper(vevent.dtstart).toUTCTimestamp());
		assertEquals("Pacific/Noumea", vevent.timezone());
		assertEquals(dtstart, new BmDateTimeWrapper(vevent.dtstart).toDateTime());

		assertEquals("TestSimpleImport", vevent.summary);
		assertEquals(VEvent.Classification.Public, vevent.classification);
		assertEquals(VEvent.Transparency.Opaque, vevent.transparency);
		assertNull(vevent.description);
		assertNull(vevent.location);
		assertNull(vevent.priority);
		assertNull(vevent.alarm);
		assertNull(vevent.status);
		assertNotNull(vevent.attendees);
		assertEquals(0, vevent.attendees.size());
		assertNull(vevent.exdate);
		assertNull(vevent.rrule);
		assertNotNull(vevent.categories);
		assertTrue(vevent.categories.isEmpty());

	}

	@Test
	public void testAttachmentImport() throws ServerFault, IOException {
		Stream ics = getIcsFromFile("testAttachmentImport.ics");

		TaskRef taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		ImportStats stats = waitImportEnd(taskRef);
		assertNotNull(stats);
		assertEquals(1, stats.importedCount());

		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete("95c659b1-eaf8-4145-a314-9cb4566636b8");

		VEvent vevent = item.value.main;
		assertNotNull(vevent);

		assertEquals("TestAttachmentImport", vevent.summary);
		assertEquals(2, vevent.attachments.size());

		List<AttachedFile> attachments = vevent.attachments;
		int checked = 0;
		for (AttachedFile attachedFile : attachments) {
			if (attachedFile.name.equals("test.gif")) {
				assertEquals("http://somewhere/1", attachedFile.publicUrl);
				checked++;
			} else if (attachedFile.name.equals("test.png")) {
				assertEquals("http://somewhere/2", attachedFile.publicUrl);
				checked++;
			}
		}
		assertEquals(2, checked);
	}

	@Test
	public void testBinaryAttachmentImport() throws ServerFault, IOException {
		Stream ics = getIcsFromFile("testBinaryAttachmentImport.ics");

		TaskRef taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		ImportStats stats = waitImportEnd(taskRef);
		assertNotNull(stats);
		assertEquals(1, stats.importedCount());

		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete("95c659b1-eaf8-4145-a314-9cb4566636b8");

		VEvent vevent = item.value.main;
		assertNotNull(vevent);

		assertEquals("TestAttachmentImport", vevent.summary);
		assertEquals(2, vevent.attachments.size());

		List<AttachedFile> attachments = vevent.attachments;
		int checked = 0;
		for (AttachedFile attachedFile : attachments) {
			if (attachedFile.name.equals("test.gif")) {
				assertEquals("http://somewhere/1", attachedFile.publicUrl);
				checked++;
			} else if (attachedFile.name.equals("attachment_1.txt")) {
				assertTrue(attachedFile.publicUrl.startsWith("https://"));
				checked++;
			}
		}
		assertEquals(2, checked);
	}

	@Test
	public void testImport() throws ServerFault, IOException {
		Stream ics = getIcsFromFile("testImport.ics");

		LoggerFactory.getLogger(VEventServiceTests.class).info(String.format("ics: %s", ics));

		// test anonymous
		try {
			TaskRef taskRef = getVEventService(SecurityContext.ANONYMOUS, userCalendarContainer).importIcs(ics);
			waitImportEnd(taskRef);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		TaskRef taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		ImportStats stats = waitImportEnd(taskRef);

		assertNotNull(stats);
		assertEquals(1, stats.importedCount());

		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete("ced586fb-836c-462b-91d8-2c6bae2cd6ad");
		VEvent vevent = item.value.main;
		assertNotNull(vevent);

		assertEquals("Europe/Paris", vevent.dtstart.timezone);
		assertEquals("testImport", vevent.summary);
		assertEquals(VEvent.Classification.Private, vevent.classification);
		assertEquals(Transparency.Opaque, vevent.transparency);
		assertEquals("Lorem ipsum", vevent.description);
		assertEquals("Toulouse", vevent.location);
		assertEquals(9, vevent.priority.intValue());
		assertEquals(VEvent.Status.Confirmed, vevent.status);
		assertEquals("Europe/Paris", vevent.timezone());

		assertEquals(2, vevent.attendees.size());
		boolean johnFound = false;
		boolean janeFound = false;
		for (VEvent.Attendee attendee : vevent.attendees) {
			if ("john.bang@bm.lan".equals(attendee.mailto)) {
				johnFound = true;
				assertEquals(VEvent.CUType.Individual, attendee.cutype);
				assertNull(attendee.member);
				assertEquals(VEvent.Role.Chair, attendee.role);
				assertEquals(VEvent.ParticipationStatus.Accepted, attendee.partStatus);
				assertTrue(attendee.rsvp);
				assertNull(attendee.delTo);
				assertNull(attendee.delFrom);
				assertNull(attendee.sentBy);
				// FIXME expected
				// assertEquals("john.bang@bm.lan", attendee.commonName);
				// looks better ?
				assertEquals("John Bang", attendee.commonName);

				assertNull(attendee.dir);
				assertNull(attendee.lang);
			} else if ("jane.bang@bm.lan".equals(attendee.mailto)) {
				janeFound = true;
				assertEquals(VEvent.CUType.Individual, attendee.cutype);
				assertNull(attendee.member);
				assertEquals(VEvent.Role.RequiredParticipant, attendee.role);
				assertEquals(VEvent.ParticipationStatus.NeedsAction, attendee.partStatus);
				assertFalse(attendee.rsvp);
				assertNull(attendee.delTo);
				assertNull(attendee.delFrom);
				assertNull(attendee.sentBy);
				// FIXME expected
				// assertEquals("jane.bang@bm.lan", attendee.commonName);
				// looks better ?
				assertEquals("Jane Bang", attendee.commonName);
				assertNull(attendee.dir);
				assertNull(attendee.lang);
			}
		}
		assertTrue(johnFound);
		assertTrue(janeFound);

		assertEquals(3, vevent.exdate.size());
		ZonedDateTime expectedExDate1 = ZonedDateTime.of(1983, 2, 13, 10, 0, 0, 0, utcTz);
		ZonedDateTime expectedExDate2 = ZonedDateTime.of(2012, 3, 31, 8, 30, 0, 0, utcTz);
		ZonedDateTime expectedExDate3 = ZonedDateTime.of(2014, 7, 14, 1, 2, 3, 0, utcTz);

		boolean date1Found = false;
		boolean date2Found = false;
		boolean date3Found = false;
		for (net.bluemind.core.api.date.BmDateTime d : vevent.exdate) {
			ZonedDateTime exdate = new BmDateTimeWrapper(d).toDateTime();
			if (exdate.isEqual(expectedExDate1)) {
				date1Found = true;
			} else if (exdate.isEqual(expectedExDate2)) {
				date2Found = true;
			} else if (exdate.isEqual(expectedExDate3)) {
				date3Found = true;
			}
		}
		assertTrue(date1Found);
		assertTrue(date2Found);
		assertTrue(date3Found);

		assertEquals(2, vevent.rdate.size());
		ZonedDateTime expectedRDate1 = ZonedDateTime.of(1983, 2, 13, 13, 0, 0, 0, hoChiMinhTz);
		ZonedDateTime expectedRDate2 = ZonedDateTime.of(1983, 2, 22, 13, 0, 0, 0, hoChiMinhTz);
		boolean rDate1Found = false;
		boolean rDate2Found = false;
		for (net.bluemind.core.api.date.BmDateTime d : vevent.rdate) {
			ZonedDateTime rdate = new BmDateTimeWrapper(d).toDateTime();
			if (rdate.isEqual(expectedRDate1)) {
				rDate1Found = true;
			} else if (rdate.isEqual(expectedRDate2)) {
				rDate2Found = true;
			}
		}
		assertTrue(rDate1Found);
		assertTrue(rDate2Found);

		assertNotNull(vevent.rrule);
		ZonedDateTime until = ZonedDateTime.of(2022, 12, 25, 13, 30, 0, 0, defaultTz);
		VEvent.RRule rrule = vevent.rrule;
		assertEquals(VEvent.RRule.Frequency.WEEKLY, rrule.frequency);
		assertNull(rrule.count);
		assertEquals(2, rrule.interval.intValue());
		assertTrue(until.isEqual(new BmDateTimeWrapper(rrule.until).toDateTime()));

		assertNotNull(rrule.bySecond);
		assertEquals(2, rrule.bySecond.size());
		assertTrue(rrule.bySecond.contains(10));
		assertTrue(rrule.bySecond.contains(20));

		assertNotNull(rrule.byMinute);
		assertEquals(3, rrule.byMinute.size());
		assertTrue(rrule.byMinute.contains(1));
		assertTrue(rrule.byMinute.contains(2));
		assertTrue(rrule.byMinute.contains(3));

		assertNotNull(rrule.byHour);
		assertEquals(2, rrule.byHour.size());
		assertTrue(rrule.byHour.contains(2));
		assertTrue(rrule.byHour.contains(22));

		assertNotNull(rrule.byDay);
		assertEquals(4, rrule.byDay.size());
		assertTrue(rrule.byDay.contains(VEvent.RRule.WeekDay.MO));
		assertTrue(rrule.byDay.contains(VEvent.RRule.WeekDay.TU));
		assertTrue(rrule.byDay.contains(VEvent.RRule.WeekDay.TH));
		assertTrue(rrule.byDay.contains(VEvent.RRule.WeekDay.FR));

		assertNotNull(rrule.byMonthDay);
		assertEquals(2, rrule.byMonthDay.size());
		assertTrue(rrule.byMonthDay.contains(2));
		assertTrue(rrule.byMonthDay.contains(3));

		assertNotNull(rrule.byYearDay);
		assertEquals(4, rrule.byYearDay.size());
		assertTrue(rrule.byYearDay.contains(8));
		assertTrue(rrule.byYearDay.contains(13));
		assertTrue(rrule.byYearDay.contains(42));
		assertTrue(rrule.byYearDay.contains(200));

		assertNotNull(rrule.byWeekNo);
		assertEquals(3, rrule.byWeekNo.size());
		assertTrue(rrule.byWeekNo.contains(8));
		assertTrue(rrule.byWeekNo.contains(13));
		assertTrue(rrule.byWeekNo.contains(42));

		assertNotNull(rrule.byMonth);
		assertEquals(1, rrule.byMonth.size());
		assertTrue(rrule.byMonth.contains(8));

		assertNotNull(vevent.organizer);
		// FIXME was osef@mailto.lan
		assertEquals("Osef du CN", vevent.organizer.commonName);
		assertEquals("osef@mailto.lan", vevent.organizer.mailto);

		// TODO categories
	}

	@Test
	public void testImportZimbra() throws IOException, ServerFault {
		Stream ics = getIcsFromFile("zimbra.ics");

		TaskRef taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		ImportStats stats = waitImportEnd(taskRef);

		assertNotNull(stats);
		assertEquals(4, stats.importedCount());
		assertEquals(5, stats.expectedCount());

		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete("a6f213213232112313124214124124214212bc");
		VEvent event = item.value.main;
		assertEquals("[Compta] - Envoi factures Redmine", event.summary);
		assertEquals(VEvent.RRule.Frequency.WEEKLY, event.rrule.frequency);
		assertEquals(2, event.rrule.interval.intValue());
		assertEquals(ZonedDateTime.of(2013, 7, 1, 11, 15, 0, 0, tz), new BmDateTimeWrapper(event.dtstart).toDateTime());
		assertEquals(ZonedDateTime.of(2013, 7, 1, 12, 15, 0, 0, tz), new BmDateTimeWrapper(event.dtend).toDateTime());

		item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete("6d987654321321654987158ZAEZAEZAE");
		event = item.value.main;
		assertEquals("[RH] - Relancer John Lennon", event.summary);
		assertEquals(ZonedDateTime.of(2013, 10, 21, 14, 0, 0, 0, tz),
				new BmDateTimeWrapper(event.dtstart).toDateTime());
		assertEquals("Europe/Paris", event.dtstart.timezone);
		assertEquals(ZonedDateTime.of(2013, 10, 21, 15, 0, 0, 0, tz), new BmDateTimeWrapper(event.dtend).toDateTime());
	}

	@Test
	public void testImportUpdated() throws Exception {
		Stream ics = getIcsFromFile("zimbra.ics");

		TaskRef taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		ImportStats stats = waitImportEnd(taskRef);
		assertNotNull(stats);

		// zimbra.ics already tested just above. skip assertions stuff

		// Import updated ics
		ics = getIcsFromFile("zimbra-updated.ics");

		taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		stats = waitImportEnd(taskRef);

		assertNotNull(stats);
		assertEquals(1, stats.importedCount());
		assertEquals(5, stats.expectedCount());

		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete("a6f213213232112313124214124124214212bc");

		VEvent event = item.value.main;
		assertEquals("[YAY] Updated!", event.summary);
		assertEquals(VEvent.RRule.Frequency.DAILY, event.rrule.frequency);
		assertEquals(1, event.rrule.interval.intValue());
		assertEquals(ZonedDateTime.of(2013, 7, 1, 12, 0, 0, 0, tz), new BmDateTimeWrapper(event.dtstart).toDateTime());
		assertEquals(ZonedDateTime.of(2013, 7, 1, 12, 15, 0, 0, tz), new BmDateTimeWrapper(event.dtend).toDateTime());
	}

	@Test
	public void testImportLotus() throws IOException, ServerFault {
		Stream ics = getIcsFromFile("lotus.ics");

		TaskRef taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		ImportStats stats = waitImportEnd(taskRef);
		assertNotNull(stats);
		// FIXME rdate support
		assertEquals(87, stats.importedCount());

		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete("3A128B86A21633F78525775D006A9DCE-Lotus_Notes_Generated");
		VEvent event = item.value.main;
		assertEquals("Robin", event.summary);
		assertEquals("335-7600", event.description);
		assertEquals("At this place", event.location);
		assertTrue(ZonedDateTime.of(2010, 9, 24, 19, 0, 0, 0, utcTz)
				.isEqual(new BmDateTimeWrapper(event.dtstart).toDateTime()));
	}

	@Test
	public void testImportOBM() throws IOException, ServerFault {
		Stream ics = getIcsFromFile("obm.ics");

		TaskRef taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		ImportStats stats = waitImportEnd(taskRef);

		assertNotNull(stats);
		// total 54 but 35 because of recurid
		// - 12 of
		// FIXME this event has 2 master, what we do with that ?
		// 040000008200E00074C5B7101A82E0080000000000FD15D8F2E4CC010000000000000000100000004D88FD3F9CD43D45A4E29A017CEC6706
		// - 3 of
		// 62b6fc35963912c9529ce2be5f4d16143141b3295fb1cd0d3bd09ec5fbebc500234df6cb6d592effe7cefa5b1c5a6e3e72d7e01e195637d37074d6c761c7a80a0b7c07d2a014bd27
		// - 2 of
		// 62b6fc35963912c9529ce2be5ce0e138b66b677c7429b7f0cbcc88eb4525d3fd234df6cb6d592effe7cefa5b1c5a6e3e72d7e01e5d3afba9357daa7ff43d7e5d7db1fcc53c1305c7
		//
		assertEquals(35, stats.importedCount());

		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete("OBM-2.2.19-f28114d4b018d8fcd269-b35fa558525c03c17edb103cbf2a8994ce3638");

		VEvent event = item.value.main;
		assertEquals("test", event.summary);
		assertEquals("C'est batte", event.description);
		assertEquals("Torronto", event.location);
		assertEquals(ZonedDateTime.of(2010, 3, 13, 11, 30, 0, 0, ZoneId.of("America/Toronto")),
				new BmDateTimeWrapper(event.dtstart).toDateTime());

		item = getCalendarService(userSecurityContext, userCalendarContainer).getComplete(
				"040000008200E00074C5B7101A82E0080000000000FD15D8F2E4CC010000000000000000100000004D88FD3F9CD43D45A4E29A017CEC6706");
		assertNotNull(item);
		assertEquals(12, item.value.occurrences.size());

		item = getCalendarService(userSecurityContext, userCalendarContainer).getComplete(
				"62b6fc35963912c9529ce2be5f4d16143141b3295fb1cd0d3bd09ec5fbebc500234df6cb6d592effe7cefa5b1c5a6e3e72d7e01e195637d37074d6c761c7a80a0b7c07d2a014bd27");

		assertNotNull(item);
		assertEquals(3, item.value.occurrences.size());

		item = getCalendarService(userSecurityContext, userCalendarContainer).getComplete(
				"62b6fc35963912c9529ce2be5ce0e138b66b677c7429b7f0cbcc88eb4525d3fd234df6cb6d592effe7cefa5b1c5a6e3e72d7e01e5d3afba9357daa7ff43d7e5d7db1fcc53c1305c7");
		assertNotNull(item);
		assertEquals(3, item.value.occurrences.size());
	}

	@Test
	public void testImportOutlook() throws IOException, ServerFault {
		Stream ics = getIcsFromFile("outlook.ics");

		TaskRef taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		ImportStats stats = waitImportEnd(taskRef);

		assertNotNull(stats);
		assertEquals(18, stats.importedCount());

		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer).getComplete(
				"040000008200E00074C5B7101A82E0080000000020E6BE25176BCD01000000000000000010000000AC9273385C95F54C9F9C4831A068C8D4");
		VEvent event = item.value.main;
		assertEquals("Electronic Reliability Estimation: How reliable are the results?", event.summary);
		assertEquals(ZonedDateTime.of(2012, 9, 25, 14, 50, 0, 0, ZoneId.of("Etc/UTC")).toInstant().toEpochMilli(),
				new BmDateTimeWrapper(event.dtstart).toDateTime().toInstant().toEpochMilli());
	}

	@Test
	public void testImportOpenXchange() throws IOException, ServerFault {
		Stream ics = getIcsFromFile("openxchange.ics");

		TaskRef taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		ImportStats stats = waitImportEnd(taskRef);

		assertNotNull(stats);
		assertEquals(2, stats.importedCount());
	}

	@Test
	public void testImportGoogle() throws IOException, ServerFault {
		Stream ics = getIcsFromFile("google.ics");

		TaskRef taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		ImportStats stats = waitImportEnd(taskRef);

		assertNotNull(stats);
		assertEquals(59, stats.importedCount());

		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete("Ical99de6e29c45ce103a75dcc539c5eb764");
		VEvent event = item.value.main;
		assertEquals("Presidential Fitness Awards", event.summary);
		assertEquals("<div>Grades 1-5</div>", event.description);
		assertEquals("LS multi-purpose room", event.location);
		assertEquals("UTC", event.dtstart.timezone);
		assertEquals(ZonedDateTime.of(2013, 4, 29, 16, 45, 0, 0, ZoneId.of("Etc/UTC")).toInstant().toEpochMilli(),
				new BmDateTimeWrapper(event.dtstart).toDateTime().toInstant().toEpochMilli());

		item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete("Icalff9bb2a513c75f5f09f31b83a3a4ac63");
		event = item.value.main;
		assertEquals(ZonedDateTime.of(2012, 6, 28, 15, 0, 0, 0, ZoneId.of("Australia/Sydney")),
				new BmDateTimeWrapper(event.dtstart).toDateTime());
	}

	@Test
	public void testImportContactOffice() throws IOException, ServerFault {
		Stream ics = getIcsFromFile("contactoffice.ics");

		TaskRef taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		ImportStats stats = waitImportEnd(taskRef);

		assertNotNull(stats);
		assertEquals(1, stats.importedCount());

		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete("0987654Z567890ZFAFZFAFZAFAZ");
		VEvent event = item.value.main;
		assertEquals("anniv john bang", event.summary);
		assertEquals(ZonedDateTime.of(2007, 3, 2, 8, 0, 0, 0, tz), new BmDateTimeWrapper(event.dtstart).toDateTime());
	}

	@Test
	public void testImportBlueMind() throws IOException, ServerFault {
		Stream ics = getIcsFromFile("bluemind.ics");

		TaskRef taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		ImportStats stats = waitImportEnd(taskRef);

		assertNotNull(stats);
		assertEquals(6, stats.importedCount());
	}

	@Test
	public void testImportingEventUsingDateShouldNotSaveTime() throws Exception {

		Stream ics = getIcsFromFile("bug9059.ics");

		TaskRef taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		ImportStats stats = waitImportEnd(taskRef);

		assertNotNull(stats);
		assertEquals(1, stats.importedCount());

		ItemValue<VEventSeries> event = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete(stats.uids.get(0));

		BmDateTime dtStart = event.value.main.dtstart;

		assertEquals(dtStart.precision, Precision.Date);
	}

	@Test
	public void testImportBug3084() throws IOException, ServerFault {
		Stream ics = getIcsFromFile("bug3084.ics");

		TaskRef taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		ImportStats stats = waitImportEnd(taskRef);

		assertNotNull(stats);
		assertEquals(2, stats.importedCount());

		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete("867468489AAD498498498484984");

		VEvent event = item.value.main;
		assertEquals("w00t", event.summary);

		item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete("867468489AAD498412130150808");
		event = item.value.main;
		assertEquals("-", event.summary);
	}

	@Test
	public void testImportBug3449() throws IOException, ServerFault {
		Stream ics = getIcsFromFile("bug3449.ics");

		TaskRef taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		ImportStats stats = waitImportEnd(taskRef);

		assertNotNull(stats);
		assertEquals(1, stats.importedCount());

		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete("BCD5213EA10-1043-13793940");
		VEvent event = item.value.main;
		assertEquals("junit bug 3449", event.summary);
	}

	@Test
	public void testExportImport() throws ServerFault {
		VEventSeries vevent = defaultVEvent();
		vevent.main.summary = "Yummy yummy";
		vevent.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(1998, 7, 12, 22, 30, 0, 0, tz));
		vevent.main.priority = 7;
		vevent.main.alarm = new ArrayList<ICalendarElement.VAlarm>(1);
		ICalendarElement.VAlarm alarm = ICalendarElement.VAlarm.create(-600);
		vevent.main.alarm.add(alarm);

		Set<net.bluemind.core.api.date.BmDateTime> exdate = new HashSet<>(3);
		ZonedDateTime exDate = ZonedDateTime.of(1983, 2, 13, 10, 0, 0, 0, tz);
		exdate.add(BmDateTimeHelper.time(exDate));
		ZonedDateTime exDate2 = ZonedDateTime.of(2012, 3, 31, 8, 30, 0, 0, tz);
		exdate.add(BmDateTimeHelper.time(exDate2));
		ZonedDateTime exDate3 = ZonedDateTime.of(2014, 7, 14, 1, 2, 3, 0, tz);
		exdate.add(BmDateTimeHelper.time(exDate3));
		vevent.main.exdate = exdate;

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.WEEKLY;
		rrule.interval = 2;
		// UNTIL is UTC date
		rrule.until = BmDateTimeHelper.time(ZonedDateTime.of(2022, 12, 25, 14, 30, 0, 0, ZoneId.of("UTC")));
		vevent.main.rrule = rrule;

		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, vevent, sendNotifications);

		String export = getVEventService(userSecurityContext, userCalendarContainer).exportIcs(uid);
		assertNotNull(export);

		TaskRef taskRef = getVEventService(userSecurityContext, userCalendarContainer)
				.importIcs(GenericStream.simpleValue(export, s -> s.getBytes()));
		ImportStats stats = waitImportEnd(taskRef);
		// event not modified
		assertEquals(0, stats.importedCount());

		export = export.replaceAll("LAST-MODIFIED:\\d{8}T\\d{6}", "LAST-MODIFIED:20221207T000000");

		taskRef = getVEventService(userSecurityContext, userCalendarContainer)
				.importIcs(GenericStream.simpleValue(export, s -> s.getBytes()));
		stats = waitImportEnd(taskRef);
		assertEquals(1, stats.importedCount());

		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete(stats.uids.get(0));
		VEvent yummy = item.value.main;
		assertNotNull(yummy);
		assertEquals(vevent.main.summary, yummy.summary);
		assertEquals(vevent.main.dtstart, yummy.dtstart);
		assertEquals(vevent.main.priority, yummy.priority);
		assertEquals(1, vevent.main.alarm.size());
		VAlarm valarm = vevent.main.alarm.get(0);
		assertEquals(alarm.action, valarm.action);
		assertEquals(alarm.trigger, valarm.trigger);
		assertNull(alarm.description);
		assertNull(alarm.summary);
		assertNull(alarm.repeat);
		assertEquals(vevent.main.organizer, yummy.organizer);
		assertEquals(vevent.main.attendees.size(), yummy.attendees.size());
		assertEquals(vevent.main.exdate.size(), yummy.exdate.size());

		boolean exDate1Found = false;
		boolean exDate2Found = false;
		boolean exDate3Found = false;

		for (net.bluemind.core.api.date.BmDateTime expected : yummy.exdate) {
			if (new BmDateTimeWrapper(expected).toDateTime().equals(exDate)) {
				exDate1Found = true;
			}
			if (new BmDateTimeWrapper(expected).toDateTime().equals(exDate2)) {
				exDate2Found = true;
			}
			if (new BmDateTimeWrapper(expected).toDateTime().equals(exDate3)) {
				exDate3Found = true;
			}
		}

		assertTrue(exDate1Found);
		assertTrue(exDate2Found);
		assertTrue(exDate3Found);

		assertNotNull(yummy.rrule);
		assertEquals(vevent.main.rrule.frequency, yummy.rrule.frequency);
		assertEquals(vevent.main.rrule.interval, yummy.rrule.interval);

		// . It MUST be specified in UTC time.
		assertEquals(vevent.main.rrule.until, yummy.rrule.until);
	}

	@Test
	public void testImportEmptyTitle() throws IOException, ServerFault {
		Stream ics = getIcsFromFile("icsEmptyTitle.ics");

		TaskRef taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		ImportStats stats = waitImportEnd(taskRef);

		assertNotNull(stats);
		assertEquals(1, stats.importedCount());
		assertEquals(1, stats.expectedCount());
	}

	@Test
	public void testImportAccent() throws IOException, ServerFault {
		Stream ics = getIcsFromFile("accent.ics");

		TaskRef taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		ImportStats stats = waitImportEnd(taskRef);

		assertNotNull(stats);
		assertEquals(1, stats.importedCount());
		assertEquals(1, stats.expectedCount());

		ItemValue<VEventSeries> res = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete("aa25edae-8fdc-4f7c-8c11-df20381a247a");
		assertEquals("éèçà", res.value.main.summary);
	}

	@Test
	public void testImportBug4923() throws ServerFault, IOException {
		Stream ics = getIcsFromFile("bug4923.ics");

		TaskRef taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		ImportStats stats = waitImportEnd(taskRef);

		assertNotNull(stats);
		assertEquals(2, stats.importedCount());

		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete("DDB479AD1F7108D6C1257B7282735372821-Lotus_Notes_Generated");

		VEvent event = item.value.main;
		assertEquals("Réunion Formation", event.summary);
		assertEquals(ZonedDateTime.of(2013, 2, 13, 14, 0, 0, 0, ZoneId.of("CET")),
				new BmDateTimeWrapper(event.dtstart).toDateTime());
	}

	@Test
	public void testImportBug5096() throws IOException, ServerFault {
		Stream ics = getIcsFromFile("bug5096.ics");

		TaskRef taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		ImportStats stats = waitImportEnd(taskRef);

		assertNotNull(stats);
		assertEquals(2, stats.importedCount());

		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete("8889ec48-07ea-4abb-ada9-9876546341321");

		VEvent event = item.value.main;
		assertEquals("RPM", event.summary);
		assertEquals(ZonedDateTime.of(2011, 4, 11, 12, 0, 0, 0, tz), new BmDateTimeWrapper(event.dtstart).toDateTime());
		assertEquals(ZonedDateTime.of(2011, 4, 11, 14, 0, 0, 0, tz), new BmDateTimeWrapper(event.dtend).toDateTime());
	}

	@Test
	public void testImportBug5019() throws IOException, ServerFault {
		Stream ics = getIcsFromFile("bug5019-1.ics");

		TaskRef taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		ImportStats stats = waitImportEnd(taskRef);

		assertNotNull(stats);
		assertEquals(1, stats.importedCount());

		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete("h2p562nvgt1ksuejet19d82d28@google.com");
		VEvent event = item.value.main;
		assertEquals("test", event.summary);
		assertEquals(ZonedDateTime.of(2013, 7, 17, 8, 0, 0, 0, ZoneId.of("Etc/UTC")).toInstant().toEpochMilli(),
				new BmDateTimeWrapper(event.dtstart).toDateTime().toInstant().toEpochMilli());

		// Update
		// FIXME fail because olditem.updated != null
		ics = getIcsFromFile("bug5019-2.ics");
		taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		stats = waitImportEnd(taskRef);

		assertNotNull(stats);
		assertEquals(1, stats.importedCount());

		item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete("h2p562nvgt1ksuejet19d82d28@google.com");

		assertNotNull(item);
		event = item.value.main;
		assertEquals("w00t", event.summary);
		assertEquals(ZonedDateTime.of(2013, 7, 17, 18, 0, 0, 0, ZoneId.of("Etc/UTC")).toInstant().toEpochMilli(),
				new BmDateTimeWrapper(event.dtstart).toDateTime().toInstant().toEpochMilli());
	}

	@Test
	public void testImportUpperCaseAttendeeEmail() throws IOException, ServerFault {
		Stream ics = getIcsFromFile("upperCaseAttendeeEmail.ics");

		TaskRef taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		ImportStats stats = waitImportEnd(taskRef);

		assertNotNull(stats);
		assertEquals(1, stats.importedCount());

		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete("BCD5213EA10-1043-13793940-213321321");
		VEvent event = item.value.main;
		assertEquals("junit uppercase attendee email", event.summary);
		assertEquals(1, event.attendees.size());
		assertEquals("john@dom.lan", event.attendees.get(0).mailto);
		// FIXME expected
		// assertEquals("john@dom.lan", event.attendees.get(0).commonName);
		// looks better ?
		assertEquals("John Bang", event.attendees.get(0).commonName);
	}

	@Test
	public void testImportMonthlyByDay() throws ServerFault, IOException {
		Stream ics = getIcsFromFile("monthlyByDay.ics");

		TaskRef taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		ImportStats stats = waitImportEnd(taskRef);

		assertNotNull(stats);
		assertEquals(2, stats.importedCount());

		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete("13c81f3c-9f5e-4a55-b1f9-74f175d26444");
		VEvent vevent = item.value.main;
		assertNotNull(vevent);

		assertNotNull(vevent.rrule);
		assertEquals(VEvent.RRule.Frequency.MONTHLY, vevent.rrule.frequency);
		assertEquals(1, vevent.rrule.byDay.size());
		assertEquals(new VEvent.RRule.WeekDay("TH", 2), vevent.rrule.byDay.get(0));
		assertEquals(1, vevent.rrule.interval.intValue());

		item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete("13c81f3c-9f5e-4a55-b1f9-74f175d26444-2");
		vevent = item.value.main;
		assertNotNull(vevent);

		assertNotNull(vevent.rrule);
		assertEquals(VEvent.RRule.Frequency.MONTHLY, vevent.rrule.frequency);
		assertEquals(1, vevent.rrule.byDay.size());
		assertEquals(new VEvent.RRule.WeekDay("MO", -1), vevent.rrule.byDay.get(0));
		assertEquals(1, vevent.rrule.interval.intValue());
	}

	@Test
	public void testImportDuplicateExtId() throws IOException, ServerFault {
		Stream ics = getIcsFromFile("duplicateExtId_1.ics");
		TaskRef taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		ImportStats stats = waitImportEnd(taskRef);

		assertNotNull(stats);
		assertEquals(1, stats.importedCount());

		ics = getIcsFromFile("duplicateExtId_2.ics");
		taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		stats = waitImportEnd(taskRef);

		assertNotNull(stats);
		assertEquals(1, stats.importedCount());
	}

	@Test
	public void testVAlarmImport() throws Exception {
		Stream ics = getIcsFromFile("testVAlarmImport.ics");

		LoggerFactory.getLogger(VEventServiceTests.class).info(String.format("ics: %s", ics));

		TaskRef taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		ImportStats stats = waitImportEnd(taskRef);

		assertNotNull(stats);
		assertEquals(1, stats.importedCount());

		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete("ced586fb-836c-462b-91d8-2c6bae2cd6ad");
		VEvent vevent = item.value.main;
		assertNotNull(vevent);

		assertEquals(1, vevent.alarm.size());
		ICalendarElement.VAlarm valarm = vevent.alarm.get(0);
		assertEquals(ICalendarElement.VAlarm.Action.Email, valarm.action);
		assertEquals(-600, valarm.trigger.intValue());
		assertEquals(0, valarm.repeat.intValue());
		assertEquals(30, valarm.duration.intValue());
		assertEquals("email alarm", valarm.description);
		assertEquals("AA", valarm.summary);
	}

	@Test
	public void testMultipleVAlarmImport() throws Exception {
		Stream ics = getIcsFromFile("testMultipleVAlarmImport.ics");

		LoggerFactory.getLogger(VEventServiceTests.class).info(String.format("ics: %s", ics));

		TaskRef taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		ImportStats stats = waitImportEnd(taskRef);

		assertNotNull(stats);
		assertEquals(1, stats.importedCount());

		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete("ced586fb-836c-462b-91d8-2c6bae2cd6ad");
		VEvent vevent = item.value.main;
		assertNotNull(vevent);

		assertEquals(3, vevent.alarm.size());

		boolean emailAlarm = false;
		boolean audioAlarm = false;
		boolean displayAlarm = false;
		for (int i = 0; i < vevent.alarm.size(); i++) {
			ICalendarElement.VAlarm valarm = vevent.alarm.get(i);
			if (ICalendarElement.VAlarm.Action.Email == valarm.action) {
				emailAlarm = true;
				assertEquals(ICalendarElement.VAlarm.Action.Email, valarm.action);
				assertEquals(-600, valarm.trigger.intValue());
				assertEquals(0, valarm.repeat.intValue());
				assertEquals(30, valarm.duration.intValue());
				assertEquals("email alarm", valarm.description);
				assertEquals("AA", valarm.summary);
			} else if (ICalendarElement.VAlarm.Action.Audio == valarm.action) {
				audioAlarm = true;
				assertEquals(200, valarm.trigger.intValue());
				assertEquals(5, valarm.repeat.intValue());
				assertEquals(5, valarm.duration.intValue());
				assertEquals("audio alarm", valarm.description);
				assertEquals("CC", valarm.summary);
			} else if (ICalendarElement.VAlarm.Action.Display == valarm.action) {
				displayAlarm = true;
				assertEquals(-60, valarm.trigger.intValue());
				assertEquals(0, valarm.repeat.intValue());
				assertEquals(-10, valarm.duration.intValue());
				assertEquals("display alarm", valarm.description);
				assertEquals("BB", valarm.summary);
			}
		}

		assertTrue(emailAlarm);
		assertTrue(audioAlarm);
		assertTrue(displayAlarm);
	}

	@Test
	public void testImportCustomizedTZ() throws IOException {
		Stream ics = getIcsFromFile("customizedTZ.ics");

		TaskRef taskRef = getVEventService(userSecurityContext, userCalendarContainer).importIcs(ics);
		ImportStats stats = waitImportEnd(taskRef);

		assertNotNull(stats);
		assertEquals(1, stats.importedCount());

		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete("this-is-uid");
		VEvent vevent = item.value.main;
		assertNotNull(vevent);
	}

	private ImportStats waitImportEnd(TaskRef taskRef) throws ServerFault {
		ITask task = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITask.class, taskRef.id);
		while (!task.status().state.ended) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}

		TaskStatus status = task.status();
		if (status.state == State.InError) {
			throw new ServerFault("import error");
		}

		return JsonUtils.read(status.result, ImportStats.class);
	}

	protected IVEvent getVEventService(SecurityContext context, Container container) throws ServerFault {
		return new VEventService(new BmTestContext(context), container);
	}

}
