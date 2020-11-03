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
package net.bluemind.calendar.helper.ical4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.CUType;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.icalendar.api.ICalendarElement.Role;
import net.bluemind.icalendar.parser.ICal4jHelper;
import net.bluemind.lib.ical4j.util.IcalConverter;
import net.bluemind.utils.FileUtils;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.util.Configurator;

public class VEventServiceHelperTest {
	@Test
	public void icsToVEventWithMultipleVCalendarsBM8890() throws IOException, ServerFault {
		InputStream in = VEventServiceHelperTest.class.getClassLoader()
				.getResourceAsStream("event_multiplevcalendar.ics");
		String ics = FileUtils.streamString(in, true);
		in.close();
		List<ItemValue<VEventSeries>> events = toEvents(ics);
		assertEquals(2, events.size());

	}

	@Test
	public void icsToVEventWithEtcGMT() throws IOException, ServerFault {
		InputStream in = VEventServiceHelperTest.class.getClassLoader().getResourceAsStream("invite.ics");
		String ics = IOUtils.toString(in);
		in.close();
		List<ItemValue<VEventSeries>> events = toEvents(ics);

		assertEquals(1, events.size());

		ItemValue<VEventSeries> event = events.get(0);

		// Etc/GMT+11:20160401T180000
		ZonedDateTime dtstart = ZonedDateTime.of(2016, 4, 1, 18, 0, 0, 0, ZoneId.of("Etc/GMT+11"));
		assertEquals(dtstart.toInstant().toEpochMilli(),
				new BmDateTimeWrapper(event.value.main.dtstart).toUTCTimestamp());
	}

	@Test
	public void icsToVEventWithUnkownTz() throws IOException, ServerFault {
		InputStream in = VEventServiceHelperTest.class.getClassLoader().getResourceAsStream("unknowntz.ics");
		String ics = IOUtils.toString(in);
		in.close();
		List<ItemValue<VEventSeries>> events = toEvents(ics);

		assertEquals(2, events.size());
		int checked = 0;
		for (ItemValue<VEventSeries> event : events) {
			if (event.value.icsUid.equals("30smvdkoolkjlklkkljkljkljkljvrdrkl7vlesogoi4s@google.com")) {
				// DTSTART:20161201T180000
				ZonedDateTime dtstart = ZonedDateTime.of(2016, 12, 1, 18, 0, 0, 0, ZoneId.of("Etc/GMT+5"));
				assertEquals(dtstart.toInstant().toEpochMilli(),
						new BmDateTimeWrapper(event.value.main.dtstart).toUTCTimestamp());
				checked++;
			}
			if (event.value.icsUid.equals("29smvdkoolkjlklkkljkljkljkljvrdrkl7vlesogoi4s@google.com")) {
				// DTSTART:19500312T020000
				ZonedDateTime dtstart = ZonedDateTime.of(2016, 4, 1, 18, 0, 0, 0, ZoneId.of("Etc/GMT+4"));
				assertEquals(dtstart.toInstant().toEpochMilli(),
						new BmDateTimeWrapper(event.value.main.dtstart).toUTCTimestamp());
				checked++;
			}
		}
		assertEquals(2, checked);
	}

	@Test
	public void icsToVEventWithGlobalX_WR_TimeZone() throws IOException, ServerFault {
		InputStream in = VEventServiceHelperTest.class.getClassLoader().getResourceAsStream("event2.ics");
		String ics = IOUtils.toString(in);
		in.close();
		List<ItemValue<VEventSeries>> events = toEvents(ics);

		assertEquals(2, events.size());

		ItemValue<VEventSeries> event = null;

		for (ItemValue<VEventSeries> e : events) {
			if ("8697@agendadulibre.org".equals(e.uid)) {
				event = e;
			}
		}

		assertNotNull(event);

		// X-WR-TIMEZONE:Europe/Paris
		// 20150610T193000
		ZonedDateTime dtstart = ZonedDateTime.of(2015, 6, 10, 19, 30, 0, 0, ZoneId.of("Europe/Paris"));
		assertEquals(dtstart.toInstant().toEpochMilli(),
				new BmDateTimeWrapper(event.value.main.dtstart).toUTCTimestamp());
	}

	@Test
	public void icsToVEvent() throws IOException, ServerFault {
		InputStream in = VEventServiceHelperTest.class.getClassLoader().getResourceAsStream("event.ics");
		String ics = IOUtils.toString(in);
		in.close();

		List<ItemValue<VEventSeries>> events = toEvents(ics);

		assertEquals(1, events.size());

		ItemValue<VEventSeries> event = events.get(0);

		ZonedDateTime dtstart = ZonedDateTime.of(1983, 2, 13, 2, 0, 0, 0, ZoneId.of("Europe/Paris"));

		assertEquals("Europe/Paris", event.value.main.dtstart.timezone);
		assertEquals(dtstart.toInstant().toEpochMilli(),
				new BmDateTimeWrapper(event.value.main.dtstart).toUTCTimestamp());
		assertNull(event.value.main.dtend);

		assertEquals("ced586fb-836c-462b-91d8-2c6bae2cd6ad", event.uid);
		assertEquals("testImport", event.value.main.summary);
		assertEquals("Lorem ipsum", event.value.main.description);
		assertEquals("Toulouse", event.value.main.location);
		assertEquals(42, event.value.main.priority.intValue());
		assertEquals(VEvent.Classification.Private, event.value.main.classification);
		assertEquals(VEvent.Transparency.Opaque, event.value.main.transparency);
		assertEquals(2, event.value.main.attendees.size());
		assertEquals(1, event.value.main.alarm.size());

		boolean johnFound = false;
		boolean janeFound = false;
		for (VEvent.Attendee attendee : event.value.main.attendees) {
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
				assertEquals("Jane Bang", attendee.commonName);
				assertNull(attendee.dir);
				assertNull(attendee.lang);
			}
		}
		assertTrue(johnFound);
		assertTrue(janeFound);

		assertEquals(3, event.value.main.exdate.size());

		ZonedDateTime expectedExDate1 = ZonedDateTime.of(1983, 2, 13, 10, 0, 0, 0, ZoneId.systemDefault());
		ZonedDateTime expectedExDate2 = ZonedDateTime.of(2012, 3, 31, 8, 30, 0, 0, ZoneId.systemDefault());
		ZonedDateTime expectedExDate3 = ZonedDateTime.of(2014, 7, 14, 1, 2, 3, 0, ZoneId.systemDefault());

		boolean date1Found = false;
		boolean date2Found = false;
		boolean date3Found = false;
		for (net.bluemind.core.api.date.BmDateTime d : event.value.main.exdate) {

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

		assertNotNull(event.value.main.rrule);
		ZonedDateTime until = ZonedDateTime.of(2022, 12, 25, 13, 30, 0, 0, ZoneId.of("UTC"));
		VEvent.RRule rrule = event.value.main.rrule;
		assertEquals(VEvent.RRule.Frequency.WEEKLY, rrule.frequency);
		assertNull(rrule.count);
		assertEquals(2, rrule.interval.intValue());
		assertEquals(until.toOffsetDateTime(), new BmDateTimeWrapper(rrule.until).toDateTime().toOffsetDateTime());

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

		assertNotNull(event.value.main.organizer);
		assertEquals("Osef du CN", event.value.main.organizer.commonName);
		assertEquals("osef@mailto.lan", event.value.main.organizer.mailto);

	}

	@Test
	public void testConvertingFromDateToBmDateShouldReturnBmDateWithPrecisionDateTimeWhenTimezoneIsPresent() {
		Date date = new net.fortuna.ical4j.model.DateTime();

		BmDateTime dt = IcalConverter.convertToDateTime(date, TimeZone.getTimeZone("EST").getID());

		assertEquals(Precision.DateTime, dt.precision);
	}

	@Test
	public void testConvertingFromDateToBmDateShouldReturnBmDateWithPrecisionDateTimeWhenInstanceIsDateTime() {
		Date date = new net.fortuna.ical4j.model.DateTime();

		BmDateTime dt = IcalConverter.convertToDateTime(date, (String) null);

		assertEquals(Precision.DateTime, dt.precision);
	}

	@Test
	public void testConvertingFromDateToBmDateShouldReturnBmDateWithPrecisionDateTimeWhenInstanceIsDate() {
		Date date = new Date();

		BmDateTime dt = IcalConverter.convertToDateTime(date, TimeZone.getDefault().getID());

		assertEquals(Precision.Date, dt.precision);
	}

	@Test
	public void notePropagation() {
		VEventSeries series = new VEventSeries();
		VEvent event = new VEvent();
		series.main = event;
		ZoneId tz = ZoneId.of("Europe/Paris");

		long now = System.currentTimeMillis();
		long start = now + (1000 * 60 * 60);
		ZonedDateTime temp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(start), tz);
		event.dtstart = BmDateTimeWrapper.create(temp, Precision.DateTime);
		temp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(start + (1000 * 60 * 60)), tz);
		event.dtend = BmDateTimeWrapper.create(temp, Precision.DateTime);
		event.summary = "notePropagation-" + System.currentTimeMillis();
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.priority = 1;
		event.organizer = new VEvent.Organizer(null, "david@bm.lan");
		event.status = ICalendarElement.Status.NeedsAction;
		Attendee attendee = VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u2", "", "", "", "", "", "u2", "u2@test.lan");
		attendee.responseComment = "this a a note";
		event.attendees = new ArrayList<Attendee>(1);
		event.attendees.add(attendee);

		// X-RESPONSE-COMMENT on REPLY only
		String ics = VEventServiceHelper.convertToIcs(Method.REPLY, ItemValue.create("test", series));
		assertTrue(ics.contains("X-RESPONSE-COMMENT=" + attendee.responseComment));

		ics = VEventServiceHelper.convertToIcs(Method.REQUEST, ItemValue.create("test", series));
		assertFalse(ics.contains("X-RESPONSE-COMMENT=" + attendee.responseComment));

		ics = VEventServiceHelper.convertToIcs(Method.CANCEL, ItemValue.create("test", series));
		assertFalse(ics.contains("X-RESPONSE-COMMENT=" + attendee.responseComment));
	}

	@Test
	public void notAbsoluteURI() throws IOException, ServerFault {
		InputStream in = VEventServiceHelperTest.class.getClassLoader().getResourceAsStream("not_absolute_uri.ics");
		String ics = IOUtils.toString(in);
		in.close();
		List<ItemValue<VEventSeries>> events = toEvents(ics);

		assertEquals(1, events.size());
		ItemValue<VEventSeries> event = events.get(0);

		assertEquals(3, event.value.main.attendees.size());
		assertEquals("david@bm.lan", event.value.main.attendees.get(0).mailto);
		assertEquals("john@bm.lan", event.value.main.attendees.get(1).mailto);
		assertEquals("john.john@bm.lan", event.value.main.attendees.get(2).mailto);

		assertEquals("john.john@bm.lan", event.value.main.organizer.mailto);
	}

	@Test
	public void dateTimeWithoutTimezone() {
		VEventSeries series = new VEventSeries();
		VEvent event = new VEvent();
		series.main = event;
		series.main.dtstart = new BmDateTime("1983-02-13T21:00:00+01:00", null, Precision.DateTime);
		series.main.dtend = new BmDateTime("1983-02-13T22:00:00+01:00", null, Precision.DateTime);

		String ics = VEventServiceHelper.convertToIcs(ItemValue.create("test", series));
		System.err.println(ics);
		assertTrue(ics.contains("DTSTART;TZID=Europe/London:19830213T200000"));
		assertTrue(ics.contains("DTEND;TZID=Europe/London:19830213T210000"));
	}

	@SuppressWarnings("deprecation")
	@Test
	public void exportShouldSetLastModifiedIfPossible() {
		VEventSeries series = new VEventSeries();
		VEvent event = new VEvent();
		series.main = event;
		series.main.dtstart = new BmDateTime("1983-02-13T21:00:00+01:00", null, Precision.DateTime);
		series.main.dtend = new BmDateTime("1983-02-13T22:00:00+01:00", null, Precision.DateTime);

		ItemValue<VEventSeries> create = ItemValue.create("test", series);
		create.updated = new java.util.Date(111, 06, 12, 12, 0);
		System.err.println(create.updated);
		String ics = VEventServiceHelper.convertToIcs(create);
		System.err.println(ics);
		assertTrue(ics.contains("LAST-MODIFIED:20110712T"));
	}

	@Test
	public void exdateWithoutTimezone() {
		VEventSeries series = new VEventSeries();
		VEvent event = new VEvent();
		series.main = event;
		series.main.dtstart = new BmDateTime("1983-02-13T21:00:00+01:00", null, Precision.DateTime);
		series.main.dtend = new BmDateTime("1983-02-13T22:00:00+01:00", null, Precision.DateTime);

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.WEEKLY;
		rrule.interval = 1;
		series.main.rrule = rrule;

		Set<BmDateTime> exdate = new HashSet<BmDateTime>(1);
		exdate.add(new BmDateTime("1983-02-20T21:00:00+01:00", null, Precision.DateTime));
		series.main.exdate = exdate;

		String ics = VEventServiceHelper.convertToIcs(ItemValue.create("test", series));
		assertTrue(ics.contains("EXDATE;TZID=Europe/London:19830220T200000"));
	}

	@Test
	public void rdateWithoutTimezone() {
		VEventSeries series = new VEventSeries();
		VEvent event = new VEvent();
		series.main = event;
		series.main.dtstart = new BmDateTime("1983-02-13T21:00:00+01:00", null, Precision.DateTime);
		series.main.dtend = new BmDateTime("1983-02-13T22:00:00+01:00", null, Precision.DateTime);

		Set<BmDateTime> rdate = new HashSet<BmDateTime>(1);
		rdate.add(new BmDateTime("1983-02-20T21:00:00+01:00", null, Precision.DateTime));
		series.main.rdate = rdate;

		String ics = VEventServiceHelper.convertToIcs(ItemValue.create("test", series));
		assertTrue(ics.contains("RDATE;TZID=Europe/London:19830220T200000"));

	}

	@Test
	public void doNotFailOnInvalidGEO() throws IOException {
		try (InputStream in = VEventServiceHelperTest.class.getClassLoader()
				.getResourceAsStream("event_invalid_geo.ics")) {
			String ics = IOUtils.toString(in);
			toEvents(ics);
		} catch (Exception e) {
			fail("should not fail " + e.getMessage());
		}
	}

	@Test
	public void icsImportBreakRegistry() throws IOException, ServerFault {

		System.err.println(Configurator.getProperty("net.fortuna.ical4j.timezone.update.enabled"));
		System.err.println(Configurator.getProperty("net.fortuna.ical4j.timezone.registry"));

		int rawOffset = ICal4jHelper.getTimeZoneRegistry().getTimeZone("Europe/Paris").getRawOffset();
		System.err.println(rawOffset);

		InputStream in = VEventServiceHelperTest.class.getClassLoader().getResourceAsStream("event_1601.ics");
		String ics = IOUtils.toString(in);
		in.close();

		toEvents(ics);

		assertEquals(rawOffset, ICal4jHelper.getTimeZoneRegistry().getTimeZone("Europe/Paris").getRawOffset());
	}

	private List<ItemValue<VEventSeries>> toEvents(String ics) {
		List<ItemValue<VEventSeries>> ret = new LinkedList<>();
		Consumer<ItemValue<VEventSeries>> consumer = series -> ret.add(series);
		VEventServiceHelper.convertToVEventList(ics, Optional.empty(), Collections.emptyList(), consumer);
		return ret;
	}
}
