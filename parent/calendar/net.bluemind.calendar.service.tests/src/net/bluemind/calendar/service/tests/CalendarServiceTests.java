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

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventAttendeeQuery;
import net.bluemind.calendar.api.VEventChanges;
import net.bluemind.calendar.api.VEventChanges.ItemDelete;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventQuery;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.hook.CalendarHookAddress;
import net.bluemind.calendar.occurrence.OccurrenceHelper;
import net.bluemind.calendar.service.AbstractCalendarTests;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.ChangelogStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.api.TaskStatus.State;
import net.bluemind.core.tests.vertx.VertxEventChecker;
import net.bluemind.core.utils.UIDGenerator;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.CUType;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.icalendar.api.ICalendarElement.RRule;
import net.bluemind.icalendar.api.ICalendarElement.RRule.Frequency;
import net.bluemind.icalendar.api.ICalendarElement.Role;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm.Action;
import net.bluemind.tag.api.ITags;
import net.bluemind.tag.api.TagRef;
import net.bluemind.tests.defaultdata.BmDateTimeHelper;

public class CalendarServiceTests extends AbstractCalendarTests {

	@Test
	public void testCreate() throws ServerFault {
		VertxEventChecker<JsonObject> createdMessageChecker = new VertxEventChecker<>(
				CalendarHookAddress.EVENT_CREATED);

		VEventSeries event = defaultVEvent();
		String uid = "test_" + System.nanoTime();

		// test anonymous
		try {
			getCalendarService(SecurityContext.ANONYMOUS, userCalendarContainer).create(uid, event, sendNotifications);
			fail();
		} catch (ServerFault e) {
		}

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		Message<JsonObject> message = createdMessageChecker.shouldSuccess();
		assertNotNull(message);
	}

	@Test
	public void testCreateWithItem() throws Exception {
		VertxEventChecker<JsonObject> createdMessageChecker = new VertxEventChecker<>(
				CalendarHookAddress.EVENT_CREATED);

		ItemValue<VEventSeries> eventItem = defaultVEventItem(42);

		// test anonymous
		try {
			getCalendarService(SecurityContext.ANONYMOUS, userCalendarContainer).createWithItem(eventItem);
			fail();
		} catch (ServerFault e) {
		}

		ICalendar api = getCalendarService(userSecurityContext, userCalendarContainer);
		api.createWithItem(eventItem);

		ItemValue<VEventSeries> createdItem = api.getComplete(eventItem.uid);
		assertItemEquals(eventItem, createdItem);

		Message<JsonObject> message = createdMessageChecker.shouldSuccess();
		assertNotNull(message);
	}

	@Test
	public void testCreateWithBrokenRRuleShouldNotPass() throws ServerFault {
		VertxEventChecker<JsonObject> createdMessageChecker = new VertxEventChecker<>(
				CalendarHookAddress.EVENT_CREATED);

		VEventSeries event = defaultVEvent();
		RRule rule = new RRule();
		rule.frequency = Frequency.SECONDLY;
		net.bluemind.icalendar.api.ICalendarElement.RRule.WeekDay day = new net.bluemind.icalendar.api.ICalendarElement.RRule.WeekDay(
				"patate", 0);
		rule.byDay = Arrays.asList(day);
		event.main.rrule = rule;
		String uid = "test_" + System.nanoTime();

		// test anonymous
		try {
			getCalendarService(SecurityContext.ANONYMOUS, userCalendarContainer).create(uid, event, sendNotifications);
			fail();
		} catch (ServerFault e) {
		}

		try {
			getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);
			fail("Should not be able to create event with invalid weekday declaration");
		} catch (ServerFault e) {
			assertEquals("Unsupported weekday patate", e.getMessage());
		}

	}

	@Test
	public void testCreateIsolatedException() throws ServerFault {
		VertxEventChecker<JsonObject> createdMessageChecker = new VertxEventChecker<>(
				CalendarHookAddress.EVENT_CREATED);

		VEventSeries event = new VEventSeries();
		event.occurrences = Arrays.asList((VEventOccurrence.fromEvent(defaultVEvent().main,
				BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()))));
		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		Message<JsonObject> message = createdMessageChecker.shouldSuccess();
		assertNotNull(message);
	}

	@Test
	public void testUpdateIsolatedException() throws ServerFault {
		VEventSeries event = new VEventSeries();
		event.occurrences = Arrays.asList((VEventOccurrence.fromEvent(defaultVEvent().main,
				BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()))));
		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		getCalendarService(userSecurityContext, userCalendarContainer).update(uid, event, sendNotifications);
	}

	@Test
	public void testCreateWithException() throws ServerFault {
		VEventSeries event = new VEventSeries();

		VEventOccurrence exception = recurringVEvent();
		exception.categories.clear();
		exception.recurid = BmDateTimeHelper.time(ZonedDateTime.of(2014, 2, 13, 8, 0, 0, 0, tz));
		exception.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2014, 2, 13, 16, 0, 0, 0, tz));

		event.occurrences = Arrays.asList(exception);

		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);
		getCalendarService(attendee1SecurityContext, attendee1CalendarContainer).create(uid, event, sendNotifications);

		ItemValue<VEventSeries> complete = getCalendarService(userSecurityContext, userCalendarContainer)
				.multipleGet(Arrays.asList(uid)).get(0);

		assertEquals(1, complete.value.occurrences.size());
	}

	@Test
	public void testDuplicateRecurIdValidation() throws ServerFault {
		VEventSeries event = new VEventSeries();
		BmDateTime recurid = BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis());
		event.occurrences = new ArrayList<>(Arrays.asList((VEventOccurrence.fromEvent(defaultVEvent().main, recurid))));
		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);
		getCalendarService(userSecurityContext, userCalendarContainer).update(uid, event, sendNotifications);

		event.occurrences.add(event.occurrences.get(0));
		try {
			getCalendarService(userSecurityContext, userCalendarContainer).update(uid, event, sendNotifications);
			fail();
		} catch (ServerFault e) {
			assertEquals(e.getCode(), ErrorCode.EVENT_DUPLICATED_RECURID);
		}

		uid = "test_" + System.nanoTime();
		try {
			getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);
			fail();
		} catch (ServerFault e) {
			assertEquals(e.getCode(), ErrorCode.EVENT_DUPLICATED_RECURID);
		}
	}

	@Test
	public void testOrganizer() throws ServerFault {
		VEventSeries event = defaultVEvent();
		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		ItemValue<VEventSeries> vevent = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete(uid);
		assertNotNull(vevent);

		assertNotNull(vevent.value.main.organizer);
		assertEquals(testUser.value.login + "@bm.lan", vevent.value.main.organizer.mailto);
	}

	@Test
	public void testExternalOrganizer() throws ServerFault {
		VEventSeries event = defaultVEvent();
		String organizerMail = "ext" + System.currentTimeMillis() + "@extdomain.lan";
		String organizerCN = "External Organizer";
		event.main.organizer = new VEvent.Organizer(organizerMail);
		event.main.organizer.commonName = organizerCN;
		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		ItemValue<VEventSeries> vevent = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete(uid);
		assertNotNull(vevent);

		assertNotNull(vevent.value.main.organizer);
		assertEquals(organizerMail, vevent.value.main.organizer.mailto);
		// FIXME why was that ?
		// assertEquals(organizerMail, vevent.value.organizer.commonName);
		assertEquals(organizerCN, vevent.value.main.organizer.commonName);
	}

	@Test
	public void testExternalAttendee() throws ServerFault {
		VEventSeries event = defaultVEvent();

		String externalEmail = "external@attendee" + System.currentTimeMillis() + ".lan";
		String externalDisplayName = "External Attendee";

		Attendee external = Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "", "", "", externalDisplayName, "", "", null, externalEmail);

		event.main.attendees.add(external);

		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		// getComplete as testUser
		ItemValue<VEventSeries> vevent = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete(uid);
		assertNotNull(vevent);

		assertEquals(2, vevent.value.main.attendees.size());

		boolean externalAttendeeFound = false;

		for (Attendee att : vevent.value.main.attendees) {
			if (externalEmail.equals(att.mailto)) {
				// FIXME why was that ?
				// assertEquals(externalEmail, att.commonName);
				assertEquals(externalDisplayName, att.commonName);
				assertEquals(externalEmail, att.mailto);
				assertFalse(att.internal);
				externalAttendeeFound = true;
			}
			assertNotNull(att.commonName);
			assertNotNull(att.mailto);
		}

		assertTrue(externalAttendeeFound);

		// getComplete as attendee1
		vevent = getCalendarService(attendee1SecurityContext, attendee1CalendarContainer).getComplete(uid);
		assertNull(vevent);

		// getComplete as attendee2
		vevent = getCalendarService(attendee2SecurityContext, attendee2CalendarContainer).getComplete(uid);
		assertNull(vevent);
	}

	@Test
	public void testUpdate() throws ServerFault {

		VertxEventChecker<JsonObject> updatedMessageChecker = new VertxEventChecker<>(
				CalendarHookAddress.EVENT_UPDATED);

		VEventSeries event = defaultVEvent();
		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		// test anonymous
		try {
			getCalendarService(SecurityContext.ANONYMOUS, userCalendarContainer).update(uid, event, sendNotifications);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getCalendarService(userSecurityContext, userCalendarContainer).update(uid, event, sendNotifications);

		Message<JsonObject> message = updatedMessageChecker.shouldSuccess();
		assertNotNull(message);

		List<AccessControlEntry> ace = Arrays.asList(
				AccessControlEntry.create(userSecurityContext.getSubject(), Verb.All),
				AccessControlEntry.create(attendee1SecurityContext.getSubject(), Verb.All),
				AccessControlEntry.create(attendee2SecurityContext.getSubject(), Verb.Write));
		try {
			aclStoreData.store(userCalendarContainer, ace);
			aclStoreData.store(userTagContainer, ace);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			// Cannot modifiy private event with write verb
			getCalendarService(attendee2SecurityContext, userCalendarContainer).update(uid, event, sendNotifications);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
		// Can modifiy private event with manage verb

		getCalendarService(attendee1SecurityContext, userCalendarContainer).update(uid, event, sendNotifications);

		message = updatedMessageChecker.shouldSuccess();
		assertNotNull(message);
	}

	@Test
	public void testUpdateWithItem() throws Exception {

		VertxEventChecker<JsonObject> updatedMessageChecker = new VertxEventChecker<>(
				CalendarHookAddress.EVENT_UPDATED);

		VEventSeries event = defaultVEvent();
		String uid = "test_" + System.nanoTime();
		ICalendar userCalApi = getCalendarService(userSecurityContext, userCalendarContainer);
		userCalApi.create(uid, event, sendNotifications);
		ItemValue<VEventSeries> eventItem = userCalApi.getComplete(uid);

		try {
			ICalendar anonymousCalApi = getCalendarService(SecurityContext.ANONYMOUS, userCalendarContainer);
			anonymousCalApi.updateWithItem(eventItem);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		userCalApi.updateWithItem(eventItem);

		ItemValue<VEventSeries> updatedItem = userCalApi.getComplete(eventItem.uid);
		assertItemEquals(eventItem, updatedItem);
		Message<JsonObject> message = updatedMessageChecker.shouldSuccess();
		assertNotNull(message);

		List<AccessControlEntry> ace = Arrays.asList(
				AccessControlEntry.create(userSecurityContext.getSubject(), Verb.All),
				AccessControlEntry.create(attendee1SecurityContext.getSubject(), Verb.All),
				AccessControlEntry.create(attendee2SecurityContext.getSubject(), Verb.Write));
		try {
			aclStoreData.store(userCalendarContainer, ace);
			aclStoreData.store(userTagContainer, ace);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			// Cannot modifiy private event with write verb
			ICalendar attendee2CalApi = getCalendarService(attendee2SecurityContext, userCalendarContainer);
			attendee2CalApi.updateWithItem(eventItem);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		// Can modifiy private event with manage verb
		ICalendar attendee1CalApi = getCalendarService(attendee1SecurityContext, userCalendarContainer);
		eventItem.version++;
		attendee1CalApi.updateWithItem(eventItem);

		ItemValue<VEventSeries> attendee1UpdatedItem = attendee1CalApi.getComplete(eventItem.uid);
		assertItemEquals(eventItem, attendee1UpdatedItem);
		message = updatedMessageChecker.shouldSuccess();
		assertNotNull(message);
	}

	private static <T> void assertItemEquals(ItemValue<T> expected, ItemValue<T> actual) {
		assertNotNull(actual);
		assertEquals(expected.internalId, actual.internalId);
		assertEquals(expected.uid, actual.uid);
		assertEquals(expected.externalId, actual.externalId);
		assertEquals(expected.updated, actual.updated);
		assertEquals(expected.version, actual.version);
	}

	@Test
	public void testDelete() throws ServerFault {

		VertxEventChecker<JsonObject> deletedMessageChecker = new VertxEventChecker<>(
				CalendarHookAddress.EVENT_DELETED);

		VEventSeries event = defaultVEvent();
		// add attendee1
		Attendee attendee = Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, true, "", "", "",
				attendee1.value.contactInfos.identification.formatedName.value, "", "", null,
				attendee1.value.login + "@bm.lan");
		event.main.attendees.add(attendee);

		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		// test anonymous
		try {
			getCalendarService(SecurityContext.ANONYMOUS, userCalendarContainer).delete(uid, sendNotifications);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getCalendarService(userSecurityContext, userCalendarContainer).delete(uid, sendNotifications);

		ItemValue<VEventSeries> vevent = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete(uid);
		assertNull(vevent);

		vevent = getCalendarService(attendee1SecurityContext, attendee1CalendarContainer).getComplete(uid);
		assertNull(vevent);

		Message<JsonObject> message = deletedMessageChecker.shouldSuccess();
		assertNotNull(message);
	}

	@Test
	public void testDeleteRecurringEventShouldDeleteExceptions() throws Exception {
		VEventSeries event = defaultVEvent();
		RRule rrule = new RRule();
		rrule.frequency = Frequency.DAILY;
		event.main.rrule = rrule;

		// add attendee1
		Attendee attendee = Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, true, "", "", "",
				attendee1.value.contactInfos.identification.formatedName.value, "", "", null,
				attendee1.value.login + "@bm.lan");
		event.main.attendees.add(attendee);

		String uid = "test_" + System.nanoTime();

		VEventOccurrence eventException = recurringVEvent();
		eventException.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 1, 0, 0, 0, tz));

		event.occurrences = Arrays.asList(eventException);

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, false);

		Thread.sleep(1000);

		getCalendarService(userSecurityContext, userCalendarContainer).delete(uid, sendNotifications);

		ItemValue<VEventSeries> evt = getCalendarService(userSecurityContext, userCalendarContainer).getComplete(uid);
		assertNull(evt);
	}

	@Test
	public void testGetComplete() throws ServerFault {
		VEventSeries event = defaultVEvent();
		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		// test anonymous
		try {
			getCalendarService(SecurityContext.ANONYMOUS, userCalendarContainer).getComplete(uid);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		ItemValue<VEventSeries> vevent = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete(uid);
		assertNotNull(vevent);

		assertEquals("Asia/Ho_Chi_Minh", vevent.value.main.timezone());
		System.err.println(event.main.dtstart);
		System.err.println(vevent.value.main.dtstart);
		assertEquals(event.main.dtstart.iso8601, vevent.value.main.dtstart.iso8601);
		assertNotNull(vevent.value.main.categories);
		assertEquals(2, vevent.value.main.categories.size());
		assertEquals(0, vevent.value.properties.size());

		assertEquals(uid, vevent.uid);
		vevent = getCalendarService(userSecurityContext, userCalendarContainer).getComplete("nonExistant");
		assertNull(vevent);
	}

	@Test
	public void testMUpdates() throws ServerFault, SQLException {
		VEventSeries new1 = defaultVEvent();
		VEventSeries new2 = defaultVEvent();
		String new1Id = "test_1" + System.nanoTime();
		String new2Id = "test_2" + System.nanoTime();

		VEventSeries update = defaultVEvent();
		String updateUID = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(updateUID, update, sendNotifications);
		update.main.summary = "update" + System.currentTimeMillis();

		VEventSeries delete = defaultVEvent();
		String deleteUID = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(deleteUID, delete, sendNotifications);

		VEventChanges.ItemAdd add1 = VEventChanges.ItemAdd.create(new1Id, new1, false);
		VEventChanges.ItemAdd add2 = VEventChanges.ItemAdd.create(new2Id, new2, false);

		VEventChanges.ItemModify modify = VEventChanges.ItemModify.create(updateUID, update, false);

		ItemDelete itemDelete = VEventChanges.ItemDelete.create(deleteUID, false);

		VEventChanges changes = VEventChanges.create(Arrays.asList(add1, add2), Arrays.asList(modify),
				Arrays.asList(itemDelete));

		// test anonymous
		try {
			getCalendarService(SecurityContext.ANONYMOUS, userCalendarContainer).updates(changes);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getCalendarService(userSecurityContext, userCalendarContainer).updates(changes);

		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete(deleteUID);
		assertNull(item);

		item = getCalendarService(userSecurityContext, userCalendarContainer).getComplete(new1Id);
		assertNotNull(item);

		item = getCalendarService(userSecurityContext, userCalendarContainer).getComplete(new2Id);
		assertNotNull(item);

		item = getCalendarService(userSecurityContext, userCalendarContainer).getComplete(updateUID);
		assertNotNull(item);
		assertEquals(update.main.summary, item.value.main.summary);

	}

	@Test
	public void testSearch() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.summary = "yay";
		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		VEventQuery query = VEventQuery.create("value.summary:yay");

		// test anonymous
		try {
			getCalendarService(SecurityContext.ANONYMOUS, userCalendarContainer).search(query);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		ListResult<ItemValue<VEventSeries>> res = getCalendarService(userSecurityContext, userCalendarContainer)
				.search(query);
		assertEquals(1, res.values.size());
		ItemValue<VEventSeries> itemValue = res.values.get(0);
		VEvent found = itemValue.value.main;
		assertEquals("yay", found.summary);

		query = VEventQuery.create("value.summary:what?");
		res = getCalendarService(userSecurityContext, userCalendarContainer).search(query);
		assertEquals(0, res.values.size());

		VEventSeries event2 = defaultVEvent();
		String uid2 = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid2, event2, sendNotifications);

		query = VEventQuery.create("value.location:Toulouse");
		res = getCalendarService(userSecurityContext, userCalendarContainer).search(query);
		assertEquals(2, res.values.size());
	}

	@Test
	public void testCreateImproperVEvent() throws ServerFault {
		VEventSeries vevent = null;
		String uid = "test_" + System.nanoTime();

		try {
			getCalendarService(userSecurityContext, userCalendarContainer).create(uid, vevent, sendNotifications);
			fail();
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testChangeset() throws ServerFault {

		getCalendarService(userSecurityContext, userCalendarContainer).create("test1", defaultVEvent(),
				sendNotifications);
		getCalendarService(userSecurityContext, userCalendarContainer).create("test2", defaultVEvent(),
				sendNotifications);
		getCalendarService(userSecurityContext, userCalendarContainer).delete("test1", sendNotifications);
		getCalendarService(userSecurityContext, userCalendarContainer).update("test2", defaultVEvent(),
				sendNotifications);

		// begin tests
		ContainerChangeset<String> changeset = getCalendarService(userSecurityContext, userCalendarContainer)
				.changeset(null);

		assertEquals(1, changeset.created.size());
		assertEquals("test2", changeset.created.get(0));

		assertEquals(0, changeset.deleted.size());

		getCalendarService(userSecurityContext, userCalendarContainer).delete("test2", sendNotifications);
		changeset = getCalendarService(userSecurityContext, userCalendarContainer).changeset(changeset.version);

		assertEquals(0, changeset.created.size());
		assertEquals(1, changeset.deleted.size());
		assertEquals("test2", changeset.deleted.get(0));
	}

	@Test
	public void testSync() throws ServerFault {

		// test anonymous
		try {
			getCalendarService(SecurityContext.ANONYMOUS, userCalendarContainer).sync(null, null);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		ContainerChangeset<String> changeset = getCalendarService(userSecurityContext, userCalendarContainer).sync(null,
				null);

		assertEquals(0, changeset.created.size());
		assertEquals(0, changeset.updated.size());
		assertEquals(0, changeset.deleted.size());

		changeset = getCalendarService(userSecurityContext, userCalendarContainer).sync(changeset.version,
				new VEventChanges());

		assertEquals(0, changeset.created.size());
		assertEquals(0, changeset.updated.size());
		assertEquals(0, changeset.deleted.size());

		VEventSeries event1 = defaultVEvent();
		String evt1Id = "test_" + System.nanoTime();
		VEventChanges.ItemAdd add1 = VEventChanges.ItemAdd.create(evt1Id, event1, false);
		VEventChanges changes = VEventChanges.create(Arrays.asList(add1), null, null);

		changeset = getCalendarService(userSecurityContext, userCalendarContainer).sync(changeset.version, changes);

		assertEquals(1, changeset.created.size());
		assertEquals(evt1Id, changeset.created.get(0));
		assertEquals(0, changeset.updated.size());
		assertEquals(0, changeset.deleted.size());

		changeset = getCalendarService(userSecurityContext, userCalendarContainer).sync(changeset.version,
				new VEventChanges());

		assertEquals(0, changeset.created.size());
		assertEquals(0, changeset.updated.size());
		assertEquals(0, changeset.deleted.size());

		VEventSeries event2 = defaultVEvent();
		String evt2Id = "test_" + System.nanoTime();
		VEventChanges.ItemAdd add2 = VEventChanges.ItemAdd.create(evt2Id, event2, false);

		ItemDelete deleteEvent1 = VEventChanges.ItemDelete.create(evt1Id, false);

		changes = VEventChanges.create(Arrays.asList(add2), null, Arrays.asList(deleteEvent1));

		changeset = getCalendarService(userSecurityContext, userCalendarContainer).sync(changeset.version, changes);

		assertEquals(1, changeset.created.size());
		assertEquals(evt2Id, changeset.created.get(0));
		assertEquals(0, changeset.updated.size());
		assertEquals(1, changeset.deleted.size());
		assertEquals(evt1Id, changeset.deleted.get(0));

		// Updated
		event2.main.summary = "updated event 2";
		VEventChanges.ItemModify updated = VEventChanges.ItemModify.create(evt2Id, event2, false);
		changes = VEventChanges.create(null, Arrays.asList(updated), null);
		changeset = getCalendarService(userSecurityContext, userCalendarContainer).sync(changeset.version, changes);
		assertEquals(0, changeset.created.size());
		assertEquals(1, changeset.updated.size());
		assertEquals(evt2Id, changeset.updated.get(0));
		assertEquals(0, changeset.deleted.size());

		changeset = getCalendarService(userSecurityContext, userCalendarContainer).sync(changeset.version,
				new VEventChanges());

		assertEquals(0, changeset.created.size());
		assertEquals(0, changeset.updated.size());
		assertEquals(0, changeset.deleted.size());

		VEventChanges.ItemModify modifyEvent2 = VEventChanges.ItemModify.create(evt2Id, event2, false);
		changes = VEventChanges.create(null, Arrays.asList(modifyEvent2), null);

		changeset = getCalendarService(userSecurityContext, userCalendarContainer).sync(changeset.version, changes);

		assertEquals(1, changeset.updated.size());
		assertEquals(evt2Id, changeset.updated.get(0));
		assertEquals(0, changeset.deleted.size());
	}

	@Test
	public void testSyncDeleteUnexistingEvent() throws ServerFault {
		ItemDelete deleted = VEventChanges.ItemDelete.create(UUID.randomUUID().toString(), false);

		VEventChanges changes = VEventChanges.create(null, null, Arrays.asList(deleted));

		ContainerChangeset<String> changeset = getCalendarService(userSecurityContext, userCalendarContainer).sync(null,
				changes);

		assertEquals(0, changeset.created.size());
		assertEquals(0, changeset.updated.size());
		assertEquals(0, changeset.deleted.size());
	}

	@Test
	public void testSyncUpdateUnexistingEvent() throws ServerFault {
		VEventSeries event = defaultVEvent();
		String evtId = "test_" + System.nanoTime();

		VEventChanges.ItemModify modify = VEventChanges.ItemModify.create(evtId, event, false);
		VEventChanges changes = VEventChanges.create(null, Arrays.asList(modify), null);

		ContainerChangeset<String> changeset = getCalendarService(userSecurityContext, userCalendarContainer).sync(null,
				changes);

		assertEquals(1, changeset.created.size());
		assertEquals(evtId, changeset.created.get(0));
		assertEquals(0, changeset.updated.size());
		assertEquals(0, changeset.deleted.size());

		// Check that event is created
		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete(evtId);
		assertNotNull(item);
	}

	@Test
	public void testSyncCreateExistingEvent() throws ServerFault {
		VEventSeries event = defaultVEvent();
		String evtId = "test_" + System.nanoTime();

		VEventChanges.ItemAdd add = VEventChanges.ItemAdd.create(evtId, event, false);
		VEventChanges changes = VEventChanges.create(Arrays.asList(add), null, null);

		ContainerChangeset<String> changeset = getCalendarService(userSecurityContext, userCalendarContainer).sync(null,
				changes);

		assertEquals(1, changeset.created.size());
		assertEquals(evtId, changeset.created.get(0));
		assertEquals(0, changeset.deleted.size());

		event.main.summary = "testCreateExistingEvent updated summary";
		add = VEventChanges.ItemAdd.create(evtId, event, false);
		changes = VEventChanges.create(Arrays.asList(add), null, null);

		changeset = getCalendarService(userSecurityContext, userCalendarContainer).sync(changeset.version, changes);

		assertEquals(1, changeset.updated.size());
		assertEquals(evtId, changeset.updated.get(0));
		assertEquals(0, changeset.deleted.size());

		// Check that event is updated
		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete(evtId);
		assertNotNull(item);
		assertEquals("testCreateExistingEvent updated summary", item.value.main.summary);
	}

	@Test
	public void testNoAttendee() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.attendees = null;
		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		ItemValue<VEventSeries> vevent = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete(uid);
		assertNotNull(vevent);
	}

	@Test
	public void testList() throws ServerFault {

		// test anonymous
		try {
			getCalendarService(SecurityContext.ANONYMOUS, userCalendarContainer).list();
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		ListResult<ItemValue<VEventSeries>> result = getCalendarService(userSecurityContext, userCalendarContainer)
				.list();

		assertNotNull(result);
		assertEquals(0, result.total);

		VEventSeries event = defaultVEvent();
		event.main.attendees = null;
		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		result = getCalendarService(userSecurityContext, userCalendarContainer).list();
		assertEquals(1, result.total);

		event = defaultVEvent();
		event.main.attendees = null;
		uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		result = getCalendarService(userSecurityContext, userCalendarContainer).list();
		assertEquals(2, result.total);

		getCalendarService(userSecurityContext, userCalendarContainer).delete(uid, sendNotifications);

		result = getCalendarService(userSecurityContext, userCalendarContainer).list();
		assertEquals(1, result.total);
	}

	@Test
	public void testSearchByDateInterval() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(1983, 2, 13, 0, 0, 0, 0, tz));
		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		ZonedDateTime dateMin = ZonedDateTime.of(1983, 2, 1, 0, 0, 0, 0, tz);
		ZonedDateTime dateMax = ZonedDateTime.of(1983, 3, 1, 0, 0, 0, 0, tz);
		VEventQuery query = VEventQuery.create(BmDateTimeHelper.time(dateMin), BmDateTimeHelper.time(dateMax));
		ListResult<ItemValue<VEventSeries>> res = getCalendarService(userSecurityContext, userCalendarContainer)
				.search(query);
		assertEquals(1, res.values.size());
	}

	@Test
	public void testSearchPendingEventShouldFilterOccurencesInThePast() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.organizer = new VEvent.Organizer(attendee1.value.login + "@bm.lan");
		String dir = "bm://" + domainUid + "/users/" + testUser.uid;
		Attendee me = Attendee.create(CUType.Individual, "", Role.RequiredParticipant, ParticipationStatus.NeedsAction,
				true, "", "", "", "osef", dir, null, null, null);
		event.main.attendees = Arrays.asList(me);
		event.main.rrule = new RRule();
		event.main.rrule.frequency = Frequency.DAILY;
		event.main.rrule.until = BmDateTimeHelper.time(ZonedDateTime.of(2030, 2, 13, 0, 0, 0, 0, tz));
		event.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2014, 2, 13, 0, 0, 0, 0, tz));
		event.main.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2014, 2, 13, 1, 0, 0, 0, tz));

		String uid = "test_" + System.nanoTime();
		VEventOccurrence eventExceptionPast = recurringVEvent();
		eventExceptionPast.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2015, 2, 15, 1, 0, 0, 0, tz));
		eventExceptionPast.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2015, 2, 15, 2, 0, 0, 0, tz));
		eventExceptionPast.recurid = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 0, 0, 0, 0, tz));
		eventExceptionPast.attendees = event.main.attendees;

		VEventOccurrence eventExceptionFuture = recurringVEvent();
		eventExceptionFuture.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2025, 2, 15, 1, 0, 0, 0, tz));
		eventExceptionFuture.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2025, 2, 15, 2, 0, 0, 0, tz));
		eventExceptionFuture.recurid = BmDateTimeHelper.time(ZonedDateTime.of(2025, 2, 15, 0, 0, 0, 0, tz));
		eventExceptionFuture.attendees = event.main.attendees;

		event.occurrences = Arrays.asList(eventExceptionPast, eventExceptionFuture);

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		ZonedDateTime dateMin = ZonedDateTime.of(2020, 2, 1, 0, 0, 0, 0, tz);
		VEventQuery query = VEventQuery.create(BmDateTimeHelper.time(dateMin), null);
		query.attendee = new VEventAttendeeQuery();
		query.attendee.partStatus = ParticipationStatus.NeedsAction;
		query.attendee.dir = null;
		query.attendee.calendarOwnerAsDir = true;

		ListResult<ItemValue<VEventSeries>> res = getCalendarService(userSecurityContext, userCalendarContainer)
				.search(query);
		assertEquals(1, res.values.size());
		assertEquals(1, res.values.get(0).value.occurrences.size());
	}

	@Test
	public void testBug3286() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2014, 5, 29, 0, 0, 0, 0, tz));

		RRule rrule = new RRule();
		rrule.frequency = RRule.Frequency.WEEKLY;
		rrule.until = BmDateTimeHelper.time(ZonedDateTime.of(2014, 6, 4, 0, 0, 0, 0, tz));
		List<RRule.WeekDay> weekDay = new ArrayList<RRule.WeekDay>(4);
		weekDay.add(RRule.WeekDay.MO);
		weekDay.add(RRule.WeekDay.TU);
		weekDay.add(RRule.WeekDay.TH);
		weekDay.add(RRule.WeekDay.FR);
		rrule.byDay = weekDay;

		event.main.rrule = rrule;

		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		BmDateTime dateMin = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 5, 26, 0, 0, 0, 0, tz),
				Precision.DateTime);
		BmDateTime dateMax = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 6, 2, 0, 0, 0, 0, tz), Precision.DateTime);

		VEventQuery query = VEventQuery.create(dateMin, dateMax);
		ItemValue<VEventSeries> res = getCalendarService(userSecurityContext, userCalendarContainer)
				.search(query).values.get(0);
		List<VEvent> list = OccurrenceHelper.list(res, dateMin, dateMax);
		assertEquals(2, list.size());

		BmDateTime expectedOccurrence1 = BmDateTimeHelper.time(ZonedDateTime.of(2014, 5, 29, 0, 0, 0, 0, tz));
		BmDateTime expectedOccurrence2 = BmDateTimeHelper.time(ZonedDateTime.of(2014, 5, 30, 0, 0, 0, 0, tz));

		boolean f1 = false;
		boolean f2 = false;

		for (VEvent item : list) {
			if (expectedOccurrence1.equals(item.dtstart)) {
				f1 = true;
			}
			if (expectedOccurrence2.equals(item.dtstart)) {
				f2 = true;
			}
		}

		assertTrue(f1);
		assertTrue(f2);

		dateMin = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 6, 5, 0, 0, 0, 0, tz), Precision.Date);
		dateMax = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 6, 20, 0, 0, 0, 0, tz), Precision.Date);
		query = VEventQuery.create(dateMin, dateMax);
		assertEquals(0, getCalendarService(userSecurityContext, userCalendarContainer).search(query).total);
	}

	@Test
	public void testDailyOccurrences() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2014, 2, 13, 8, 0, 0, 0, tz));
		RRule rrule = new RRule();
		rrule.frequency = RRule.Frequency.DAILY;
		rrule.interval = 1;
		rrule.count = 5;
		event.main.rrule = rrule;

		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		BmDateTime dateMin = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 1, 0, 0, 0, 0, tz), Precision.DateTime);
		BmDateTime dateMax = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 28, 0, 0, 0, 0, tz),
				Precision.DateTime);

		VEventQuery query = VEventQuery.create(dateMin, dateMax);
		ItemValue<VEventSeries> res = getCalendarService(userSecurityContext, userCalendarContainer)
				.search(query).values.get(0);
		List<VEvent> list = OccurrenceHelper.list(res, dateMin, dateMax);
		assertEquals(5, list.size());

		List<ZonedDateTime> found = new ArrayList<ZonedDateTime>(12);
		for (VEvent item : list) {
			found.add(new BmDateTimeWrapper(item.dtstart).toDateTime());
		}

		assertTrue(found.contains(ZonedDateTime.of(2014, 2, 13, 8, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2014, 2, 14, 8, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2014, 2, 15, 8, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2014, 2, 16, 8, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2014, 2, 17, 8, 0, 0, 0, tz)));

		dateMin = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 18, 0, 0, 0, 0, tz), Precision.DateTime);
		dateMax = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 28, 0, 0, 0, 0, tz), Precision.DateTime);

		query = VEventQuery.create(dateMin, dateMax);
		res = getCalendarService(userSecurityContext, userCalendarContainer).search(query).values.get(0);
		list = OccurrenceHelper.list(res, dateMin, dateMax);
		assertEquals(0, list.size());
	}

	@Test
	public void testMonthlyOccurrences() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2014, 1, 1, 8, 0, 0, 0, tz));
		RRule rrule = new RRule();
		rrule.frequency = RRule.Frequency.MONTHLY;
		rrule.interval = 1;
		event.main.rrule = rrule;

		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		BmDateTime dateMin = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 1, 0, 0, 0, 0, tz), Precision.DateTime);
		BmDateTime dateMax = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 28, 0, 0, 0, 0, tz),
				Precision.DateTime);

		VEventQuery query = VEventQuery.create(dateMin, dateMax);
		ItemValue<VEventSeries> res = getCalendarService(userSecurityContext, userCalendarContainer)
				.search(query).values.get(0);
		List<VEvent> list = OccurrenceHelper.list(res, dateMin, dateMax);
		assertEquals(1, list.size());

		ZonedDateTime expectedOccurrence1 = ZonedDateTime.of(2014, 2, 1, 8, 0, 0, 0, tz);

		boolean f1 = false;

		for (VEvent item : list) {

			if (expectedOccurrence1.equals(new BmDateTimeWrapper(item.dtstart).toDateTime())) {
				f1 = true;
			}
		}

		assertTrue(f1);

		dateMin = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 1, 1, 0, 0, 0, 0, tz), Precision.DateTime);
		dateMax = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 12, 31, 0, 0, 0, 0, tz), Precision.DateTime);
		query = VEventQuery.create(dateMin, dateMax);
		res = getCalendarService(userSecurityContext, userCalendarContainer).search(query).values.get(0);
		list = OccurrenceHelper.list(res, dateMin, dateMax);
		assertEquals(12, list.size());

		dateMin = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 18, 0, 0, 0, 0, tz), Precision.DateTime);
		dateMax = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 28, 0, 0, 0, 0, tz), Precision.DateTime);

		query = VEventQuery.create(dateMin, dateMax);
		res = getCalendarService(userSecurityContext, userCalendarContainer).search(query).values.get(0);
		list = OccurrenceHelper.list(res, dateMin, dateMax);
		assertEquals(0, list.size());
	}

	@Test
	public void testMonthlyOccurrencesException() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2014, 1, 1, 8, 0, 0, 0, tz));
		RRule rrule = new RRule();
		rrule.frequency = RRule.Frequency.MONTHLY;
		rrule.interval = 1;
		event.main.rrule = rrule;

		Set<BmDateTime> exdate = new HashSet<>(1);
		BmDateTime exDate = BmDateTimeHelper.time(ZonedDateTime.of(2014, 6, 1, 8, 0, 0, 0, tz));
		exdate.add(exDate);
		event.main.exdate = exdate;

		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		BmDateTime dateMin = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 1, 1, 0, 0, 0, 0, tz), Precision.DateTime);
		BmDateTime dateMax = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 12, 31, 0, 0, 0, 0, tz),
				Precision.DateTime);

		VEventQuery query = VEventQuery.create(dateMin, dateMax);
		ItemValue<VEventSeries> res = getCalendarService(userSecurityContext, userCalendarContainer)
				.search(query).values.get(0);
		List<VEvent> list = OccurrenceHelper.list(res, dateMin, dateMax);
		assertEquals(11, list.size());
	}

	@Test
	public void testMonthlyByDayOccurrences() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.summary = "monthlyByDay";
		event.main.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2010, 2, 4, 17, 0, 0, 0, tz),
				Precision.DateTime);
		event.main.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2010, 2, 4, 18, 0, 0, 0, tz), Precision.DateTime);

		// Every _1st_ thurday
		RRule rrule = new RRule();
		rrule.frequency = RRule.Frequency.MONTHLY;
		List<RRule.WeekDay> weekDay = new ArrayList<RRule.WeekDay>(1);
		weekDay.add(new RRule.WeekDay("TH", 1));
		rrule.byDay = weekDay;
		rrule.interval = 1;
		event.main.rrule = rrule;

		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		BmDateTime dateMin = BmDateTimeWrapper.create(ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, tz), Precision.DateTime);
		BmDateTime dateMax = BmDateTimeWrapper.create(ZonedDateTime.of(2011, 12, 31, 0, 0, 0, 0, tz),
				Precision.DateTime);
		VEventQuery query = VEventQuery.create(dateMin, dateMax);
		ItemValue<VEventSeries> res = getCalendarService(userSecurityContext, userCalendarContainer)
				.search(query).values.get(0);
		List<VEvent> list = OccurrenceHelper.list(res, dateMin, dateMax);

		List<ZonedDateTime> found = new ArrayList<ZonedDateTime>(12);
		for (VEvent item : list) {
			found.add(new BmDateTimeWrapper(item.dtstart).toDateTime());
		}

		for (ZonedDateTime dd : found) {
			System.err.println(dd.toString());
		}

		assertTrue(found.contains(ZonedDateTime.of(2011, 1, 6, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 2, 3, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 3, 3, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 4, 7, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 5, 5, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 6, 2, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 7, 7, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 8, 4, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 9, 1, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 10, 6, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 11, 3, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 12, 1, 17, 0, 0, 0, tz)));

		// Every _LAST_ monday
		rrule = new RRule();
		rrule.frequency = RRule.Frequency.MONTHLY;
		weekDay = new ArrayList<RRule.WeekDay>(1);
		weekDay.add(new RRule.WeekDay("MO", -1));
		rrule.byDay = weekDay;
		rrule.interval = 1;
		event.main.rrule = rrule;

		Set<BmDateTime> exdate = new HashSet<>(1);
		BmDateTime exDate = BmDateTimeHelper.time(ZonedDateTime.of(2011, 2, 28, 17, 0, 0, 0, tz));
		exdate.add(exDate);
		event.main.exdate = exdate;

		uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		// we have 2 results here, so we need to flatmap the outcome
		list = getCalendarService(userSecurityContext, userCalendarContainer).search(query).values.stream()
				.flatMap((series) -> {
					return OccurrenceHelper.list(series, dateMin, dateMax).stream();
				}).collect(Collectors.toList());

		assertEquals(23, list.size());

		found = new ArrayList<ZonedDateTime>();
		for (VEvent item : list) {
			found.add(new BmDateTimeWrapper(item.dtstart).toDateTime());
		}

		assertTrue(found.contains(ZonedDateTime.of(2011, 1, 6, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 2, 3, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 3, 3, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 4, 7, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 5, 5, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 6, 2, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 7, 7, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 8, 4, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 9, 1, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 10, 6, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 11, 3, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 12, 1, 17, 0, 0, 0, tz)));

		assertTrue(found.contains(ZonedDateTime.of(2011, 1, 31, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 3, 28, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 4, 25, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 5, 30, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 6, 27, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 7, 25, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 8, 29, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 9, 26, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 10, 31, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 11, 28, 17, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2011, 12, 26, 17, 0, 0, 0, tz)));
	}

	@Test
	public void testYearlyOccurrences() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.summary = "Yearly";
		event.main.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2000, 12, 25, 19, 0, 0, 0, tz),
				Precision.DateTime);
		event.main.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2000, 12, 25, 20, 0, 0, 0, tz),
				Precision.DateTime);
		RRule rrule = new RRule();
		rrule.frequency = RRule.Frequency.YEARLY;
		rrule.interval = 1;
		event.main.rrule = rrule;

		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		BmDateTime dateMin = BmDateTimeWrapper.create(ZonedDateTime.of(2002, 1, 1, 0, 0, 0, 0, tz), Precision.DateTime);
		BmDateTime dateMax = BmDateTimeWrapper.create(ZonedDateTime.of(2023, 12, 31, 0, 0, 0, 0, tz),
				Precision.DateTime);
		VEventQuery query = VEventQuery.create(dateMin, dateMax);
		ItemValue<VEventSeries> res = getCalendarService(userSecurityContext, userCalendarContainer)
				.search(query).values.get(0);
		List<VEvent> list = OccurrenceHelper.list(res, dateMin, dateMax);
		assertEquals(22, list.size());
	}

	@Test
	public void testUpdateUnknownEvent() throws ServerFault {
		try {
			getCalendarService(userSecurityContext, userCalendarContainer).update(UUID.randomUUID().toString(),
					defaultVEvent(), sendNotifications);
			fail();
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.NOT_FOUND, sf.getCode());
		}
	}

	@Test
	public void testNoAlarm() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.alarm = null;
		event.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2000, 12, 25, 19, 0, 0, 0, tz));
		event.main.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2000, 12, 25, 20, 0, 0, 0, tz));

		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		ItemValue<VEventSeries> vevent = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete(uid);
		assertNotNull(vevent);
		assertNull(vevent.value.main.alarm);
	}

	@Test
	public void testSetAlarm() throws ServerFault {
		VEventSeries event = defaultVEvent();

		event.main.alarm = new ArrayList<ICalendarElement.VAlarm>(1);
		event.main.alarm.add(ICalendarElement.VAlarm.create(Action.Email, -600, "alarm desc", 10, 1, "w00t"));

		event.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2000, 12, 25, 19, 0, 0, 0, tz));
		event.main.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2000, 12, 25, 20, 0, 0, 0, tz));

		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		ItemValue<VEventSeries> vevent = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete(uid);
		assertNotNull(vevent);

		assertEquals(1, vevent.value.main.alarm.size());
		VAlarm alarm = vevent.value.main.alarm.get(0);
		assertEquals(Action.Email, alarm.action);
		assertEquals(-600, alarm.trigger.intValue());
		assertEquals("alarm desc", alarm.description);
		assertEquals(10, alarm.duration.intValue());
		assertEquals(1, alarm.repeat.intValue());
		assertEquals("w00t", alarm.summary);

		vevent.value.main.alarm = null;
		getCalendarService(userSecurityContext, userCalendarContainer).update(uid, vevent.value, sendNotifications);
		vevent = getCalendarService(userSecurityContext, userCalendarContainer).getComplete(uid);
		assertNotNull(vevent);
		assertNull(vevent.value.main.alarm);
	}

	@Test
	public void testSpecialChars() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.summary = "l'apéro à 4€ élision";

		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		VEventQuery query = VEventQuery.create("value.summary:\"l'apéro à 4€ élision\"");

		ListResult<ItemValue<VEventSeries>> res = getCalendarService(userSecurityContext, userCalendarContainer)
				.search(query);
		assertEquals(1, res.values.size());
		ItemValue<VEventSeries> itemValue = res.values.get(0);
		VEventSeries found = itemValue.value;
		assertEquals(event.main.summary, found.main.summary);

		String[] patterns = event.main.summary.split(" ");
		for (String p : patterns) {
			query = VEventQuery.create("value.summary:\"" + p + "\"");
			res = getCalendarService(userSecurityContext, userCalendarContainer).search(query);
			assertEquals(1, res.values.size());
			itemValue = res.values.get(0);
			found = itemValue.value;
			assertEquals(event.main.summary, found.main.summary);
		}
	}

	@Test
	public void testCreateAllDay() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2022, 2, 13, 1, 0, 0, 0, tz), Precision.Date);
		long ts = ZonedDateTime.of(2022, 2, 14, 0, 0, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli();
		event.main.dtend = BmDateTimeWrapper.fromTimestamp(ts, null);
		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);
		ItemValue<VEventSeries> vevent = getCalendarService(userSecurityContext, userCalendarContainer)
				.getComplete(uid);
		assertNotNull(vevent);
		assertTrue(vevent.value.main.allDay());
	}

	@Test
	public void testUpdateStatus() throws ServerFault {
		VEventSeries event = defaultVEvent();
		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);
		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer).getComplete(uid);

		event = item.value;
		assertEquals(ParticipationStatus.Accepted, event.main.attendees.get(0).partStatus);

		event.main.attendees.get(0).partStatus = ParticipationStatus.NeedsAction;

		getCalendarService(userSecurityContext, userCalendarContainer).update(uid, event, sendNotifications);

		item = getCalendarService(userSecurityContext, userCalendarContainer).getComplete(uid);
		event = item.value;
		assertEquals(ParticipationStatus.NeedsAction, event.main.attendees.get(0).partStatus);
	}

	@Test
	public void testUpdateTag() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.categories = new ArrayList<TagRef>(1);
		event.main.categories.add(tagRef1);
		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);
		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer).getComplete(uid);

		event = item.value;
		assertEquals(ParticipationStatus.Accepted, event.main.attendees.get(0).partStatus);

		assertNotNull(event.main.categories);
		assertEquals(1, event.main.categories.size());

		event.main.attendees.get(0).partStatus = ParticipationStatus.NeedsAction;
		event.main.categories.add(tagRef2);

		getCalendarService(userSecurityContext, userCalendarContainer).update(uid, event, sendNotifications);

		item = getCalendarService(userSecurityContext, userCalendarContainer).getComplete(uid);
		event = item.value;
		assertEquals(ParticipationStatus.NeedsAction, event.main.attendees.get(0).partStatus);
		assertEquals(2, event.main.categories.size());
	}

	// @Test
	// public void testForbidden() throws ServerFault {
	// VEventSeries event = defaultVEvent();
	// String uid = "test_" + System.nanoTime();
	//
	// // add forbidden
	// Attendee attendee =
	// Attendee.create(CUType.Individual, "",
	// Role.RequiredParticipant,
	// ParticipationStatus.NeedsAction, true, "", "", "",
	// forbidden.value.contactInfos.identification.formatedName.value, "", "",
	// null,
	// forbidden.value.login + "@bm.lan");
	// event.attendees.add(attendee);
	//
	// getCalendarService(userSecurityContext,
	// userCalendarContainer).create(uid, event, sendNotifications);
	// ItemValue<VEventSeries> item = getCalendarService(userSecurityContext,
	// userCalendarContainer).getComplete(uid);
	// event = item.value;
	//
	// assertEquals(2, event.attendees.size());
	//
	// boolean forbiddenFound = false;
	// for (Attendee a : event.attendees) {
	// if (a.mailto.equals(forbidden.value.login + "@bm.lan")) {
	// assertEquals(ParticipationStatus.Forbidden, a.partStatus);
	// forbiddenFound = true;
	// }
	// }
	//
	// assertTrue(forbiddenFound);
	// }

	@Test
	public void testExpandDlistMembers() throws ServerFault {
		VEventSeries event = defaultVEvent();
		String uid = "test_" + System.nanoTime();

		// add vcard with 2 members to event attendees
		// FIXME CUType.Group ? don't really care for now
		Attendee attendee = Attendee.create(CUType.Group, "", Role.RequiredParticipant, ParticipationStatus.NeedsAction,
				true, "", "", "", "", "", "", "book:Contacts_" + testUser.uid + "/" + dlistItemValue.uid,
				dlistItemValue.value.defaultMail());

		event.main.attendees.add(attendee);

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);
		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer).getComplete(uid);
		event = item.value;

		// BM-9467 groups are not expanded ! ( should be 3 attendees if
		// autoExpand DList
		assertEquals(2, event.main.attendees.size());
	}

	@Test
	public void testOnTagChanged() throws ServerFault, SQLException {
		VEventSeries event = defaultVEvent();
		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		ChangelogStore changelogStore = new ChangelogStore(dataDataSource, userCalendarContainer);
		ContainerChangeset<String> changeset = changelogStore.changeset(0, Long.MAX_VALUE);
		long version = changeset.version;
		VertxEventChecker<JsonObject> changedMessageChecker = new VertxEventChecker<>(
				CalendarHookAddress.getChangedEventAddress(userCalendarContainer.uid));
		ITags tags = ServerSideServiceProvider.getProvider(userSecurityContext).instance(ITags.class,
				userTagContainer.uid);

		tag1.label = "udpated " + System.currentTimeMillis();
		tags.update("tag1", tag1);

		Message<JsonObject> message = changedMessageChecker.shouldSuccess();
		assertNotNull(message);

		changeset = changelogStore.changeset(0, Long.MAX_VALUE);
		assertTrue(version < changeset.version);
	}

	@Test
	public void testInviteGroupFromDir() throws ServerFault {
		VEventSeries event = defaultVEvent();
		String uid = "test_" + System.nanoTime();
		// add group with 2 members to event attendees
		Attendee attendee = Attendee.create(CUType.Group, "", Role.RequiredParticipant, ParticipationStatus.NeedsAction,
				true, "", "", "", "", "", "", "", null);
		attendee.dir = "bm://bm.lan/groups/" + groupUid;
		event.main.attendees.add(attendee);

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);
		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer).getComplete(uid);
		event = item.value;

		assertEquals(2, event.main.attendees.size());
	}

	@Test
	public void testInviteGroupFromEmail() throws ServerFault {
		VEventSeries event = defaultVEvent();
		String uid = "test_" + System.nanoTime();
		// add group with 2 members to event attendees
		Attendee attendee = Attendee.create(CUType.Group, "", Role.RequiredParticipant, ParticipationStatus.NeedsAction,
				true, "", "", "", "", "", "", null, group.emails.iterator().next().address);

		event.main.attendees.add(attendee);

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);
		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer).getComplete(uid);
		event = item.value;

		assertEquals(2, event.main.attendees.size());
	}

	@Test
	public void testReset() throws ServerFault {
		VEventSeries event1 = defaultVEvent();
		String uid1 = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid1, event1, sendNotifications);

		VEventSeries event2 = defaultVEvent();
		String uid2 = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid2, event2, sendNotifications);

		// test anonymous
		try {
			waitFor(getCalendarService(SecurityContext.ANONYMOUS, userCalendarContainer).reset());
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		waitFor(getCalendarService(userSecurityContext, userCalendarContainer).reset());

		ListResult<ItemValue<VEventSeries>> list = getCalendarService(userSecurityContext, userCalendarContainer)
				.list();
		assertEquals(0, list.total);
	}

	private void waitFor(TaskRef taskRef) throws ServerFault {
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
	}

	@Test
	public void testCustomProperties() throws ServerFault {
		ICalendar service = getCalendarService(userSecurityContext, userCalendarContainer);

		VEventSeries event = defaultVEvent();
		Map<String, String> properties = new HashMap<String, String>();
		properties.put("wat", "da funk");
		event.properties = properties;

		String uid = "test_" + System.nanoTime();
		service.create(uid, event, sendNotifications);

		ItemValue<VEventSeries> vevent = service.getComplete(uid);
		assertEquals(1, vevent.value.properties.size());
		assertEquals("da funk", vevent.value.properties.get("wat"));

		properties.put("another custom prop", "yeah yeah");
		event.properties = properties;
		service.update(uid, event, sendNotifications);
		vevent = service.getComplete(uid);
		assertEquals(2, vevent.value.properties.size());
		assertEquals("da funk", vevent.value.properties.get("wat"));
		assertEquals("yeah yeah", vevent.value.properties.get("another custom prop"));

		event.properties = null;
		service.update(uid, event, sendNotifications);
		vevent = service.getComplete(uid);
		assertEquals(ImmutableMap.of("wat", "da funk" //
				, "another custom prop", "yeah yeah"), vevent.value.properties);

		event.properties = new HashMap<String, String>();
		service.update(uid, event, sendNotifications);
		vevent = service.getComplete(uid);
		assertEquals(0, vevent.value.properties.size());
	}

	@Test
	public void testRDate() throws ServerFault {

		VEventSeries event = defaultVEvent();
		event.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2014, 1, 1, 8, 0, 0, 0, tz));
		RRule rrule = new RRule();
		rrule.frequency = RRule.Frequency.MONTHLY;
		rrule.interval = 1;
		event.main.rrule = rrule;

		Set<BmDateTime> exdate = new HashSet<>(1);
		BmDateTime exDate = BmDateTimeHelper.time(ZonedDateTime.of(2014, 6, 1, 8, 0, 0, 0, tz));
		exdate.add(exDate);
		event.main.exdate = exdate;

		Set<BmDateTime> rdate = new HashSet<>(1);
		BmDateTime rDate1 = BmDateTimeHelper.time(ZonedDateTime.of(2014, 6, 13, 12, 0, 0, 0, tz));
		rdate.add(rDate1);

		BmDateTime rDate2 = BmDateTimeHelper.time(ZonedDateTime.of(2014, 7, 14, 16, 0, 0, 0, tz));
		rdate.add(rDate2);

		event.main.rdate = rdate;

		String uid = "test_" + System.nanoTime();

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		BmDateTime dateMin = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 1, 1, 0, 0, 0, 0, tz), Precision.DateTime);
		BmDateTime dateMax = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 12, 31, 0, 0, 0, 0, tz),
				Precision.DateTime);

		VEventQuery query = VEventQuery.create(dateMin, dateMax);
		ItemValue<VEventSeries> res = getCalendarService(userSecurityContext, userCalendarContainer)
				.search(query).values.get(0);
		List<VEvent> list = OccurrenceHelper.list(res, dateMin, dateMax);

		assertEquals(13, list.size());

		boolean exDateFound = false;
		boolean rDateFound = false;
		boolean rDate2Found = false;

		for (VEvent item : list) {

			if (dateEquals(exDate, item.dtstart)) {
				exDateFound = true;
			}

			if (dateEquals(rDate1, item.dtstart)) {
				rDateFound = true;
			}

			if (dateEquals(rDate2, item.dtstart)) {
				rDate2Found = true;
			}

		}
		assertFalse(exDateFound);
		assertTrue(rDateFound);
		assertTrue(rDate2Found);
	}

	private static boolean dateEquals(BmDateTime a, BmDateTime b) {
		return new BmDateTimeWrapper(a).toUTCTimestamp() == new BmDateTimeWrapper(b).toUTCTimestamp();
	}

	@Test
	public void testUpdateRecurringEventShouldDeleteExceptions() throws Exception {
		VEventSeries event = defaultVEvent();
		event.main.rrule = new RRule();
		event.main.rrule.frequency = Frequency.DAILY;
		String uid = "test_" + System.nanoTime();

		VEventOccurrence eventException = recurringVEvent();
		eventException.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 1, 0, 0, 0, tz));
		eventException.recurid = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 15, 1, 0, 0, 0, tz));

		event.occurrences = Arrays.asList(eventException);

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);
		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer).getComplete(uid);

		event = defaultVEvent();
		getCalendarService(userSecurityContext, userCalendarContainer).update(uid, event, sendNotifications);

		item = getCalendarService(userSecurityContext, userCalendarContainer).getComplete(uid);
		assertEquals(0, item.value.occurrences.size());

	}

	@Test(timeout = 360000)
	public void testParallel() throws Exception {

		ExecutorService ee = Executors.newFixedThreadPool(20);

		LinkedList<Future<?>> futures = new LinkedList<>();
		int execs = 1000;
		CompletableFuture<?>[] creates = new CompletableFuture<?>[execs];
		CompletableFuture<?>[] updates = new CompletableFuture<?>[execs];
		for (int i = 0; i < execs; i++) {
			creates[i] = new CompletableFuture<>();
			updates[i] = new CompletableFuture<>();
		}
		for (int i = 0; i < execs; i++) {
			final int t = i;
			Future<?> futre = ee.submit(new Runnable() {

				@Override
				public void run() {
					try {
						if (t % 3 == 0) {
							VEventSeries event = defaultVEvent();
							getCalendarService(userSecurityContext, userCalendarContainer).create("tt" + t, event,
									false);
							creates[t].complete(null);
						} else if (t % 3 == 1) {
							creates[t - 1].thenAccept(v -> {
								VEventSeries event = defaultVEvent();
								getCalendarService(userSecurityContext, userCalendarContainer).update("tt" + (t - 1),
										event, false);
								updates[t - 1].complete(null);
							});
						} else {
							updates[t - 2].thenAccept(v -> {
								getCalendarService(userSecurityContext, userCalendarContainer).delete("tt" + (t - 2),
										false);
							});
						}

					} catch (ServerFault e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}
				}

			});
			futures.add(futre);
		}

		LinkedList<Exception> errors = new LinkedList<>();
		for (Future<?> f : futures) {
			try {
				f.get();
			} catch (Exception e) {
				errors.add(e);
			}
		}

		if (!errors.isEmpty()) {
			for (Exception e : errors) {
				e.printStackTrace();
			}
		}

		assertTrue(errors.isEmpty());
	}

	@Test
	public void twoEventsOneItem() throws ServerFault, SQLException {
		VEventSeries event = defaultVEvent();
		String uid = UUID.randomUUID().toString();

		ICalendar service = getCalendarService(userSecurityContext, userCalendarContainer);

		try {
			service.create(uid, event, sendNotifications);
		} catch (Exception e) {
			fail();
		}

		try {
			service.create(uid, event, sendNotifications);
			fail();
		} catch (Exception e) {
		}
	}

	/**
	 * BM-10032
	 * 
	 * @throws ServerFault
	 */
	@Test
	public void testSearchOccurrences() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2014, 2, 13, 8, 0, 0, 0, tz));
		RRule rrule = new RRule();
		rrule.frequency = RRule.Frequency.DAILY;
		rrule.interval = 1;
		rrule.count = 5;
		event.main.rrule = rrule;

		VEventOccurrence exception = recurringVEvent();
		exception.recurid = BmDateTimeHelper.time(ZonedDateTime.of(2014, 2, 13, 8, 0, 0, 0, tz));
		exception.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2014, 2, 13, 16, 0, 0, 0, tz));

		event.occurrences = Arrays.asList(exception);

		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		BmDateTime dateMin = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 1, 0, 0, 0, 0, tz), Precision.DateTime);
		BmDateTime dateMax = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 28, 0, 0, 0, 0, tz),
				Precision.DateTime);

		VEventQuery query = VEventQuery.create(dateMin, dateMax);
		ItemValue<VEventSeries> res = getCalendarService(userSecurityContext, userCalendarContainer)
				.search(query).values.get(0);
		List<VEvent> list = OccurrenceHelper.list(res, dateMin, dateMax);
		assertEquals(5, list.size());

		List<ZonedDateTime> found = new ArrayList<ZonedDateTime>(5);
		for (VEvent item : list) {
			found.add(new BmDateTimeWrapper(item.dtstart).toDateTime());
		}

		assertTrue(found.contains(ZonedDateTime.of(2014, 2, 13, 16, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2014, 2, 14, 8, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2014, 2, 15, 8, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2014, 2, 16, 8, 0, 0, 0, tz)));
		assertTrue(found.contains(ZonedDateTime.of(2014, 2, 17, 8, 0, 0, 0, tz)));
	}

	/**
	 * 
	 * @throws ServerFault
	 */
	@Test
	public void sequenceDefaultValueIsZero() throws ServerFault {
		VEventSeries event = defaultVEvent();
		String uid = UIDGenerator.uid();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);
		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer).getComplete(uid);
		assertEquals(new Integer(0), item.value.main.sequence);
		event.main.summary = "Breaking Changes!";
		event.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2020, 12, 28, 1, 0, 0, 0, tz));
		event.main.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2020, 12, 28, 1, 0, 0, 0, tz));
		getCalendarService(userSecurityContext, userCalendarContainer).update(uid, event, sendNotifications);
		item = getCalendarService(userSecurityContext, userCalendarContainer).getComplete(uid);
		assertEquals(new Integer(0), item.value.main.sequence);
	}

	@Test
	public void sequenceCanBeHandledByClient() throws ServerFault {
		VEventSeries event = defaultVEvent();
		String uid = UIDGenerator.uid();
		event.main.sequence = 1;
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);
		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer).getComplete(uid);
		assertEquals(new Integer(1), item.value.main.sequence);
		event.main.sequence = 10;
		getCalendarService(userSecurityContext, userCalendarContainer).update(uid, event, sendNotifications);
		item = getCalendarService(userSecurityContext, userCalendarContainer).getComplete(uid);
		assertEquals(new Integer(10), item.value.main.sequence);
		event.main.sequence = 3;
		getCalendarService(userSecurityContext, userCalendarContainer).update(uid, event, sendNotifications);
		item = getCalendarService(userSecurityContext, userCalendarContainer).getComplete(uid);
		assertEquals(new Integer(3), item.value.main.sequence);
	}

	/**
	 * 
	 * @throws ServerFault
	 */
	@Test
	public void draftDefaultValueIsFalse() throws ServerFault {
		VEventSeries event = defaultVEvent();
		String uid = UIDGenerator.uid();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, false);
		ItemValue<VEventSeries> item = getCalendarService(userSecurityContext, userCalendarContainer).getComplete(uid);
		assertFalse(item.value.main.draft);
		getCalendarService(userSecurityContext, userCalendarContainer).update(uid, event, false);
		item = getCalendarService(userSecurityContext, userCalendarContainer).getComplete(uid);
		assertFalse(item.value.main.draft);

	}

	@Test
	public void testMultipleGet() throws ServerFault {
		VEventSeries event = defaultVEvent();
		String uid = UUID.randomUUID().toString();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, false);

		event = defaultVEvent();
		String uid2 = UUID.randomUUID().toString();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid2, event, false);

		List<ItemValue<VEventSeries>> items = getCalendarService(userSecurityContext, userCalendarContainer)
				.multipleGet(Arrays.asList(uid, uid2));
		assertNotNull(items);
		assertEquals(2, items.size());

		items = getCalendarService(userSecurityContext, userCalendarContainer)
				.multipleGet(Arrays.asList("nonExistant"));

		assertNotNull(items);
		assertEquals(0, items.size());

		try {
			getCalendarService(SecurityContext.ANONYMOUS, userCalendarContainer).multipleGet(Arrays.asList(uid, uid2));
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void testMultipleGetById() throws ServerFault {
		VEventSeries event = defaultVEvent();
		String uid = UUID.randomUUID().toString();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, false);

		event = defaultVEvent();
		String uid2 = UUID.randomUUID().toString();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid2, event, false);

		List<ItemValue<VEventSeries>> items = getCalendarService(userSecurityContext, userCalendarContainer)
				.multipleGet(Arrays.asList(uid, uid2));
		assertNotNull(items);
		assertEquals(2, items.size());

		try {
			getCalendarService(SecurityContext.ANONYMOUS, userCalendarContainer)
					.multipleGetById(Arrays.asList(items.get(0).internalId, items.get(1).internalId));
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		items = getCalendarService(userSecurityContext, userCalendarContainer)
				.multipleGetById(Arrays.asList(items.get(0).internalId, items.get(1).internalId));
		assertNotNull(items);
		assertEquals(2, items.size());

		items = getCalendarService(userSecurityContext, userCalendarContainer)
				.multipleGetById(Arrays.asList(9876543L, 34567L));
		assertNotNull(items);
		assertEquals(0, items.size());

	}

	@Test
	public void testEventUrl() throws ServerFault {
		ICalendar service = getCalendarService(userSecurityContext, userCalendarContainer);

		VEventSeries event = defaultVEvent();
		String uid = "testEventUrl_" + System.nanoTime();
		service.create(uid, event, sendNotifications);

		ItemValue<VEventSeries> created = service.getComplete(uid);
		assertEquals(event.main.url, created.value.main.url);
		assertEquals(event.main.conference, created.value.main.conference);

		created.value.main.url = "https://updated.url";
		created.value.main.conference = "https//visio.url/updated";

		service.update(uid, created.value, null);

		ItemValue<VEventSeries> updated = service.getComplete(uid);
		assertEquals(updated.value.main.url, created.value.main.url);
		assertEquals(updated.value.main.conference, created.value.main.conference);
	}

	private ItemValue<VEventSeries> defaultVEventItem(long id) throws ParseException {
		Item item = new Item();
		item.id = id;
		item.uid = "test_" + System.nanoTime();
		item.externalId = "externalId" + System.nanoTime();
		item.displayName = "test";
		item.created = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:44:21");
		item.updated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:46:00");
		item.version = 17;
		return ItemValue.create(item, defaultVEvent());
	}
}
