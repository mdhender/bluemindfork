/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.service.eventdeferredaction.EventDeferredAction;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.deferredaction.api.DeferredAction;
import net.bluemind.deferredaction.api.IDeferredAction;
import net.bluemind.deferredaction.api.IDeferredActionContainerUids;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.icalendar.api.ICalendarElement.RRule;
import net.bluemind.icalendar.api.ICalendarElement.RRule.Frequency;
import net.bluemind.icalendar.api.ICalendarElement.RRule.WeekDay;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm.Action;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.BmDateTimeHelper;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class DeferredActionCalendarHookTests {

	@Before
	public void setup() throws Exception {
		System.setProperty("user.timezone", "UTC");
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		JdbcTestHelper.getInstance().beforeTest();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		Server imapServer = new Server();
		imapServer.tags = Collections.singletonList("mail/imap");
		imapServer.ip = PopulateHelper.FAKE_CYRUS_IP;

		ElasticsearchTestHelper.getInstance().beforeTest();
		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		PopulateHelper.initGlobalVirt(imapServer, esServer);

		String domainUid = "defbm.lan";
		PopulateHelper.createTestDomain(domainUid, imapServer, esServer);

		PopulateHelper.addUser("testuser", domainUid);
		PopulateHelper.addUser("participant1", domainUid);

	}

	@After
	public void teardown() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCreatingAnEventWithoutAlarmShouldNotCreateATrigger() throws Exception {
		ICalendar cal = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.defaultUserCalendar("testuser"));

		CompletableFuture<Void> wait = registerOnHook("uid1");
		cal.create("uid1", defaultVEvent(), false);
		wait.get(5, TimeUnit.SECONDS);

		List<ItemValue<DeferredAction>> byActionId = service("testuser").getByActionId(EventDeferredAction.ACTION_ID,
				new Date().getTime());
		assertEquals(0, byActionId.size());
	}

	@Test
	public void testCreatingASimpleEventWithAlarmShouldCreateATrigger() throws Exception {
		ICalendar cal = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.defaultUserCalendar("testuser"));

		VEventSeries defaultVEvent = defaultVEvent();
		addAlarm(defaultVEvent.main, 120);
		CompletableFuture<Void> wait = registerOnHook("uid1");
		cal.create("uid1", defaultVEvent, false);
		wait.get(5, TimeUnit.SECONDS);

		List<ItemValue<DeferredAction>> byActionId = service("testuser").getByActionId(EventDeferredAction.ACTION_ID,
				LocalDate.of(2099, 12, 31).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
		assertEquals(1, byActionId.size());

		checkDate(defaultVEvent.main.dtstart, byActionId, 2 * 60);
		DeferredAction action = byActionId.get(0).value;
		assertEquals(EventDeferredAction.ACTION_ID, action.actionId);
		assertEquals("calendar:Default:testuser#uid1", action.reference);
	}

	@Test
	public void testCreatingAnAnnulationShouldNotCreateATrigger() throws Exception {
		ICalendar cal = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.defaultUserCalendar("testuser"));

		VEventSeries defaultVEvent = defaultVEvent();
		defaultVEvent.main.attendees.get(0).partStatus = ParticipationStatus.Declined;
		addAlarm(defaultVEvent.main, 120);
		CompletableFuture<Void> wait = registerOnHook("uid1");
		cal.create("uid1", defaultVEvent, false);
		wait.get(5, TimeUnit.SECONDS);

		List<ItemValue<DeferredAction>> byActionId = service("testuser").getByActionId(EventDeferredAction.ACTION_ID,
				LocalDate.of(2099, 12, 31).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
		assertEquals(0, byActionId.size());
	}

	@Test
	public void testCreatingASimpleEventWithMultipleAlarmsShouldCreateAllTriggers() throws Exception {
		ICalendar cal = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.defaultUserCalendar("testuser"));

		VEventSeries defaultVEvent = defaultVEvent();
		addAlarm(defaultVEvent.main, 120);
		addAlarm(defaultVEvent.main, 240);
		addAlarm(defaultVEvent.main, 10);
		CompletableFuture<Void> wait = registerOnHook("uid2");
		cal.create("uid2", defaultVEvent, false);
		wait.get(5, TimeUnit.SECONDS);

		List<ItemValue<DeferredAction>> byActionId = service("testuser").getByActionId(EventDeferredAction.ACTION_ID,
				LocalDate.of(2099, 12, 31).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
		assertEquals(3, byActionId.size());

		checkDate(defaultVEvent.main.dtstart, byActionId, 120, 240, 10);
	}

	@Test
	public void testCreatingARecurrentEventstartingWithExdates() throws Exception {
		ICalendar cal = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.defaultUserCalendar("testuser"));

		ZoneId tz = ZoneId.of("Europe/Paris");
		ZonedDateTime start = ZonedDateTime.now(tz).plusDays(3);
		VEventSeries defaultVEvent = defaultVEvent(start);
		addAlarm(defaultVEvent.main, 120);
		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.DAILY;
		defaultVEvent.main.rrule = rrule;

		BmDateTime ex1 = BmDateTimeHelper.time(start);
		BmDateTime ex2 = BmDateTimeHelper.time(start.plusDays(1));
		BmDateTime ex3 = BmDateTimeHelper.time(start.plusDays(2));
		defaultVEvent.main.exdate = new HashSet<>(Arrays.asList(ex1, ex2, ex3));

		CompletableFuture<Void> wait = registerOnHook("uid2");
		cal.create("uid2", defaultVEvent, false);
		wait.get(5, TimeUnit.SECONDS);

		List<ItemValue<DeferredAction>> byActionId = service("testuser").getByActionId(EventDeferredAction.ACTION_ID,
				LocalDate.of(2099, 12, 31).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
		assertEquals(1, byActionId.size());

		BmDateTime expected = BmDateTimeHelper.time(start.plusDays(3));
		checkDate(expected, byActionId, 120);
	}

	@Test
	public void testCreatingARecurrentEventAllDay() throws Exception {
		ICalendar cal = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.defaultUserCalendar("testuser"));

		VEventSeries defaultVEvent = defaultVEvent();
		defaultVEvent.main.dtstart = new BmDateTime("2024-01-16", null, Precision.Date);
		addAlarm(defaultVEvent.main, 15 * 60 * 60);
		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.WEEKLY;
		rrule.byDay = Arrays.asList(WeekDay.SA);
		rrule.interval = 2;
		defaultVEvent.main.rrule = rrule;

		CompletableFuture<Void> wait = registerOnHook("uid2");
		cal.create("uid2", defaultVEvent, false);
		wait.get(5, TimeUnit.SECONDS);

		List<ItemValue<DeferredAction>> byActionId = service("testuser").getByActionId(EventDeferredAction.ACTION_ID,
				LocalDate.of(2099, 12, 31).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());

		System.err.println(byActionId.get(0).value.executionDate);

		assertEquals(1, byActionId.size());
		ZoneId tz = ZoneId.of("Europe/Paris");

		BmDateTime expected = BmDateTimeHelper.time(ZonedDateTime.of(2024, 1, 16, 0, 0, 0, 0, tz));
		checkDate(expected, byActionId, 15 * 60 * 60);
	}

	@Test
	public void testCreatingASimpleEventWithMultipleAlarmOfTypeEmailsShouldCreateAllTriggersInTheDomainContainer()
			throws Exception {
		ICalendar cal = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.defaultUserCalendar("testuser"));

		VEventSeries defaultVEvent = defaultVEvent();
		addAlarm(defaultVEvent.main, 120);
		addAlarm(defaultVEvent.main, 240);
		addAlarm(defaultVEvent.main, 10);
		defaultVEvent.main.alarm = defaultVEvent.main.alarm.stream().map(ala -> {
			ala.action = Action.Email;
			return ala;
		}).collect(Collectors.toList());
		CompletableFuture<Void> wait = registerOnHook("uid2");
		cal.create("uid2", defaultVEvent, false);
		wait.get(5, TimeUnit.SECONDS);

		List<ItemValue<DeferredAction>> byActionId = service("testuser").getByActionId(EventDeferredAction.ACTION_ID,
				LocalDate.of(2099, 12, 31).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
		assertEquals(0, byActionId.size());

		byActionId = domainService("defbm.lan").getByActionId(EventDeferredAction.ACTION_ID,
				LocalDate.of(2099, 12, 31).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
		assertEquals(3, byActionId.size());

		checkDate(defaultVEvent.main.dtstart, byActionId, 120, 240, 10);
	}

	@Test
	public void testCreatingASimpleEventWithMultipleAlarmsShouldNotCreateTriggersInThePast() throws Exception {
		ICalendar cal = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.defaultUserCalendar("testuser"));

		VEventSeries defaultVEvent = defaultVEvent();
		addAlarm(defaultVEvent.main, 120);
		addAlarm(defaultVEvent.main, 525600000);
		addAlarm(defaultVEvent.main, 10);
		CompletableFuture<Void> wait = registerOnHook("uid2");
		cal.create("uid2", defaultVEvent, false);
		wait.get(5, TimeUnit.SECONDS);

		List<ItemValue<DeferredAction>> byActionId = service("testuser").getByActionId(EventDeferredAction.ACTION_ID,
				LocalDate.of(2099, 12, 31).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
		assertEquals(2, byActionId.size());

		checkDate(defaultVEvent.main.dtstart, byActionId, 120, 10);
	}

	@Test
	public void testUpdatingAnEventShouldRecreateAllTriggers() throws Exception {
		ICalendar cal = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.defaultUserCalendar("testuser"));

		VEventSeries defaultVEvent = defaultVEvent();
		addAlarm(defaultVEvent.main, 120);
		addAlarm(defaultVEvent.main, 240);
		addAlarm(defaultVEvent.main, 10);
		CompletableFuture<Void> wait = registerOnHook("uid2");
		cal.create("uid2", defaultVEvent, false);
		wait.get(5, TimeUnit.SECONDS);

		List<ItemValue<DeferredAction>> byActionId = service("testuser").getByActionId(EventDeferredAction.ACTION_ID,
				LocalDate.of(2099, 12, 31).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
		assertEquals(3, byActionId.size());

		checkDate(defaultVEvent.main.dtstart, byActionId, 120, 240, 10);

		ItemValue<VEventSeries> storedEvent = cal.getComplete("uid2");
		storedEvent.value.main.alarm = new ArrayList<>();
		addAlarm(storedEvent.value.main, 420);
		addAlarm(storedEvent.value.main, 11);
		wait = registerOnHook("uid2");
		cal.update("uid2", storedEvent.value, false);
		wait.get(5, TimeUnit.SECONDS);

		byActionId = service("testuser").getByActionId(EventDeferredAction.ACTION_ID,
				LocalDate.of(2099, 12, 31).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
		assertEquals(2, byActionId.size());

		checkDate(defaultVEvent.main.dtstart, byActionId, 420, 11);
	}

	@Test
	public void testDeletingAnEventShouldDeleteAllAssociatedTriggers() throws Exception {
		ICalendar cal = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.defaultUserCalendar("testuser"));

		VEventSeries defaultVEvent = defaultVEvent();
		addAlarm(defaultVEvent.main, 120);
		addAlarm(defaultVEvent.main, 240);
		addAlarm(defaultVEvent.main, 10);
		CompletableFuture<Void> wait = registerOnHook("uid2");
		cal.create("uid2", defaultVEvent, false);
		wait.get(5, TimeUnit.SECONDS);

		VEventSeries defaultVEvent2 = defaultVEvent();
		addAlarm(defaultVEvent2.main, 120);
		addAlarm(defaultVEvent2.main, 240);
		addAlarm(defaultVEvent2.main, 10);
		wait = registerOnHook("uid3");
		cal.create("uid3", defaultVEvent, false);
		wait.get(5, TimeUnit.SECONDS);

		List<ItemValue<DeferredAction>> byActionId = service("testuser").getByActionId(EventDeferredAction.ACTION_ID,
				LocalDate.of(2099, 12, 31).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
		assertEquals(6, byActionId.size());

		wait = registerOnHook("uid2");
		cal.delete("uid2", false);
		wait.get(5, TimeUnit.SECONDS);

		byActionId = service("testuser").getByActionId(EventDeferredAction.ACTION_ID,
				LocalDate.of(2099, 12, 31).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
		assertEquals(3, byActionId.size());
	}

	@Test
	public void testCreatingARecurringEventShouldCalculateTheNextTrigger() throws Exception {
		ICalendar cal = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.defaultUserCalendar("testuser"));

		VEventSeries defaultVEvent = defaultVEvent();
		ZoneId tz = ZoneId.of("Europe/Paris");
		defaultVEvent.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2017, 2, 13, 0, 0, 1, 0, tz));
		defaultVEvent.main.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2017, 2, 13, 1, 0, 1, 0, tz));
		defaultVEvent.main.rrule = new RRule();
		defaultVEvent.main.rrule.byDay = Arrays.asList(WeekDay.MO, WeekDay.TU, WeekDay.WE, WeekDay.TH, WeekDay.FR,
				WeekDay.SA, WeekDay.SU);
		defaultVEvent.main.rrule.frequency = Frequency.WEEKLY;
		defaultVEvent.main.rrule.until = BmDateTimeHelper.time(ZonedDateTime.of(2025, 2, 13, 0, 0, 0, 1, tz));
		addAlarm(defaultVEvent.main, 60 * 60 * 2);
		CompletableFuture<Void> wait = registerOnHook("uid2");
		cal.create("uid2", defaultVEvent, false);
		wait.get(5, TimeUnit.SECONDS);

		List<ItemValue<DeferredAction>> byActionId = service("testuser").getByActionId(EventDeferredAction.ACTION_ID,
				LocalDate.of(2099, 12, 31).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
		assertEquals(1, byActionId.size());

		// expected date is today 22pm Europe/Paris
		ZonedDateTime expected = LocalDateTime.now().withHour(22).withMinute(0).withSecond(1).withNano(0).atZone(tz);

		ZonedDateTime triggerValueAsDate = ZonedDateTime
				.ofInstant(Instant.ofEpochMilli(byActionId.get(0).value.executionDate.getTime()), tz);
		assertEquals(expected, triggerValueAsDate);
	}

	@Test
	public void testCreatingAnExceptionShouldIncludeRecurId() throws Exception {
		ICalendar cal = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.defaultUserCalendar("testuser"));

		VEventSeries defaultVEvent = defaultVEvent();
		ZoneId tz = ZoneId.of("Asia/Ho_Chi_Minh");
		BmDateTime recurId = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 14, 6, 0, 0, 0, tz));
		defaultVEvent.occurrences = Arrays.asList(VEventOccurrence.fromEvent(defaultVEvent.main, recurId));
		defaultVEvent.main = null;

		addAlarm(defaultVEvent.occurrences.get(0), 120);
		CompletableFuture<Void> wait = registerOnHook("uid1");
		cal.create("uid1", defaultVEvent, false);
		wait.get(5, TimeUnit.SECONDS);

		List<ItemValue<DeferredAction>> byActionId = service("testuser").getByActionId(EventDeferredAction.ACTION_ID,
				LocalDate.of(2099, 12, 31).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
		assertEquals(1, byActionId.size());

		checkDate(defaultVEvent.occurrences.get(0).dtstart, byActionId, 120);
		DeferredAction action = byActionId.get(0).value;
		assertEquals(EventDeferredAction.ACTION_ID, action.actionId);
		assertEquals("calendar:Default:testuser#uid1", action.reference);
		assertEquals("2022-02-14T06:00:00.000+07:00", action.configuration.get("recurid"));

	}

	@Test
	public void testCreatingARecurringEventWithoutEndDateShouldnotStackoverflow() throws Exception {
		ICalendar cal = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.defaultUserCalendar("testuser"));

		VEventSeries defaultVEvent = defaultVEvent();
		ZoneId tz = ZoneId.of("Europe/Paris");
		defaultVEvent.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2017, 2, 13, 8, 0, 0, 0, tz));
		defaultVEvent.main.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2017, 2, 13, 9, 0, 0, 0, tz));
		defaultVEvent.main.rrule = new RRule();
		defaultVEvent.main.rrule.byDay = Arrays.asList(WeekDay.MO, WeekDay.TU, WeekDay.WE, WeekDay.TH, WeekDay.FR,
				WeekDay.SA, WeekDay.SU);
		defaultVEvent.main.rrule.frequency = Frequency.WEEKLY;
		defaultVEvent.main.rrule.until = null;
		addAlarm(defaultVEvent.main, 120);
		CompletableFuture<Void> wait = registerOnHook("uid2");
		cal.create("uid2", defaultVEvent, false);
		wait.get(15, TimeUnit.SECONDS);

		List<ItemValue<DeferredAction>> byActionId = service("testuser").getByActionId(EventDeferredAction.ACTION_ID,
				LocalDate.of(2099, 12, 31).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
		assertEquals(1, byActionId.size());
	}

	@Test
	public void testCreatingAnExceptionShouldIncludeRecuId() throws Exception {
		ICalendar cal = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.defaultUserCalendar("testuser"));

		VEventSeries defaultVEvent = defaultVEvent();
		ZoneId tz = ZoneId.of("Europe/Paris");
		defaultVEvent.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2017, 2, 13, 8, 0, 0, 0, tz));
		defaultVEvent.main.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2017, 2, 13, 9, 0, 0, 0, tz));
		defaultVEvent.main.rrule = new RRule();
		defaultVEvent.main.rrule.byDay = Arrays.asList(WeekDay.MO, WeekDay.TU, WeekDay.WE, WeekDay.TH, WeekDay.FR,
				WeekDay.SA, WeekDay.SU);
		defaultVEvent.main.rrule.frequency = Frequency.WEEKLY;
		defaultVEvent.main.rrule.until = null;
		addAlarm(defaultVEvent.main, 120);
		CompletableFuture<Void> wait = registerOnHook("uid2");
		cal.create("uid2", defaultVEvent, false);
		wait.get(15, TimeUnit.SECONDS);

		List<ItemValue<DeferredAction>> byActionId = service("testuser").getByActionId(EventDeferredAction.ACTION_ID,
				LocalDate.of(2099, 12, 31).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
		assertEquals(1, byActionId.size());
	}

	@Test
	public void testCreatingATriggerForEveryException() throws Exception {
		ICalendar cal = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.defaultUserCalendar("testuser"));

		ZoneId tz = ZoneId.of("Europe/Paris");
		ZonedDateTime start = ZonedDateTime.now(tz).plusDays(3);
		VEventSeries event = defaultVEvent(start);
		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.DAILY;
		rrule.interval = 3;
		event.main.rrule = rrule;
		String uid = "uid2";
		addAlarm(event.main, 120);

		VEventOccurrence event2 = recurringVEvent();
		event2.dtstart = BmDateTimeHelper.time(start.plusDays(1).plusHours(4));
		event2.recurid = BmDateTimeHelper.time(start.plusDays(1));
		event.occurrences = Arrays.asList(event2);
		addAlarm(event2, 240);

		CompletableFuture<Void> wait = registerOnHook("uid2");
		cal.create("uid2", event, false);
		wait.get(5, TimeUnit.SECONDS);

		List<ItemValue<DeferredAction>> byActionId = service("testuser").getByActionId(EventDeferredAction.ACTION_ID,
				LocalDate.of(2099, 12, 31).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
		assertEquals(2, byActionId.size());

		checkDate(event.main.dtstart, byActionId, 120);
		checkDate(event2.dtstart, byActionId, 240);
	}

	private <T extends VEvent> void addAlarm(T event, int trigger) {
		trigger *= -1;
		if (!event.hasAlarm()) {
			event.alarm = new ArrayList<>();
		}
		VAlarm alarm = new VAlarm();
		alarm.trigger = trigger;
		alarm.action = Action.Display;
		event.alarm.add(alarm);
	}

	private void checkDate(BmDateTime dtstart, List<ItemValue<DeferredAction>> storedTriggers, int... triggers) {
		for (int i = 0; i < triggers.length; i++) {
			int trigger = triggers[i];
			boolean ok = false;
			long timestamp = BmDateTimeWrapper.toTimestamp(dtstart.iso8601, dtstart.timezone);
			LocalDateTime date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.of("UTC")).toLocalDateTime();
			LocalDateTime expectedTriggerDate = date.minus(trigger, ChronoUnit.SECONDS);
			for (ItemValue<DeferredAction> triggerValue : storedTriggers) {
				LocalDateTime triggerValueAsDate = new java.sql.Timestamp(triggerValue.value.executionDate.getTime())
						.toLocalDateTime().atZone(ZoneId.of(dtstart.timezone)).toLocalDateTime();
				if (triggerValueAsDate.toEpochSecond(ZoneOffset.UTC) == expectedTriggerDate
						.toEpochSecond(ZoneOffset.UTC)) {
					ok = true;
				}
			}

			assertTrue(ok);
		}
	}

	private IDeferredAction service(String uid) {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDeferredAction.class,
				IDeferredActionContainerUids.uidForUser(uid));
	}

	private IDeferredAction domainService(String uid) {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDeferredAction.class,
				IDeferredActionContainerUids.uidForDomain(uid));
	}

	private CompletableFuture<Void> registerOnHook(String uid) throws InterruptedException {
		return WaitForCalendarHook.register(uid);
	}

	private VEventSeries defaultVEvent() {
		ZoneId tz = ZoneId.of("Europe/Paris");
		return defaultVEvent(ZonedDateTime.now(tz).plusDays(3));
	}

	private VEventSeries defaultVEvent(ZonedDateTime startAt) {
		VEventSeries series = new VEventSeries();
		VEvent event = new VEvent();
		event.dtstart = BmDateTimeHelper.time(startAt);
		event.summary = "event " + System.currentTimeMillis();
		event.location = "Toulouse";
		event.description = "Lorem ipsum";

		event.organizer = new VEvent.Organizer("testuser@bm.lan");

		List<VEvent.Attendee> attendees = new ArrayList<>(1);
		VEvent.Attendee me = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "osef", "bm://defbm.lan/users/testuser", null,
				null, "participant1@bm.lan");
		attendees.add(me);

		event.attendees = attendees;
		series.main = event;
		return series;
	}

	private VEventOccurrence recurringVEvent() {
		VEventOccurrence event = new VEventOccurrence();
		ZoneId tz = ZoneId.of("Asia/Ho_Chi_Minh");
		event.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 13, 8, 0, 0, 0, tz));
		event.recurid = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 13, 10, 0, 0, 0, tz));
		event.summary = "event " + System.currentTimeMillis();
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.transparency = VEvent.Transparency.Opaque;
		event.classification = VEvent.Classification.Private;
		event.status = VEvent.Status.Confirmed;
		event.priority = 3;

		event.organizer = new VEvent.Organizer("testuser@bm.lan");
		return event;
	}

}
