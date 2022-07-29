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
package net.bluemind.todolist.persistence;

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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.Status;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm.Action;
import net.bluemind.todolist.api.VTodo;

public class VTodoStoreTests {
	private static Logger logger = LoggerFactory.getLogger(VTodoStoreTests.class);
	private VTodoStore vTodoStore;
	private ItemStore itemStore;
	private ZoneId defaultTz = ZoneId.systemDefault();

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

		vTodoStore = new VTodoStore(JdbcTestHelper.getInstance().getDataSource(), container);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testSchemaIsWellRegistered() {
		assertNotNull(JdbcTestHelper.getInstance().getDbSchemaService().getSchemaDescriptor("vtodo-schema"));
	}

	@Test
	public void testStoreAndRetrieveWithUid() throws SQLException {
		VTodo todo = defaultVTodo();

		todo.uid = UUID.randomUUID().toString();

		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));

		Item item = itemStore.get(uid);
		vTodoStore.create(item, todo);

		VTodo td = vTodoStore.get(item);

		assertNotNull(td);
		assertTrue(todo.dtstart.equals(td.dtstart));
		assertTrue(todo.due.equals(td.due));
		assertEquals(todo.summary, td.summary);
		assertEquals(todo.classification, td.classification);
		assertEquals(todo.location, td.location);
		assertEquals(todo.description, td.description);
		assertEquals(todo.status, td.status);
		assertEquals(todo.priority.intValue(), td.priority.intValue());
		assertNull(td.alarm);
		assertEquals(0, td.attendees.size());
		assertEquals(todo.organizer.uri, td.organizer.uri);
		assertNull(td.rrule);
		assertEquals(todo.percent, td.percent);
		assertNull(td.completed);

		item.id = new Random().nextInt();
		td = vTodoStore.get(item);
		assertNull(td);
	}

	@Test
	public void testStoreRetrieveAndUpdate() throws SQLException {
		VTodo todo = defaultVTodo();
		todo.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 6, 24, 10, 0, 0, 0, defaultTz),
				Precision.DateTime);
		todo.due = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 6, 24, 11, 0, 0, 0, defaultTz), Precision.DateTime);

		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));

		Item item = itemStore.get(uid);
		vTodoStore.create(item, todo);

		VTodo td = vTodoStore.get(item);
		assertNotNull(td);
		assertTrue(todo.dtstart.equals(td.dtstart));
		assertTrue(todo.due.equals(td.due));
		assertEquals(todo.summary, td.summary);
		assertEquals(todo.classification, td.classification);
		assertEquals(todo.location, td.location);
		assertEquals(todo.description, td.description);
		assertEquals(todo.status, td.status);
		assertEquals(todo.priority.intValue(), td.priority.intValue());
		assertNull(td.alarm);
		assertEquals(0, td.attendees.size());
		assertEquals(todo.organizer.uri, td.organizer.uri);
		assertNull(td.rrule);
		assertEquals(todo.percent, td.percent);
		assertNull(td.completed);

		td.summary = "updated summary";
		td.location = "updated location";
		td.priority = null;

		td.alarm = new ArrayList<ICalendarElement.VAlarm>(1);
		td.alarm.add(ICalendarElement.VAlarm.create(Action.Email, -60, "alarm desc", 15, 1, "w00t"));

		td.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 6, 24, 13, 0, 0, 0, defaultTz),
				Precision.DateTime);
		td.due = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 6, 24, 14, 0, 0, 0, defaultTz), Precision.DateTime);
		td.percent = 100;
		td.status = Status.Completed;
		td.completed = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 6, 22, 14, 0, 0, 0, defaultTz),
				Precision.DateTime);
		td.organizer.uri = UUID.randomUUID().toString();

		List<VTodo.Attendee> attendees = new ArrayList<>(1);
		VTodo.Attendee attendee = VTodo.Attendee.create(VTodo.CUType.Individual, "", VTodo.Role.RequiredParticipant,
				VTodo.ParticipationStatus.NeedsAction, true, "", "", "", "Kevin", "", "", "uid3", "kevin@bm.lan");
		attendees.add(attendee);
		td.attendees = attendees;
		vTodoStore.update(item, td);
		VTodo updated = vTodoStore.get(item);
		assertNotNull(updated);

		assertTrue(td.dtstart.equals(updated.dtstart));
		assertTrue(td.due.equals(updated.due));
		assertEquals(td.summary, updated.summary);
		assertEquals(td.location, updated.location);
		assertEquals(td.description, updated.description);
		assertEquals(td.classification, updated.classification);
		assertEquals(td.status, updated.status);
		assertNull(updated.priority);

		assertEquals(1, updated.alarm.size());
		VAlarm alarm = updated.alarm.get(0);
		assertEquals(Action.Email, alarm.action);
		assertEquals(-60, alarm.trigger.intValue());
		assertEquals("alarm desc", alarm.description);
		assertEquals(15, alarm.duration.intValue());
		assertEquals(1, alarm.repeat.intValue());
		assertEquals("w00t", alarm.summary);

		assertEquals(td.organizer.uri, updated.organizer.uri);
		assertEquals(td.attendees.size(), updated.attendees.size());
	}

	@Test
	public void testDelete() throws SQLException {
		VTodo todo = defaultVTodo();
		String uid = "test_" + System.nanoTime();

		createAndGet(uid, todo);
		Item item = itemStore.get(uid);

		vTodoStore.delete(item);
		assertNull(vTodoStore.get(item));
	}

	@Test
	public void testSimpleRRule() throws SQLException {
		VTodo todo = defaultVTodo();
		VTodo.RRule rrule = new VTodo.RRule();
		rrule.frequency = VTodo.RRule.Frequency.DAILY;
		rrule.interval = 3;
		rrule.count = 10;

		todo.rrule = rrule;

		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));

		Item item = itemStore.get(uid);
		vTodoStore.create(item, todo);

		VTodo td = vTodoStore.get(item);
		assertNotNull(td);
		assertNotNull(td.rrule);
		assertEquals(rrule.frequency, td.rrule.frequency);
		assertEquals(rrule.interval, td.rrule.interval);
		assertEquals(rrule.count, td.rrule.count);
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
		VTodo todo = defaultVTodo();
		todo.summary = "Daily Scrum";
		VTodo.RRule rrule = new VTodo.RRule();
		rrule.frequency = VTodo.RRule.Frequency.WEEKLY;
		rrule.interval = 1;
		ZonedDateTime until = ZonedDateTime.of(2022, 2, 13, 12, 30, 30, 0, defaultTz);
		rrule.until = BmDateTimeWrapper.create(until, Precision.DateTime);

		rrule.bySecond = Arrays.asList(10, 20);

		rrule.byMinute = Arrays.asList(1, 2, 3);

		rrule.byHour = Arrays.asList(2, 22);

		List<VTodo.RRule.WeekDay> weekDay = new ArrayList<VTodo.RRule.WeekDay>(5);
		weekDay.add(VTodo.RRule.WeekDay.MO);
		weekDay.add(VTodo.RRule.WeekDay.TU);
		weekDay.add(VTodo.RRule.WeekDay.TH);
		weekDay.add(VTodo.RRule.WeekDay.FR);
		weekDay.add(new VTodo.RRule.WeekDay("SA", 2));
		rrule.byDay = weekDay;

		rrule.byMonthDay = Arrays.asList(2, 3);

		rrule.byYearDay = Arrays.asList(8, 13, 42, 200);

		rrule.byWeekNo = Arrays.asList(8, 13, 42);

		rrule.byMonth = Arrays.asList(8);

		todo.rrule = rrule;

		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));

		Item item = itemStore.get(uid);
		vTodoStore.create(item, todo);

		VTodo td = vTodoStore.get(item);
		assertNotNull(td);
		assertEquals("Daily Scrum", td.summary);

		assertNotNull(td.rrule);
		assertEquals(rrule.frequency, td.rrule.frequency);
		assertEquals(rrule.interval, td.rrule.interval);
		assertNull(rrule.count);
		assertEquals(until.toInstant().toEpochMilli(), new BmDateTimeWrapper(td.rrule.until).toUTCTimestamp());

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
		assertTrue(rrule.byDay.contains(VTodo.RRule.WeekDay.MO));
		assertTrue(rrule.byDay.contains(VTodo.RRule.WeekDay.TU));
		assertTrue(rrule.byDay.contains(VTodo.RRule.WeekDay.TH));
		assertTrue(rrule.byDay.contains(VTodo.RRule.WeekDay.FR));
		assertFalse(rrule.byDay.contains(VTodo.RRule.WeekDay.SA));
		assertTrue(rrule.byDay.contains(new VTodo.RRule.WeekDay("SA", 2)));

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
	public void testExdate() throws SQLException {
		VTodo todo = defaultVTodo();

		Set<BmDateTime> exdate = new HashSet<>();

		ZonedDateTime exDate = ZonedDateTime.of(1983, 2, 13, 22, 0, 0, 0, defaultTz);
		exdate.add(BmDateTimeWrapper.create(exDate, Precision.DateTime));

		ZonedDateTime exDate2 = ZonedDateTime.of(2012, 3, 31, 2, 0, 0, 0, defaultTz);
		exdate.add(BmDateTimeWrapper.create(exDate2, Precision.DateTime));

		ZonedDateTime exDate3 = ZonedDateTime.of(2014, 7, 14, 0, 30, 0, 0, defaultTz);
		exdate.add(BmDateTimeWrapper.create(exDate3, Precision.DateTime));

		// add duplicate
		exdate.add(BmDateTimeWrapper.create(exDate3, Precision.DateTime));
		exdate.add(BmDateTimeWrapper.create(ZonedDateTime.of(2014, 7, 14, 0, 30, 0, 0, defaultTz), Precision.DateTime));

		todo.exdate = exdate;

		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));

		Item item = itemStore.get(uid);
		vTodoStore.create(item, todo);

		VTodo td = vTodoStore.get(item);
		assertNotNull(td);
		assertEquals(3, td.exdate.size());
		boolean foundExdate1 = false;
		boolean foundExdate2 = false;
		boolean foundExdate3 = false;

		for (net.bluemind.core.api.date.BmDateTime date : td.exdate) {
			if (new BmDateTimeWrapper(date).toDateTime().equals(exDate)) {
				foundExdate1 = true;
			} else if (new BmDateTimeWrapper(date).toDateTime().equals(exDate2)) {
				foundExdate2 = true;
			} else if (new BmDateTimeWrapper(date).toDateTime().equals(exDate3)) {
				foundExdate3 = true;
			}
		}

		assertTrue(foundExdate1);
		assertTrue(foundExdate2);
		assertTrue(foundExdate3);

		exdate = new HashSet<>(1);
		exdate.add(BmDateTimeWrapper.create(exDate, Precision.DateTime));

		td.exdate = exdate;
		vTodoStore.update(item, td);

		td = vTodoStore.get(item);
		assertNotNull(td);
		assertEquals(1, td.exdate.size());
		assertEquals(exDate, new BmDateTimeWrapper(td.exdate.iterator().next()).toDateTime());

	}

	@Test
	public void testAttendees() throws SQLException {
		VTodo todo = defaultVTodo();
		List<VTodo.Attendee> attendees = new ArrayList<>(2);

		VTodo.Attendee john = VTodo.Attendee.create(VTodo.CUType.Individual, "", VTodo.Role.Chair,
				VTodo.ParticipationStatus.Accepted, true, "", "", "", "John Bang", "", "", "uid1", "john.bang@bm.lan");
		attendees.add(john);

		VTodo.Attendee jane = VTodo.Attendee.create(VTodo.CUType.Individual, "", VTodo.Role.RequiredParticipant,
				VTodo.ParticipationStatus.NeedsAction, true, "", "", "", "Jane Bang", "", "", "uid2",
				"jane.bang@bm.lan");
		attendees.add(jane);

		todo.attendees = attendees;

		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));

		Item item = itemStore.get(uid);
		vTodoStore.create(item, todo);

		VTodo td = vTodoStore.get(item);
		assertNotNull(td);
		assertEquals(2, td.attendees.size());
		boolean haveJohn = false;
		boolean haveJane = false;
		for (VTodo.Attendee attendee : td.attendees) {
			if ("uid1".equals(attendee.uri)) {
				haveJohn = true;
				assertEquals(VTodo.CUType.Individual, attendee.cutype);
				assertEquals(VTodo.Role.Chair, attendee.role);
				assertEquals(VTodo.ParticipationStatus.Accepted, attendee.partStatus);
				assertEquals("uid1", attendee.uri);
			} else if ("uid2".equals(attendee.uri)) {
				haveJane = true;
				assertEquals(VTodo.CUType.Individual, attendee.cutype);
				assertEquals(VTodo.Role.RequiredParticipant, attendee.role);
				assertEquals(VTodo.ParticipationStatus.NeedsAction, attendee.partStatus);
			}
		}

		assertTrue(haveJohn);
		assertTrue(haveJane);
	}
	//
	// @Test
	// public void testRecurid() throws SQLException {
	// VTodo todo = defaultVTodo();
	// todo.recurid = BmDateTimeWrapper.create(new DateTime(1983, 2, 13, 0, 0,
	// 0), Precision.DateTime);
	//
	// String uid = "test_" + System.nanoTime();
	// itemStore.create(Item.create(uid, UUID.randomUUID().toString()));
	//
	// Item item = itemStore.get(uid);
	// vTodoStore.create(item, todo);
	//
	// VTodo td = vTodoStore.get(item);
	// assertNotNull(td);
	//
	// assertTrue(todo.recurid.equals(td.recurid));
	// }

	@Test
	public void testDeleteAll() throws SQLException {
		VTodo todo = defaultVTodo();
		String uid = "test_" + System.nanoTime();
		createAndGet(uid, todo);
		Item item = itemStore.get(uid);
		assertNotNull(item);

		VTodo todo2 = defaultVTodo();
		String uid2 = "test_" + System.nanoTime();
		createAndGet(uid2, todo2);
		Item item2 = itemStore.get(uid2);
		assertNotNull(item2);

		vTodoStore.deleteAll();
		assertNull(vTodoStore.get(item));
		assertNull(vTodoStore.get(item2));
	}

	@Test
	public void testNullOrganizer() throws SQLException {
		VTodo todo = defaultVTodo();
		todo.organizer = null;

		todo.uid = UUID.randomUUID().toString();

		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));

		Item item = itemStore.get(uid);
		vTodoStore.create(item, todo);

		VTodo td = vTodoStore.get(item);
		assertNotNull(td);
		assertEquals(todo.uid, td.uid);
		assertNull(td.organizer);
	}

	@Test
	public void testMultipleVAlarms() throws SQLException {
		VTodo todo = defaultVTodo();
		todo.alarm = new ArrayList<ICalendarElement.VAlarm>(1);
		todo.alarm.add(ICalendarElement.VAlarm.create(Action.Email, -600, "alarm desc", 15, 1, "w00t"));
		todo.alarm.add(ICalendarElement.VAlarm.create(Action.Display, -3600, "alert alert", 10, 0, "lorem"));
		todo.alarm.add(ICalendarElement.VAlarm.create(Action.Audio, 600, "la première chose qu'elle voit, c'est le son",
				12, 0, "ipsum"));

		String uid = "testMultipleVAlarms_" + System.currentTimeMillis();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));
		Item item = itemStore.get(uid);
		vTodoStore.create(item, todo);

		VTodo vtodo = vTodoStore.get(item);
		assertNotNull(vtodo);
		assertEquals(3, vtodo.alarm.size());

		boolean foundEmail = false;
		boolean foundDisplay = false;
		boolean foundAudio = false;
		for (VAlarm alarm : vtodo.alarm) {

			if (alarm.action == Action.Email) {
				foundEmail = true;
				assertEquals(-600, alarm.trigger.intValue());
				assertEquals("alarm desc", alarm.description);
				assertEquals(15, alarm.duration.intValue());
				assertEquals(1, alarm.repeat.intValue());
				assertEquals("w00t", alarm.summary);
			} else if (alarm.action == Action.Display) {
				foundDisplay = true;
				assertEquals(-3600, alarm.trigger.intValue());
				assertEquals("alert alert", alarm.description);
				assertEquals(10, alarm.duration.intValue());
				assertEquals(0, alarm.repeat.intValue());
				assertEquals("lorem", alarm.summary);
			} else if (alarm.action == Action.Audio) {
				foundAudio = true;
				assertEquals(600, alarm.trigger.intValue());
				assertEquals("la première chose qu'elle voit, c'est le son", alarm.description);
				assertEquals(12, alarm.duration.intValue());
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
		VTodo todo = defaultVTodo();
		todo.alarm = null;

		String uid = "testNullVAlarm_" + System.currentTimeMillis();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));
		Item item = itemStore.get(uid);
		vTodoStore.create(item, todo);

		VTodo vtodo = vTodoStore.get(item);
		assertNotNull(vtodo);
		assertNull(vtodo.alarm);
	}

	@Test
	public void testZeroVAlarm() throws SQLException {
		VTodo todo = defaultVTodo();
		todo.alarm = new ArrayList<VAlarm>();

		String uid = "testZeroVAlarm_" + System.currentTimeMillis();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));
		Item item = itemStore.get(uid);
		vTodoStore.create(item, todo);

		VTodo vtodo = vTodoStore.get(item);
		assertNotNull(vtodo);
		assertNull(vtodo.alarm);
	}

	@Test
	public void testRDate() throws SQLException {
		VTodo vtodo = defaultVTodo();

		Set<BmDateTime> rdate = new HashSet<>();
		ZonedDateTime rDate = ZonedDateTime.of(1983, 2, 13, 22, 0, 0, 0, defaultTz);
		rdate.add(BmDateTimeWrapper.create(rDate, Precision.DateTime));

		ZonedDateTime rDate2 = ZonedDateTime.of(2012, 3, 31, 2, 0, 0, 0, defaultTz);
		rdate.add(BmDateTimeWrapper.create(rDate2, Precision.DateTime));

		ZonedDateTime rDate3 = ZonedDateTime.of(2014, 7, 14, 0, 30, 0, 0, defaultTz);
		rdate.add(BmDateTimeWrapper.create(rDate3, Precision.DateTime));

		// add duplicate
		rdate.add(BmDateTimeWrapper.create(rDate3, Precision.DateTime));
		rdate.add(BmDateTimeWrapper.create(ZonedDateTime.of(2014, 7, 14, 0, 30, 0, 0, defaultTz), Precision.DateTime));

		vtodo.rdate = rdate;

		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));

		Item item = itemStore.get(uid);
		vTodoStore.create(item, vtodo);

		VTodo todo = vTodoStore.get(item);
		assertNotNull(todo);
		assertEquals(3, todo.rdate.size());
		boolean foundRDate1 = false;
		boolean foundRDate2 = false;
		boolean foundRDate3 = false;

		BmDateTime rDateBm = BmDateTimeWrapper.create(rDate, Precision.DateTime);
		BmDateTime rDateBm2 = BmDateTimeWrapper.create(rDate2, Precision.DateTime);
		BmDateTime rDateBm3 = BmDateTimeWrapper.create(rDate3, Precision.DateTime);

		for (BmDateTime date : todo.rdate) {
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

		todo.rdate = rdate;
		vTodoStore.update(item, todo);

		todo = vTodoStore.get(item);
		assertNotNull(todo);
		assertEquals(1, todo.rdate.size());
		assertEquals(rDate.toInstant().toEpochMilli(),
				new BmDateTimeWrapper(todo.rdate.iterator().next()).toUTCTimestamp());
	}

	@Test
	public void testGetReminder() throws SQLException {

		// FIXME do more tests on this method
		vTodoStore.getReminder(
				BmDateTimeWrapper.create(ZonedDateTime.of(1983, 2, 13, 22, 0, 0, 0, defaultTz), Precision.DateTime));

	}

	private VTodo createAndGet(String uid, VTodo todo) {
		try {
			itemStore.create(Item.create(uid, UUID.randomUUID().toString()));
			Item item = itemStore.get(uid);

			vTodoStore.create(item, todo);

			return vTodoStore.get(item);

		} catch (SQLException e) {
			logger.error("error during vtodo persistence call", e);
			fail(e.getMessage());
			return null;
		}

	}

	private VTodo defaultVTodo() {
		VTodo todo = new VTodo();
		todo.uid = UUID.randomUUID().toString();
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
		todo.dtstart = BmDateTimeWrapper.create(now, Precision.DateTime);
		todo.due = BmDateTimeWrapper.create(now.plusMonths(1), Precision.DateTime);
		todo.summary = "Todo " + System.currentTimeMillis();
		todo.location = "Toulouse";
		todo.description = "Lorem ipsum";
		todo.classification = VTodo.Classification.Private;
		todo.status = Status.NeedsAction;
		todo.percent = 25;
		todo.priority = 42;

		todo.organizer = new VTodo.Organizer();
		todo.organizer.uri = UUID.randomUUID().toString();

		return todo;
	}

}
