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
package net.bluemind.calendar.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.attachment.api.AttachedFile;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventCounter;
import net.bluemind.calendar.api.VEventCounter.CounterOriginator;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm.Action;

public class VEventSeriesStoreTests {
	private static Logger logger = LoggerFactory.getLogger(VEventSeriesStoreTests.class);
	private VEventSeriesStore vEventStore;
	private ItemStore itemStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerHome = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				securityContext);
		String containerId = "test_" + System.nanoTime();
		Container container = Container.create(containerId, "test", "test", "me", true);
		container = containerHome.create(container);

		assertNotNull(container);

		itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), container, securityContext);

		vEventStore = new VEventSeriesStore(JdbcTestHelper.getInstance().getDataSource(), container);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testSchemaIsWellRegistered() {
		assertNotNull(JdbcTestHelper.getInstance().getDbSchemaService().getSchemaDescriptor("vevent-schema"));
	}

	@Test
	public void testStoreRetrieveAndUpdate() throws SQLException {
		ItemValue<VEventSeries> event = defaultVEvent();

		event.value.main.dtstart = BmDateTimeWrapper
				.create(ZonedDateTime.of(2014, 6, 24, 10, 0, 0, 0, ZoneId.of("UTC")), Precision.DateTime);
		event.value.main.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 6, 24, 10, 0, 0, 0, ZoneId.of("UTC")),
				Precision.DateTime);

		itemStore.create(Item.create(event.uid, UUID.randomUUID().toString()));

		Item item = itemStore.get(event.uid);
		vEventStore.create(item, event.value);

		VEventSeries evt = vEventStore.get(item);
		assertNotNull(evt);
		assertTrue(event.value.main.dtstart.equals(evt.main.dtstart));
		assertTrue(event.value.main.dtend.equals(evt.main.dtend));
		assertEquals("Toulouse", evt.main.location);
		assertEquals("Lorem ipsum", evt.main.description);
		assertEquals(VEvent.Classification.Private, evt.main.classification);
		assertEquals(VEvent.Transparency.Opaque, evt.main.transparency);
		assertEquals(VEvent.Status.Confirmed, evt.main.status);
		assertEquals(42, evt.main.priority.intValue());
		assertEquals(event.value.main.organizer.uri, evt.main.organizer.uri);
		assertNull(evt.main.alarm);
		assertNull(evt.main.rrule);
		assertEquals(2, evt.main.attendees.size());
		assertEquals(2, evt.main.attachments.size());

		List<AttachedFile> attachments = evt.main.attachments;
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

		evt.main.summary = "updated summary";
		evt.main.location = "updated location";
		evt.main.priority = null;

		evt.main.alarm = new ArrayList<ICalendarElement.VAlarm>(1);
		evt.main.alarm.add(ICalendarElement.VAlarm.create(Action.Email, -600, "alarm desc", 15, 1, "w00t"));

		evt.main.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 6, 24, 13, 0, 0, 0, ZoneId.of("UTC")),
				Precision.DateTime);
		evt.main.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 6, 24, 14, 0, 0, 0, ZoneId.of("UTC")),
				Precision.DateTime);

		evt.main.organizer.uri = UUID.randomUUID().toString();

		Set<net.bluemind.core.api.date.BmDateTime> exdate = new HashSet<>(1);
		ZonedDateTime exDate = ZonedDateTime.of(2014, 2, 13, 0, 0, 0, 0, ZoneId.of("UTC"));
		exdate.add(BmDateTimeWrapper.create(exDate, Precision.DateTime));
		evt.main.exdate = exdate;

		List<VEvent.Attendee> attendees = evt.main.attendees;
		VEvent.Attendee attendee = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.RequiredParticipant,
				VEvent.ParticipationStatus.NeedsAction, true, "", "", "", "Kevin", "", "", "uid3", "kevin@bm.lan");
		attendees.add(attendee);

		vEventStore.update(item, evt);
		VEventSeries updated = vEventStore.get(item);
		assertNotNull(updated);

		assertTrue(evt.main.dtstart.equals(updated.main.dtstart));
		assertTrue(evt.main.dtend.equals(updated.main.dtend));
		assertEquals("updated summary", updated.main.summary);
		assertEquals("updated location", updated.main.location);
		assertEquals("Lorem ipsum", updated.main.description);
		assertEquals(VEvent.Classification.Private, updated.main.classification);
		assertEquals(VEvent.Transparency.Opaque, updated.main.transparency);
		assertEquals(VEvent.Status.Confirmed, updated.main.status);
		assertNull(updated.main.priority);

		assertEquals(1, updated.main.alarm.size());
		VAlarm alarm = updated.main.alarm.get(0);
		assertEquals(Action.Email, alarm.action);
		assertEquals(-600, alarm.trigger.intValue());
		assertEquals("alarm desc", alarm.description);
		assertEquals(15, alarm.duration.intValue());
		assertEquals(1, alarm.repeat.intValue());
		assertEquals("w00t", alarm.summary);

		assertEquals(evt.main.organizer.uri, updated.main.organizer.uri);
		assertEquals(1, updated.main.exdate.size());
		assertEquals(exDate.toInstant().toEpochMilli(),
				new BmDateTimeWrapper(updated.main.exdate.iterator().next()).toUTCTimestamp());
		assertNull(updated.main.rrule);
		assertEquals(3, updated.main.attendees.size());
	}

	@Test
	public void testStoreRetrieveAndUpdateIsolatedException() throws SQLException {
		String uid = UUID.randomUUID().toString();
		VEventOccurrence exception1 = VEventOccurrence.fromEvent(defaultVEvent().value.main,
				BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis() - 5000));
		VEventOccurrence exception2 = VEventOccurrence.fromEvent(defaultVEvent().value.main,
				BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()));
		VEventSeries series = new VEventSeries();
		series.occurrences = Arrays.asList(exception1, exception2);

		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));
		series.icsUid = uid;
		Item item = itemStore.get(uid);
		vEventStore.create(item, series);

		VEventSeries evt = vEventStore.get(item);
		assertNotNull(evt);
		assertNull(evt.main);
		assertEquals(2, evt.occurrences.size());
	}

	@Test
	public void testDelete() throws SQLException {
		ItemValue<VEventSeries> event = defaultVEvent();

		createAndGet(event);
		Item item = itemStore.get(event.uid);

		vEventStore.delete(item);
		assertNull(vEventStore.get(item));
	}

	@Test
	public void testMult() throws SQLException {
		ItemValue<VEventSeries> event = defaultVEvent();
		event.value.main.description = "t1";
		itemStore.create(Item.create(event.uid, UUID.randomUUID().toString()));

		Item item = itemStore.get(event.uid);
		vEventStore.create(item, event.value);

		ItemValue<VEventSeries> event2 = defaultVEvent();
		event2.value.main.description = "t2";
		itemStore.create(Item.create(event2.uid, UUID.randomUUID().toString()));

		Item item2 = itemStore.get(event2.uid);
		vEventStore.create(item2, event2.value);

		List<VEventSeries> values = vEventStore.getMultiple(Arrays.asList(item, item2));
		assertEquals(2, values.size());
		assertEquals("t1", values.get(0).main.description);
		assertEquals("t2", values.get(1).main.description);

		values = vEventStore.getMultiple(Arrays.asList(item2, item));
		assertEquals(2, values.size());
		assertEquals("t2", values.get(0).main.description);
		assertEquals("t1", values.get(1).main.description);
	}

	@Test
	public void testMultPerf() throws SQLException {
		for (int i = 0; i < 500; i++) {
			ItemValue<VEventSeries> event = defaultVEvent();
			event.value.main.description = "t1";
			itemStore.create(Item.create(event.uid, UUID.randomUUID().toString()));
			Item item = itemStore.get(event.uid);
			vEventStore.create(item, event.value);
		}
		List<Item> items = itemStore.all();
		// warm code
		for (int i = 0; i < 100; i++) {
			vEventStore.getMultiple(items);
		}

		System.err.println("begin test");

		long time = System.currentTimeMillis();
		for (int i = 0; i < 100; i++) {
			vEventStore.getMultiple(items);
		}
		long elaspedTime = System.currentTimeMillis() - time;
		System.err.println((elaspedTime / 100));

		// 50ms to load 500 items
		assertTrue(String.format("expected less than 100ms but was %s", elaspedTime / 100), (elaspedTime / 100) < 50);
	}

	@Test
	public void testExdate() throws SQLException {
		ItemValue<VEventSeries> event = defaultVEvent();

		Set<net.bluemind.core.api.date.BmDateTime> exdate = new HashSet<>();
		ZonedDateTime exDate = ZonedDateTime.of(1983, 2, 13, 22, 0, 0, 0, ZoneId.of("UTC"));
		exdate.add(BmDateTimeWrapper.create(exDate, Precision.DateTime));

		ZonedDateTime exDate2 = ZonedDateTime.of(2012, 3, 31, 2, 0, 0, 0, ZoneId.of("UTC"));
		exdate.add(BmDateTimeWrapper.create(exDate2, Precision.DateTime));

		ZonedDateTime exDate3 = ZonedDateTime.of(2014, 7, 14, 0, 30, 0, 0, ZoneId.of("UTC"));
		exdate.add(BmDateTimeWrapper.create(exDate3, Precision.DateTime));

		// add duplicate
		exdate.add(BmDateTimeWrapper.create(exDate3, Precision.DateTime));
		exdate.add(BmDateTimeWrapper.create(ZonedDateTime.of(2014, 7, 14, 0, 30, 0, 0, ZoneId.of("UTC")),
				Precision.DateTime));

		event.value.main.exdate = exdate;

		itemStore.create(Item.create(event.uid, UUID.randomUUID().toString()));

		Item item = itemStore.get(event.uid);
		vEventStore.create(item, event.value);

		VEventSeries evt = vEventStore.get(item);
		assertNotNull(evt);
		assertEquals(3, evt.main.exdate.size());
		boolean foundExdate1 = false;
		boolean foundExdate2 = false;
		boolean foundExdate3 = false;

		BmDateTime exDateBm = BmDateTimeWrapper.create(exDate, Precision.DateTime);
		BmDateTime exDateBm2 = BmDateTimeWrapper.create(exDate2, Precision.DateTime);
		BmDateTime exDateBm3 = BmDateTimeWrapper.create(exDate3, Precision.DateTime);

		for (BmDateTime date : evt.main.exdate) {
			if (date.equals(exDateBm)) {
				foundExdate1 = true;
				assertTrue(exDateBm.equals(date));
			} else if (date.equals(exDateBm2)) {
				foundExdate2 = true;
				assertTrue(exDateBm2.equals(date));
			} else if (date.equals(exDateBm3)) {
				foundExdate3 = true;
				assertTrue(exDateBm3.equals(date));
			}
		}

		assertTrue(foundExdate1);
		assertTrue(foundExdate2);
		assertTrue(foundExdate3);

		exdate = new HashSet<>(1);
		exdate.add(exDateBm);

		evt.main.exdate = exdate;
		vEventStore.update(item, evt);

		evt = vEventStore.get(item);
		assertNotNull(evt);
		assertEquals(1, evt.main.exdate.size());
		assertEquals(exDate.toInstant().toEpochMilli(),
				new BmDateTimeWrapper(evt.main.exdate.iterator().next()).toUTCTimestamp());

	}

	@Test
	public void testAttendees() throws SQLException {
		ItemValue<VEventSeries> event = defaultVEvent();

		itemStore.create(Item.create(event.uid, UUID.randomUUID().toString()));

		Item item = itemStore.get(event.uid);
		vEventStore.create(item, event.value);

		VEventSeries evt = vEventStore.get(item);
		assertNotNull(evt);
		assertEquals(2, evt.main.attendees.size());
		boolean john = false;
		boolean jane = false;
		for (VEvent.Attendee attendee : evt.main.attendees) {
			if ("uid1".equals(attendee.uri)) {
				john = true;
				assertEquals(VEvent.CUType.Individual, attendee.cutype);
				assertEquals(VEvent.Role.Chair, attendee.role);
				assertEquals(VEvent.ParticipationStatus.Accepted, attendee.partStatus);
				assertEquals("uid1", attendee.uri);
				assertEquals("I will be there to see jane", attendee.responseComment);
			} else if ("uid2".equals(attendee.uri)) {
				jane = true;
				assertEquals(VEvent.CUType.Individual, attendee.cutype);
				assertEquals(VEvent.Role.RequiredParticipant, attendee.role);
				assertEquals(VEvent.ParticipationStatus.NeedsAction, attendee.partStatus);
				assertNull(attendee.responseComment);
			}
		}

		assertTrue(john);
		assertTrue(jane);
	}

	@Test
	public void testSimpleRRule() throws SQLException {
		ItemValue<VEventSeries> event = defaultVEvent();
		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.DAILY;
		rrule.interval = 3;
		rrule.count = 10;

		event.value.main.rrule = rrule;

		itemStore.create(Item.create(event.uid, UUID.randomUUID().toString()));

		Item item = itemStore.get(event.uid);
		vEventStore.create(item, event.value);

		VEventSeries evt = vEventStore.get(item);
		assertNotNull(evt);
		assertNotNull(evt.main.rrule);
		assertEquals(rrule.frequency, evt.main.rrule.frequency);
		assertEquals(rrule.interval, evt.main.rrule.interval);
		assertEquals(rrule.count, evt.main.rrule.count);
		assertNull(rrule.until);
		assertNull(rrule.bySecond);
		assertNull(rrule.byMinute);
		assertNull(rrule.byHour);
		assertNull(rrule.byDay);
		assertNull(rrule.byMonthDay);
		assertNull(rrule.byYearDay);
		assertNull(rrule.byWeekNo);
		assertNull(rrule.byMonth);
	}

	@Test
	public void testRRule() throws SQLException {
		ItemValue<VEventSeries> event = defaultVEvent();
		event.value.main.summary = "Daily Scrum";
		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.WEEKLY;
		rrule.interval = 1;

		ZonedDateTime until = ZonedDateTime.of(2022, 2, 13, 12, 30, 30, 0, ZoneId.of("UTC"));
		rrule.until = BmDateTimeWrapper.create(until, Precision.DateTime);

		rrule.bySecond = Arrays.asList(10, 20);

		rrule.byMinute = Arrays.asList(1, 2, 3);

		rrule.byHour = Arrays.asList(2, 22);

		List<VEvent.RRule.WeekDay> weekDay = new ArrayList<VEvent.RRule.WeekDay>(5);
		weekDay.add(VEvent.RRule.WeekDay.MO);
		weekDay.add(VEvent.RRule.WeekDay.TU);
		weekDay.add(VEvent.RRule.WeekDay.TH);
		weekDay.add(VEvent.RRule.WeekDay.FR);
		weekDay.add(new VEvent.RRule.WeekDay("SA", 2));
		rrule.byDay = weekDay;

		rrule.byMonthDay = Arrays.asList(2, 3);

		rrule.byYearDay = Arrays.asList(8, 13, 42, 200);

		rrule.byWeekNo = Arrays.asList(8, 13, 42);

		rrule.byMonth = Arrays.asList(8);

		event.value.main.rrule = rrule;

		itemStore.create(Item.create(event.uid, UUID.randomUUID().toString()));

		Item item = itemStore.get(event.uid);
		vEventStore.create(item, event.value);

		VEventSeries evt = vEventStore.get(item);
		assertNotNull(evt);
		assertEquals("Daily Scrum", evt.main.summary);

		assertNotNull(evt.main.rrule);
		assertEquals(rrule.frequency, evt.main.rrule.frequency);
		assertEquals(rrule.interval, evt.main.rrule.interval);
		assertNull(rrule.count);
		assertEquals(until.toInstant().toEpochMilli(), new BmDateTimeWrapper(evt.main.rrule.until).toUTCTimestamp());

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
		assertEquals(5, rrule.byDay.size());
		assertTrue(rrule.byDay.contains(VEvent.RRule.WeekDay.MO));
		assertTrue(rrule.byDay.contains(VEvent.RRule.WeekDay.TU));
		assertTrue(rrule.byDay.contains(VEvent.RRule.WeekDay.TH));
		assertTrue(rrule.byDay.contains(VEvent.RRule.WeekDay.FR));
		assertFalse(rrule.byDay.contains(VEvent.RRule.WeekDay.SA));
		assertTrue(rrule.byDay.contains(new VEvent.RRule.WeekDay("SA", 2)));

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
	}

	@Test
	public void testRecurid() throws SQLException {
		ItemValue<VEventSeries> event = defaultVEvent();
		VEventOccurrence eventR = getRecurringEvent();
		event.value.occurrences = Arrays.asList(eventR);

		itemStore.create(Item.create(event.uid, UUID.randomUUID().toString()));

		Item item = itemStore.get(event.uid);
		vEventStore.create(item, event.value);

		VEventSeries evt = vEventStore.get(item);
		assertNotNull(evt);

		assertTrue(eventR.recurid.equals(evt.occurrences.get(0).recurid));
	}

	@Test
	public void testDeleteAll() throws SQLException {
		ItemValue<VEventSeries> event = defaultVEvent();
		createAndGet(event);
		Item item = itemStore.get(event.uid);
		assertNotNull(item);

		ItemValue<VEventSeries> event2 = defaultVEvent();
		createAndGet(event2);
		Item item2 = itemStore.get(event2.uid);
		assertNotNull(item2);

		vEventStore.deleteAll();
		assertNull(vEventStore.get(item));
		assertNull(vEventStore.get(item2));
	}

	@Test
	public void testNullOrganizer() throws SQLException {
		ItemValue<VEventSeries> event = defaultVEvent();
		event.value.main.organizer = null;

		itemStore.create(Item.create(event.uid, UUID.randomUUID().toString()));

		Item item = itemStore.get(event.uid);
		vEventStore.create(item, event.value);

		VEventSeries evt = vEventStore.get(item);
		assertNotNull(evt);
		assertNull(evt.main.organizer);
	}

	@Test
	public void testCustomProperties() throws SQLException {
		ItemValue<VEventSeries> event = defaultVEvent();

		ZoneId tz = ZoneId.of("Asia/Ho_Chi_Minh");
		event.value.main.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(1983, 2, 13, 2, 0, 0, 0, tz),
				Precision.DateTime);

		Map<String, String> properties = new HashMap<String, String>();
		properties.put("wat", "da funk");

		event.value.properties = properties;

		itemStore.create(Item.create(event.uid, UUID.randomUUID().toString()));

		Item item = itemStore.get(event.uid);
		vEventStore.create(item, event.value);

		VEventSeries evt = vEventStore.get(item);
		assertEquals(1, evt.properties.size());
		assertEquals("da funk", evt.properties.get("wat"));

		properties.put("another custom prop", "yeah yeah");
		evt.properties = properties;
		vEventStore.update(item, evt);

		evt = vEventStore.get(item);
		assertEquals(2, evt.properties.size());
		assertEquals("da funk", evt.properties.get("wat"));
		assertEquals("yeah yeah", evt.properties.get("another custom prop"));

		evt.properties = null;
		vEventStore.update(item, evt);

		evt = vEventStore.get(item);
		assertEquals(0, evt.properties.size());

		evt.properties = new HashMap<String, String>();
		vEventStore.update(item, evt);

		evt = vEventStore.get(item);
		assertEquals(0, evt.properties.size());
	}

	@Test
	public void testMultipleVAlarms() throws SQLException {
		ItemValue<VEventSeries> event = defaultVEvent();
		event.value.main.alarm = new ArrayList<ICalendarElement.VAlarm>(1);
		event.value.main.alarm.add(ICalendarElement.VAlarm.create(Action.Email, -600, "alarm desc", 10, 1, "w00t"));
		event.value.main.alarm.add(ICalendarElement.VAlarm.create(Action.Display, 1800, "alert alert", 20, 0, "lorem"));
		event.value.main.alarm.add(ICalendarElement.VAlarm.create(Action.Audio, -3600,
				"la première chose qu'elle voit, c'est le son", 1, 0, "ipsum"));

		itemStore.create(Item.create(event.uid, UUID.randomUUID().toString()));
		Item item = itemStore.get(event.uid);
		vEventStore.create(item, event.value);

		VEventSeries evt = vEventStore.get(item);
		assertNotNull(evt);
		assertEquals(3, evt.main.alarm.size());

		boolean foundEmail = false;
		boolean foundDisplay = false;
		boolean foundAudio = false;
		for (VAlarm alarm : evt.main.alarm) {

			if (alarm.action == Action.Email) {
				foundEmail = true;
				assertEquals(-600, alarm.trigger.intValue());
				assertEquals("alarm desc", alarm.description);
				assertEquals(10, alarm.duration.intValue());
				assertEquals(1, alarm.repeat.intValue());
				assertEquals("w00t", alarm.summary);
			} else if (alarm.action == Action.Display) {
				foundDisplay = true;
				assertEquals(1800, alarm.trigger.intValue());
				assertEquals("alert alert", alarm.description);
				assertEquals(20, alarm.duration.intValue());
				assertEquals(0, alarm.repeat.intValue());
				assertEquals("lorem", alarm.summary);
			} else if (alarm.action == Action.Audio) {
				foundAudio = true;
				assertEquals(-3600, alarm.trigger.intValue());
				assertEquals("la première chose qu'elle voit, c'est le son", alarm.description);
				assertEquals(1, alarm.duration.intValue());
				assertEquals(0, alarm.repeat.intValue());
				assertEquals("ipsum", alarm.summary);
			}
		}

		assertTrue(foundEmail);
		assertTrue(foundDisplay);
		assertTrue(foundAudio);
	}

	@Test
	public void testNullVAlarm() throws SQLException {
		ItemValue<VEventSeries> event = defaultVEvent();
		event.value.main.alarm = null;

		itemStore.create(Item.create(event.uid, UUID.randomUUID().toString()));
		Item item = itemStore.get(event.uid);
		vEventStore.create(item, event.value);

		VEventSeries evt = vEventStore.get(item);
		assertNotNull(evt);
		assertNull(evt.main.alarm);
	}

	@Test
	public void testZeroVAlarm() throws SQLException {
		ItemValue<VEventSeries> event = defaultVEvent();
		event.value.main.alarm = new ArrayList<VAlarm>();

		itemStore.create(Item.create(event.uid, UUID.randomUUID().toString()));
		Item item = itemStore.get(event.uid);
		vEventStore.create(item, event.value);

		VEventSeries evt = vEventStore.get(item);
		assertNotNull(evt);
		assertNull(evt.main.alarm);
	}

	@Test
	public void testRDate() throws SQLException {
		ItemValue<VEventSeries> event = defaultVEvent();

		Set<net.bluemind.core.api.date.BmDateTime> rdate = new HashSet<>();
		ZonedDateTime rDate = ZonedDateTime.of(1983, 2, 13, 22, 0, 0, 0, ZoneId.of("UTC"));
		rdate.add(BmDateTimeWrapper.create(rDate, Precision.DateTime));

		ZonedDateTime rDate2 = ZonedDateTime.of(2012, 3, 31, 2, 0, 0, 0, ZoneId.of("UTC"));
		rdate.add(BmDateTimeWrapper.create(rDate2, Precision.DateTime));

		ZonedDateTime rDate3 = ZonedDateTime.of(2014, 7, 14, 0, 30, 0, 0, ZoneId.of("UTC"));
		rdate.add(BmDateTimeWrapper.create(rDate3, Precision.DateTime));

		// add duplicate
		rdate.add(BmDateTimeWrapper.create(rDate3, Precision.DateTime));
		rdate.add(BmDateTimeWrapper.create(ZonedDateTime.of(2014, 7, 14, 0, 30, 0, 0, ZoneId.of("UTC")),
				Precision.DateTime));

		event.value.main.rdate = rdate;

		itemStore.create(Item.create(event.uid, UUID.randomUUID().toString()));

		Item item = itemStore.get(event.uid);
		vEventStore.create(item, event.value);

		VEventSeries evt = vEventStore.get(item);
		assertNotNull(evt);
		assertEquals(3, evt.main.rdate.size());
		boolean foundRDate1 = false;
		boolean foundRDate2 = false;
		boolean foundRDate3 = false;

		BmDateTime rDateBm = BmDateTimeWrapper.create(rDate, Precision.DateTime);
		BmDateTime rDateBm2 = BmDateTimeWrapper.create(rDate2, Precision.DateTime);
		BmDateTime rDateBm3 = BmDateTimeWrapper.create(rDate3, Precision.DateTime);

		for (BmDateTime date : evt.main.rdate) {
			if (date.equals(rDateBm)) {
				foundRDate1 = true;
				assertTrue(rDateBm.equals(date));
			} else if (date.equals(rDateBm2)) {
				foundRDate2 = true;
				assertTrue(rDateBm2.equals(date));
			} else if (date.equals(rDateBm3)) {
				foundRDate3 = true;
				assertTrue(rDateBm3.equals(date));
			}
		}

		assertTrue(foundRDate1);
		assertTrue(foundRDate2);
		assertTrue(foundRDate3);

		rdate = new HashSet<>(1);
		rdate.add(rDateBm);

		evt.main.rdate = rdate;
		vEventStore.update(item, evt);

		evt = vEventStore.get(item);
		assertNotNull(evt);
		assertEquals(1, evt.main.rdate.size());
		assertEquals(rDate.toInstant().toEpochMilli(),
				new BmDateTimeWrapper(evt.main.rdate.iterator().next()).toUTCTimestamp());
	}

	private VEventSeries createAndGet(ItemValue<VEventSeries> event) {
		try {
			itemStore.create(Item.create(event.uid, UUID.randomUUID().toString()));
			Item item = itemStore.get(event.uid);

			vEventStore.create(item, event.value);

			return vEventStore.get(item);

		} catch (SQLException e) {
			logger.error("error during vevent persistence call", e);
			fail(e.getMessage());
			return null;
		}

	}

	@Test
	public void testStoreAndRetrieveWithUid() throws SQLException {
		ItemValue<VEventSeries> event = defaultVEvent();
		VEventOccurrence rec1 = getRecurringEvent();
		rec1.recurid = BmDateTimeWrapper.create(ZonedDateTime.of(2011, 2, 13, 2, 0, 0, 0, ZoneId.of("UTC")),
				Precision.DateTime);
		VEventOccurrence rec2 = getRecurringEvent();
		rec2.recurid = BmDateTimeWrapper.create(ZonedDateTime.of(2012, 2, 13, 2, 0, 0, 0, ZoneId.of("UTC")),
				Precision.DateTime);
		event.value.occurrences = Arrays.asList(rec1, rec2);

		event.value.main.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2010, 2, 13, 2, 0, 0, 0, ZoneId.of("UTC")),
				Precision.DateTime);

		itemStore.create(Item.create(event.uid, UUID.randomUUID().toString()));

		Item item = itemStore.get(event.uid);
		vEventStore.create(item, event.value);

		VEventSeries evt = vEventStore.get(item);
		assertNotNull(evt);
		assertNotNull(evt.main.dtstart);
		assertEquals(event.value.icsUid, evt.icsUid);
		assertEquals(event.value.main.dtstart, evt.main.dtstart);
		assertNull(evt.main.dtend);
		assertEquals("Toulouse", evt.main.location);
		assertEquals("Lorem ipsum", evt.main.description);
		assertEquals(VEvent.Classification.Private, evt.main.classification);
		assertEquals(VEvent.Transparency.Opaque, evt.main.transparency);
		assertEquals(VEvent.Status.Confirmed, evt.main.status);
		assertEquals(42, evt.main.priority.intValue());
		assertEquals(event.value.main.organizer.uri, evt.main.organizer.uri);
		assertEquals("bm://users/org", event.value.main.organizer.dir);
		assertNull(evt.main.exdate);
		assertNull(evt.main.rrule);
		assertEquals(2, evt.main.attendees.size());
		assertNotNull(evt.main.categories);
		assertTrue(evt.main.categories.isEmpty());
		assertEquals(0, evt.properties.size());

		assertEquals(2, evt.occurrences.size());
		for (VEventOccurrence rec : evt.occurrences) {
			assertNotNull(rec.recurid);
		}

	}

	@Test
	public void testStoreAndRetrieveMultipleWithUid() throws SQLException {
		ItemValue<VEventSeries> event = defaultVEvent();
		event.value.main.summary = "I am event 1";
		ZonedDateTime date1 = ZonedDateTime.of(2010, 2, 13, 2, 0, 0, 0, ZoneId.of("UTC"));
		event.value.main.dtstart = BmDateTimeWrapper.create(date1, Precision.DateTime);
		VEventOccurrence rec1 = getRecurringEvent();
		rec1.summary = "exception event 1";
		ZonedDateTime date2 = ZonedDateTime.of(2011, 2, 13, 2, 0, 0, 0, ZoneId.of("UTC"));
		rec1.recurid = BmDateTimeWrapper.create(date2, Precision.DateTime);
		VEventOccurrence rec2 = getRecurringEvent();
		ZonedDateTime date3 = ZonedDateTime.of(2012, 2, 13, 2, 0, 0, 0, ZoneId.of("UTC"));
		rec2.recurid = BmDateTimeWrapper.create(date3, Precision.DateTime);
		rec2.summary = "exception event 1";
		event.value.occurrences = Arrays.asList(rec1, rec2);
		itemStore.create(Item.create(event.uid, null));
		Item item = itemStore.get(event.uid);
		vEventStore.create(item, event.value);

		List<VEventSeries> evts = vEventStore.getMultiple(Arrays.asList(item));
		assertEquals(1, evts.size());
		assertEquals(2, evts.get(0).occurrences.size());
		ItemValue<VEventSeries> event2 = defaultVEvent();
		event2.value.main.summary = "I am event 2";
		event2.value.main.dtstart = BmDateTimeWrapper.create(date1, Precision.DateTime);
		VEventOccurrence rec21 = getRecurringEvent();
		rec21.summary = "exception event 2";
		rec21.recurid = BmDateTimeWrapper.create(date2, Precision.DateTime);
		VEventOccurrence rec22 = getRecurringEvent();
		rec22.summary = "exception event 2";
		rec22.recurid = BmDateTimeWrapper.create(date3, Precision.DateTime);
		VEventOccurrence rec23 = getRecurringEvent();
		rec23.summary = "exception event 2";
		rec23.recurid = BmDateTimeWrapper.create(date3, Precision.DateTime);
		event2.value.occurrences = Arrays.asList(rec21, rec22, rec23);
		itemStore.create(Item.create(event2.uid, null));
		Item item2 = itemStore.get(event2.uid);
		vEventStore.create(item2, event2.value);

		evts = vEventStore.getMultiple(Arrays.asList(item));
		assertEquals(1, evts.size());
		assertEquals(2, evts.get(0).occurrences.size());

		evts = vEventStore.getMultiple(Arrays.asList(item, item2));

		assertEquals(2, evts.size());

		VEventSeries ret1 = evts.get(0);
		assertEquals("I am event 1", ret1.main.summary);
		assertEquals(2, ret1.occurrences.size());
		for (VEventOccurrence rec : ret1.occurrences) {
			assertNotNull(rec.recurid);
			assertEquals("exception event 1", rec.summary);
		}

		VEventSeries ret2 = evts.get(1);
		assertEquals("I am event 2", ret2.main.summary);
		assertEquals(3, ret2.occurrences.size());
		for (VEventOccurrence rec : ret2.occurrences) {
			assertNotNull(rec.recurid);
			assertEquals("exception event 2", rec.summary);
		}

	}

	@Test
	public void testFindByIcsUid() throws SQLException {
		ItemValue<VEventSeries> event = defaultVEvent();
		event.value.icsUid = "uid";
		event.value.main.alarm = new ArrayList<VAlarm>();

		itemStore.create(Item.create(event.uid, UUID.randomUUID().toString()));
		Item item = itemStore.get(event.uid);
		vEventStore.create(item, event.value);

		List<String> res = vEventStore.findByIcsUid("uid");
		assertEquals(1, res.size());
		assertEquals(event.uid, res.get(0));

		event = defaultVEvent();
		event.value.icsUid = "UID1";
		event.value.main.alarm = new ArrayList<VAlarm>();

		itemStore.create(Item.create(event.uid, UUID.randomUUID().toString()));
		item = itemStore.get(event.uid);
		vEventStore.create(item, event.value);

		res = vEventStore.findByIcsUid("UID1");
		assertEquals(1, res.size());
		assertEquals(event.uid, res.get(0));

		event = defaultVEvent();
		event.value.icsUid = "UID2";
		event.value.main.alarm = new ArrayList<VAlarm>();

		itemStore.create(Item.create(event.uid, UUID.randomUUID().toString()));
		item = itemStore.get(event.uid);
		vEventStore.create(item, event.value);

		res = vEventStore.findByIcsUid("uid2");
		assertEquals(1, res.size());
		assertEquals(event.uid, res.get(0));

		event = defaultVEvent();
		event.value.icsUid = "uid3";
		event.value.main.alarm = new ArrayList<VAlarm>();

		itemStore.create(Item.create(event.uid, UUID.randomUUID().toString()));
		item = itemStore.get(event.uid);
		vEventStore.create(item, event.value);

		res = vEventStore.findByIcsUid("UID3");
		assertEquals(1, res.size());
		assertEquals(event.uid, res.get(0));
	}

	@Test
	public void testSetSequence() throws SQLException {
		ItemValue<VEventSeries> event = defaultVEvent();
		event.value.main.sequence = 5;
		itemStore.create(Item.create(event.uid, UUID.randomUUID().toString()));
		Item item = itemStore.get(event.uid);
		vEventStore.create(item, event.value);
		VEventSeries evt = vEventStore.get(item);
		assertEquals(5, evt.main.sequence.intValue());

	}

	@Test
	public void testSetDraft() throws SQLException {
		ItemValue<VEventSeries> event = defaultVEvent();
		itemStore.create(Item.create(event.uid, UUID.randomUUID().toString()));
		Item item = itemStore.get(event.uid);
		event.value.main.draft = true;
		vEventStore.create(item, event.value);
		VEventSeries evt = vEventStore.get(item);
		assertTrue(evt.main.draft);
		event.value.main.draft = false;
		vEventStore.update(item, event.value);
		evt = vEventStore.get(item);
		assertFalse(evt.main.draft);
	}

	@Test
	public void testCounters() throws SQLException {
		ItemValue<VEventSeries> event = defaultVEvent();
		event.value.acceptCounters = true;
		event.value.counters = new ArrayList<>();
		VEvent counterEvent1 = defaultVEvent().value.main;
		BmDateTime dtstart = BmDateTimeWrapper.create(ZonedDateTime.now().plusDays(1), Precision.DateTime);
		counterEvent1.dtstart = dtstart;
		counterEvent1.dtend = BmDateTimeWrapper.create(ZonedDateTime.now().plusDays(1), Precision.DateTime);
		event.value.counters
				.add(counter("myName", "myName@bluemind.loc", VEventOccurrence.fromEvent(counterEvent1, null)));
		itemStore.create(Item.create(event.uid, UUID.randomUUID().toString()));
		Item item = itemStore.get(event.uid);

		vEventStore.create(item, event.value);
		VEventSeries evt = vEventStore.get(item);

		assertTrue(evt.acceptCounters);
		assertEquals(1, evt.counters.size());

		VEventCounter.CounterOriginator originator = evt.counters.get(0).originator;
		assertEquals("myName", originator.commonName);
		assertEquals("myName@bluemind.loc", originator.email);
		VEvent counter = evt.counters.get(0).counter;
		assertEquals(dtstart.iso8601, counter.dtstart.iso8601);

		VEvent counterEvent2 = defaultVEvent().value.main;
		BmDateTime dtstart2 = BmDateTimeWrapper.create(ZonedDateTime.now().plusDays(1), Precision.DateTime);
		counterEvent2.dtstart = dtstart2;
		counterEvent2.dtend = BmDateTimeWrapper.create(ZonedDateTime.now().plusDays(1), Precision.DateTime);
		event.value.counters
				.add(counter("myName2", "myName2@bluemind.loc", VEventOccurrence.fromEvent(counterEvent2, null)));

		vEventStore.update(item, event.value);
		evt = vEventStore.get(item);

		assertTrue(evt.acceptCounters);
		assertEquals(2, evt.counters.size());

		event.value.counters = event.value.counters.stream().filter(c -> c.originator.commonName.equals("myName2"))
				.collect(Collectors.toList());

		vEventStore.update(item, event.value);
		evt = vEventStore.get(item);

		assertTrue(evt.acceptCounters);
		assertEquals(1, evt.counters.size());

		originator = evt.counters.get(0).originator;
		assertEquals("myName2", originator.commonName);
		assertEquals("myName2@bluemind.loc", originator.email);

		event.value.counters = Collections.emptyList();

		vEventStore.update(item, event.value);
		evt = vEventStore.get(item);

		assertTrue(evt.acceptCounters);
		assertEquals(0, evt.counters.size());

		event.value.acceptCounters = false;
		vEventStore.update(item, event.value);
		evt = vEventStore.get(item);

		assertFalse(evt.acceptCounters);
	}

	private VEventCounter counter(String cn, String email, VEventOccurrence counterEvent) {
		VEventCounter counter = new VEventCounter();
		counter.originator = new CounterOriginator();
		counter.originator.commonName = cn;
		counter.originator.email = email;
		counter.counter = counterEvent;
		return counter;
	}

	private ItemValue<VEventSeries> defaultVEvent() {
		VEventSeries series = new VEventSeries();
		VEvent event = new VEvent();
		event.dtstart = BmDateTimeWrapper.create(ZonedDateTime.now(), Precision.DateTime);
		event.summary = "event " + System.currentTimeMillis();
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.transparency = VEvent.Transparency.Opaque;
		event.classification = VEvent.Classification.Private;
		event.status = VEvent.Status.Confirmed;
		event.priority = 42;

		event.attachments = new ArrayList<>();
		AttachedFile attachment1 = new AttachedFile();
		attachment1.publicUrl = "http://somewhere/1";
		attachment1.name = "test.gif";
		event.attachments.add(attachment1);
		AttachedFile attachment2 = new AttachedFile();
		attachment2.publicUrl = "http://somewhere/2";
		attachment2.name = "test.png";
		event.attachments.add(attachment2);

		event.organizer = new VEvent.Organizer();
		event.organizer.uri = UUID.randomUUID().toString();
		event.organizer.dir = "bm://users/org";
		List<VEvent.Attendee> attendees = new ArrayList<>(2);

		VEvent.Attendee john = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "John Bang", "", "", "uid1", "john.bang@bm.lan");
		attendees.add(john);
		john.responseComment = "I will be there to see jane";

		VEvent.Attendee jane = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.RequiredParticipant,
				VEvent.ParticipationStatus.NeedsAction, true, "", "", "", "Jane Bang", "", "", "uid2",
				"jane.bang@bm.lan");

		attendees.add(jane);

		event.attendees = attendees;
		series.main = event;
		ItemValue<VEventSeries> ret = ItemValue.create(UUID.randomUUID().toString(), series);
		series.icsUid = ret.uid;
		return ret;
	}

	private VEventOccurrence getRecurringEvent() {
		VEventOccurrence eventR = new VEventOccurrence();
		eventR.dtstart = BmDateTimeWrapper.create(ZonedDateTime.now(), Precision.DateTime);
		eventR.summary = "event " + System.currentTimeMillis();
		eventR.location = "Toulouse";
		eventR.description = "Lorem ipsum";
		eventR.transparency = VEvent.Transparency.Opaque;
		eventR.classification = VEvent.Classification.Private;
		eventR.status = VEvent.Status.Confirmed;
		eventR.priority = 42;

		eventR.organizer = new VEvent.Organizer();
		eventR.organizer.uri = UUID.randomUUID().toString();
		eventR.organizer.dir = "bm://users/org";
		List<VEvent.Attendee> attendees = new ArrayList<>(2);

		VEvent.Attendee john = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "John Bang", "", "", "uid1", "john.bang@bm.lan");
		attendees.add(john);
		john.responseComment = "I will be there to see jane";

		VEvent.Attendee jane = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.RequiredParticipant,
				VEvent.ParticipationStatus.NeedsAction, true, "", "", "", "Jane Bang", "", "", "uid2",
				"jane.bang@bm.lan");

		attendees.add(jane);

		eventR.attendees = attendees;

		eventR.recurid = BmDateTimeWrapper.create(ZonedDateTime.of(1983, 2, 13, 0, 0, 0, 0, ZoneId.of("UTC")),
				Precision.DateTime);
		return eventR;
	}

}
