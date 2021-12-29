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
package net.bluemind.todolist.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.junit.Test;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ChangeLogEntry;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ChangelogStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.vertx.VertxEventChecker;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm.Action;
import net.bluemind.tag.api.ITags;
import net.bluemind.tag.api.TagRef;
import net.bluemind.tag.persistence.ItemTagRef;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.VTodo;
import net.bluemind.todolist.api.VTodoChanges;
import net.bluemind.todolist.api.VTodoChanges.ItemDelete;
import net.bluemind.todolist.api.VTodoQuery;
import net.bluemind.todolist.hook.TodoListHookAddress;

public class TodoListServiceTests extends AbstractServiceTests {

	@Test
	public void testCreate() throws ServerFault, SQLException {

		VertxEventChecker<JsonObject> createdMessageChecker = new VertxEventChecker<>(TodoListHookAddress.CREATED);

		VTodo todo = defaultVTodo();
		String uid = "test_" + System.nanoTime();

		// test anonymous
		try {
			getService(SecurityContext.ANONYMOUS).create(uid, todo);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getService(defaultSecurityContext).create(uid, todo);

		Item item = itemStore.get(uid);
		assertNotNull(item);
		VTodo vtodo = vtodoStore.get(item);
		assertNotNull(vtodo);

		List<ItemTagRef> tags = tagRefStore.get(item);
		assertNotNull(tags);
		assertEquals(2, tags.size());

		Message<JsonObject> message = createdMessageChecker.shouldSuccess();
		assertNotNull(message);
	}

	@Test
	public void testCreateWithItem() throws Exception {
		VertxEventChecker<JsonObject> createdMessageChecker = new VertxEventChecker<>(TodoListHookAddress.CREATED);

		ItemValue<VTodo> todoItem = defaultVTodoItem(42);

		// test anonymous
		try {
			getService(SecurityContext.ANONYMOUS).createWithItem(todoItem);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getService(defaultSecurityContext).createWithItem(todoItem);

		Item item = itemStore.get(todoItem.uid);
		assertItemEquals(todoItem.item(), item);
		VTodo vtodo = vtodoStore.get(item);
		assertNotNull(vtodo);

		List<ItemTagRef> tags = tagRefStore.get(item);
		assertNotNull(tags);
		assertEquals(2, tags.size());

		Message<JsonObject> message = createdMessageChecker.shouldSuccess();
		assertNotNull(message);
	}

	@Test
	public void testOrganizer() throws ServerFault {
		VTodo todo = defaultVTodo();
		String uid = "test_" + System.nanoTime();

		getService(defaultSecurityContext).create(uid, todo);

		ItemValue<VTodo> vtodo = getService(defaultSecurityContext).getComplete(uid);
		assertNotNull(vtodo);

		assertNotNull(vtodo.value.organizer);
		assertEquals("mehdi@bm.lan", vtodo.value.organizer.mailto);
	}

	@Test
	public void testExternalOrganizer() throws ServerFault {
		VTodo todo = defaultVTodo();
		String organizer = "ext" + System.currentTimeMillis() + "@extdomain.lan";
		todo.organizer = new VTodo.Organizer(organizer);
		todo.organizer.commonName = "External Organizer";
		String uid = "test_" + System.nanoTime();

		getService(defaultSecurityContext).create(uid, todo);

		ItemValue<VTodo> vtodo = getService(defaultSecurityContext).getComplete(uid);
		assertNotNull(vtodo);

		assertNotNull(vtodo.value.organizer);
		assertEquals(organizer, vtodo.value.organizer.mailto);
		assertEquals(todo.organizer.commonName, vtodo.value.organizer.commonName);
	}

	@Test
	public void testExternalAttendee() throws ServerFault {
		VTodo todo = defaultVTodo();

		String externalEmail = "external@attendee" + System.currentTimeMillis() + ".lan";
		String externalDisplayName = "External Attendee";

		VTodo.Attendee external = VTodo.Attendee.create(VTodo.CUType.Individual, "", VTodo.Role.RequiredParticipant,
				VTodo.ParticipationStatus.NeedsAction, false, "", "", "", externalDisplayName, "", "", null,
				externalEmail);

		todo.attendees.add(external);

		String uid = "test_" + System.nanoTime();

		getService(defaultSecurityContext).create(uid, todo);

		// getComplete as testUser
		ItemValue<VTodo> vtodo = getService(defaultSecurityContext).getComplete(uid);
		assertNotNull(vtodo);

		assertEquals(3, vtodo.value.attendees.size());

		boolean externalAttendeeFound = false;

		for (VTodo.Attendee att : vtodo.value.attendees) {
			if (externalEmail.equals(att.mailto)) {
				assertEquals(externalDisplayName, att.commonName);
				externalAttendeeFound = true;
			}
			assertNotNull(att.commonName);
			assertNotNull(att.mailto);
		}

		assertTrue(externalAttendeeFound);

	}

	@Test
	public void testUpdate() throws ServerFault {

		VertxEventChecker<JsonObject> updatedMessageChecker = new VertxEventChecker<>(TodoListHookAddress.UPDATED);

		VTodo todo = defaultVTodo();
		String uid = "test_" + System.nanoTime();
		getService(defaultSecurityContext).create(uid, todo);

		// test anonymous
		try {
			getService(SecurityContext.ANONYMOUS).update(uid, todo);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getService(defaultSecurityContext).update(uid, todo);

		Message<JsonObject> message = updatedMessageChecker.shouldSuccess();
		assertNotNull(message);
	}

	@Test
	public void testUpdateWithItem() throws Exception {
		VertxEventChecker<JsonObject> updatedMessageChecker = new VertxEventChecker<>(TodoListHookAddress.UPDATED);

		VTodo todo = defaultVTodo();
		String uid = "test_" + System.nanoTime();
		getService(defaultSecurityContext).create(uid, todo);
		Item item = itemStore.get(uid);
		VTodo vtodo = vtodoStore.get(item);
		ItemValue<VTodo> todoItem = ItemValue.create(item, vtodo);
		todoItem.version += 10;
		todoItem.updated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:48:00");

		// test anonymous
		try {
			getService(SecurityContext.ANONYMOUS).updateWithItem(todoItem);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getService(defaultSecurityContext).updateWithItem(todoItem);

		Item updated = itemStore.get(uid);
		assertItemEquals(todoItem.item(), updated);

		Message<JsonObject> message = updatedMessageChecker.shouldSuccess();
		assertNotNull(message);
	}

	@Test
	public void testDelete() throws ServerFault {

		VertxEventChecker<JsonObject> deletedMessageChecker = new VertxEventChecker<>(TodoListHookAddress.DELETED);

		VTodo todo = defaultVTodo();

		String uid = "test_" + System.nanoTime();
		getService(defaultSecurityContext).create(uid, todo);

		// test anonymous
		try {
			getService(SecurityContext.ANONYMOUS).delete(uid);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getService(defaultSecurityContext).delete(uid);

		ItemValue<VTodo> vtodo = getService(defaultSecurityContext).getComplete(uid);
		assertNull(vtodo);

		Message<JsonObject> message = deletedMessageChecker.shouldSuccess();
		assertNotNull(message);
	}

	@Test
	public void testGetComplete() throws ServerFault {
		VTodo todo = defaultVTodo();
		String uid = "test_" + System.nanoTime();
		getService(defaultSecurityContext).create(uid, todo);

		// test anonymous
		try {
			getService(SecurityContext.ANONYMOUS).getComplete(uid);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		ItemValue<VTodo> vtodo = getService(defaultSecurityContext).getComplete(uid);
		assertNotNull(vtodo);

		assertEquals("UTC", vtodo.value.timezone());
		assertTrue(todo.dtstart.equals(vtodo.value.dtstart));
		assertNotNull(todo.categories);
		assertEquals(2, todo.categories.size());

		assertEquals(uid, vtodo.uid);
		vtodo = getService(defaultSecurityContext).getComplete("nonExistant");
		assertNull(vtodo);
	}

	@Test
	public void testMUpdates() throws ServerFault, SQLException {
		VTodo new1 = defaultVTodo();
		VTodo new2 = defaultVTodo();
		String new1UID = "test1_" + System.nanoTime();
		String new2UID = "test2_" + System.nanoTime();

		VTodo update = defaultVTodo();
		String updateUID = "test_" + System.nanoTime();
		getService(defaultSecurityContext).create(updateUID, update);
		update.summary = "update" + System.currentTimeMillis();

		VTodo delete = defaultVTodo();
		String deleteUID = "test_" + System.nanoTime();
		getService(defaultSecurityContext).create(deleteUID, delete);

		VTodoChanges.ItemAdd add1 = VTodoChanges.ItemAdd.create(new1UID, new1, false);
		VTodoChanges.ItemAdd add2 = VTodoChanges.ItemAdd.create(new2UID, new2, false);

		VTodoChanges.ItemModify modify = VTodoChanges.ItemModify.create(updateUID, update, false);

		ItemDelete itemDelete = VTodoChanges.ItemDelete.create(deleteUID, false);

		VTodoChanges changes = VTodoChanges.create(Arrays.asList(add1, add2), Arrays.asList(modify),
				Arrays.asList(itemDelete));

		// test anonymous
		try {
			getService(SecurityContext.ANONYMOUS).updates(changes);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getService(defaultSecurityContext).updates(changes);

		ItemValue<VTodo> item = getService(defaultSecurityContext).getComplete(deleteUID);
		assertNull(item);

		item = getService(defaultSecurityContext).getComplete(new1UID);
		assertNotNull(item);

		item = getService(defaultSecurityContext).getComplete(new2UID);
		assertNotNull(item);

		item = getService(defaultSecurityContext).getComplete(updateUID);
		assertNotNull(item);
		assertEquals(update.summary, item.value.summary);

	}

	@Test
	public void testSearch() throws ServerFault {
		VTodo todo = defaultVTodo();
		todo.summary = "yay";
		String uid = "test_" + System.nanoTime();
		getService(defaultSecurityContext).create(uid, todo);
		refreshIndex();

		VTodoQuery query = VTodoQuery.create("value.summary:yay");

		// test anonymous
		try {
			getService(SecurityContext.ANONYMOUS).search(query);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		ListResult<ItemValue<VTodo>> res = getService(defaultSecurityContext).search(query);
		assertEquals(1, res.values.size());
		ItemValue<VTodo> itemValue = res.values.get(0);
		VTodo found = itemValue.value;
		assertEquals("yay", found.summary);

		query = VTodoQuery.create("value.summary:what?");
		res = getService(defaultSecurityContext).search(query);
		assertEquals(0, res.values.size());

		VTodo todo2 = defaultVTodo();
		String uid2 = "test_" + System.nanoTime();
		getService(defaultSecurityContext).create(uid2, todo2);
		refreshIndex();

		query = VTodoQuery.create("value.location:Toulouse");
		res = getService(defaultSecurityContext).search(query);
		assertEquals(2, res.values.size());
	}

	@Test
	public void testCreateImproperVTodo() throws ServerFault {
		VTodo vtodo = null;
		String uid = "test_" + System.nanoTime();

		try {
			getService(defaultSecurityContext).create(uid, vtodo);
			fail();
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testChangeset() throws ServerFault {

		getService(defaultSecurityContext).create("test1", defaultVTodo());
		getService(defaultSecurityContext).create("test2", defaultVTodo());
		getService(defaultSecurityContext).delete("test1");
		getService(defaultSecurityContext).update("test2", defaultVTodo());

		// begin tests
		ContainerChangeset<String> changeset = getService(defaultSecurityContext).changeset(null);

		assertEquals(1, changeset.created.size());
		assertEquals("test2", changeset.created.get(0));

		assertEquals(0, changeset.deleted.size());

		getService(defaultSecurityContext).delete("test2");
		changeset = getService(defaultSecurityContext).changeset(changeset.version);

		assertEquals(0, changeset.created.size());
		assertEquals(0, changeset.updated.size());
		assertEquals(1, changeset.deleted.size());
		assertEquals("test2", changeset.deleted.get(0));
	}

	@Test
	public void testItemChangelog() throws ServerFault {

		getService(defaultSecurityContext).create("test1", defaultVTodo());
		getService(defaultSecurityContext).update("test1", defaultVTodo());
		getService(defaultSecurityContext).create("test2", defaultVTodo());
		getService(defaultSecurityContext).delete("test1");
		getService(defaultSecurityContext).update("test2", defaultVTodo());

		ItemChangelog itemChangeLog = getService(defaultSecurityContext).itemChangelog("test1", 0L);
		assertEquals(3, itemChangeLog.entries.size());
		assertEquals(ChangeLogEntry.Type.Created, itemChangeLog.entries.get(0).type);
		assertEquals(ChangeLogEntry.Type.Updated, itemChangeLog.entries.get(1).type);
		assertEquals(ChangeLogEntry.Type.Deleted, itemChangeLog.entries.get(2).type);

		itemChangeLog = getService(defaultSecurityContext).itemChangelog("test2", 0L);
		assertEquals(2, itemChangeLog.entries.size());
		assertEquals(ChangeLogEntry.Type.Created, itemChangeLog.entries.get(0).type);
		assertEquals(ChangeLogEntry.Type.Updated, itemChangeLog.entries.get(1).type);

	}

	@Test
	public void testNoAttendee() throws ServerFault {
		VTodo todo = defaultVTodo();
		todo.attendees = null;
		String uid = "test_" + System.nanoTime();

		getService(defaultSecurityContext).create(uid, todo);

		ItemValue<VTodo> vtodo = getService(defaultSecurityContext).getComplete(uid);
		assertNotNull(vtodo);
	}

	@Test
	public void testSearchByDateInterval() throws ServerFault {
		VTodo todo = defaultVTodo();
		ZonedDateTime temp = ZonedDateTime.of(1983, 2, 13, 0, 0, 0, 0, tz);
		todo.dtstart = BmDateTimeWrapper.create(temp, Precision.DateTime);
		todo.due = BmDateTimeWrapper.create(temp.plusDays(1), Precision.DateTime);
		String uid = "test_" + System.nanoTime();

		getService(defaultSecurityContext).create(uid, todo);
		refreshIndex();
		ZonedDateTime from = ZonedDateTime.of(1983, 2, 1, 0, 0, 0, 0, tz);
		ZonedDateTime to = ZonedDateTime.of(1983, 3, 1, 0, 0, 0, 0, tz);
		VTodoQuery query = VTodoQuery.create(BmDateTimeWrapper.create(from, Precision.DateTime),
				BmDateTimeWrapper.create(to, Precision.DateTime));
		ListResult<ItemValue<VTodo>> res = getService(defaultSecurityContext).search(query);
		assertEquals(1, res.values.size());
	}

	@Test
	public void testDeleteUnknownEvent() throws ServerFault {
		try {
			getService(defaultSecurityContext).delete(UUID.randomUUID().toString());
		} catch (ServerFault sf) {
			fail();
		}
	}

	@Test
	public void testUpdateUnknownEvent() throws ServerFault {
		try {
			getService(defaultSecurityContext).update(UUID.randomUUID().toString(), defaultVTodo());
			fail();
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.NOT_FOUND, sf.getCode());
		}
	}

	@Test
	public void testNoAlarm() throws ServerFault {
		VTodo todo = defaultVTodo();
		todo.alarm = null;
		todo.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2000, 12, 25, 19, 0, 0, 0, tz), Precision.DateTime);
		todo.due = BmDateTimeWrapper.create(ZonedDateTime.of(2000, 12, 25, 20, 0, 0, 0, tz), Precision.DateTime);

		String uid = "test_" + System.nanoTime();

		getService(defaultSecurityContext).create(uid, todo);

		ItemValue<VTodo> vtodo = getService(defaultSecurityContext).getComplete(uid);
		assertNotNull(vtodo);
		assertNull(vtodo.value.alarm);
	}

	@Test
	public void testSetAlarm() throws ServerFault {
		VTodo todo = defaultVTodo();

		todo.alarm = new ArrayList<ICalendarElement.VAlarm>(1);
		todo.alarm.add(ICalendarElement.VAlarm.create(Action.Email, -42, "alarm desc", 42, 1, "w00t"));

		todo.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2000, 12, 25, 19, 0, 0, 0, tz), Precision.DateTime);
		todo.due = BmDateTimeWrapper.create(ZonedDateTime.of(2000, 12, 25, 20, 0, 0, 0, tz), Precision.DateTime);

		String uid = "test_" + System.nanoTime();

		getService(defaultSecurityContext).create(uid, todo);

		ItemValue<VTodo> vtodo = getService(defaultSecurityContext).getComplete(uid);
		assertNotNull(vtodo);

		assertEquals(1, vtodo.value.alarm.size());
		VAlarm alarm = vtodo.value.alarm.get(0);
		assertEquals(Action.Email, alarm.action);
		assertEquals(-42, alarm.trigger.intValue());
		assertEquals("alarm desc", alarm.description);
		assertEquals(42, alarm.duration.intValue());
		assertEquals(1, alarm.repeat.intValue());
		assertEquals("w00t", alarm.summary);

		vtodo.value.alarm = null;
		getService(defaultSecurityContext).update(uid, vtodo.value);
		vtodo = getService(defaultSecurityContext).getComplete(uid);
		assertNotNull(vtodo);
		assertNull(vtodo.value.alarm);
	}

	@Test
	public void testSpecialChars() throws ServerFault {
		VTodo todo = defaultVTodo();
		todo.summary = "lision";

		String uid = "test_" + System.nanoTime();

		getService(defaultSecurityContext).create(uid, todo);

		refreshIndex();
		VTodoQuery query = VTodoQuery.create("value.summary:lision");

		ListResult<ItemValue<VTodo>> res = getService(defaultSecurityContext).search(query);
		assertEquals(1, res.values.size());
		ItemValue<VTodo> itemValue = res.values.get(0);
		VTodo found = itemValue.value;
		assertEquals(todo.summary, found.summary);

		String[] patterns = todo.summary.split(" ");
		for (String p : patterns) {
			query = VTodoQuery.create("value.summary:\"" + p + "\"");
			res = getService(defaultSecurityContext).search(query);
			assertEquals(1, res.values.size());
			itemValue = res.values.get(0);
			found = itemValue.value;
			assertEquals(todo.summary, found.summary);
		}
	}

	@Test
	public void testCreateAllDay() throws ServerFault {
		VTodo todo = defaultVTodo();
		Calendar gmt = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		gmt.set(Calendar.MILLISECOND, 0);
		gmt.set(2015, 11, 28, 0, 0, 0);
		todo.dtstart = BmDateTimeWrapper.fromTimestamp(gmt.getTimeInMillis());
		todo.due = null;
		String uid = "test_" + System.nanoTime();

		getService(defaultSecurityContext).create(uid, todo);
		ItemValue<VTodo> vtodo = getService(defaultSecurityContext).getComplete(uid);
		assertNotNull(vtodo);
		assertEquals(net.bluemind.core.api.date.BmDateTime.Precision.DateTime, vtodo.value.dtstart.precision);
	}

	@Test
	public void testUpdateStatus() throws ServerFault {
		VTodo todo = defaultVTodo();
		String uid = "test_" + System.nanoTime();
		getService(defaultSecurityContext).create(uid, todo);
		ItemValue<VTodo> item = getService(defaultSecurityContext).getComplete(uid);

		todo = item.value;
		assertEquals(VTodo.ParticipationStatus.Accepted, todo.attendees.get(0).partStatus);

		todo.attendees.get(0).partStatus = VTodo.ParticipationStatus.NeedsAction;

		getService(defaultSecurityContext).update(uid, todo);

		item = getService(defaultSecurityContext).getComplete(uid);
		todo = item.value;
		assertEquals(VTodo.ParticipationStatus.NeedsAction, todo.attendees.get(0).partStatus);
	}

	@Test
	public void testUpdateTag() throws ServerFault {
		VTodo todo = defaultVTodo();
		todo.categories = new ArrayList<TagRef>(1);
		todo.categories.add(tagRef1);
		String uid = "test_" + System.nanoTime();
		getService(defaultSecurityContext).create(uid, todo);
		ItemValue<VTodo> item = getService(defaultSecurityContext).getComplete(uid);

		todo = item.value;
		assertEquals(VTodo.ParticipationStatus.Accepted, todo.attendees.get(0).partStatus);

		assertNotNull(todo.categories);
		assertEquals(1, todo.categories.size());

		todo.attendees.get(0).partStatus = VTodo.ParticipationStatus.NeedsAction;
		todo.categories.add(tagRef2);

		getService(defaultSecurityContext).update(uid, todo);

		item = getService(defaultSecurityContext).getComplete(uid);
		todo = item.value;
		assertEquals(VTodo.ParticipationStatus.NeedsAction, todo.attendees.get(0).partStatus);
		assertEquals(2, todo.categories.size());
	}

	@Test
	public void testOnTagChanged() throws ServerFault, SQLException {
		VTodo todo = defaultVTodo();
		String uid = "test_" + System.nanoTime();
		getService(defaultSecurityContext).create(uid, todo);

		ChangelogStore changelogStore = new ChangelogStore(dataDataSource, container);
		ContainerChangeset<String> changeset = changelogStore.changeset(0, Long.MAX_VALUE);
		long version = changeset.version;

		VertxEventChecker<JsonObject> changedMessageChecker = new VertxEventChecker<>(
				TodoListHookAddress.getChangedEventAddress(container.uid));

		ITags tags = ServerSideServiceProvider.getProvider(defaultSecurityContext).instance(ITags.class,
				tagContainer.uid);

		tag1.label = "udpated";
		tags.update("tag1", tag1);

		Message<JsonObject> message = changedMessageChecker.shouldSuccess();
		assertNotNull(message);

		changeset = changelogStore.changeset(0, Long.MAX_VALUE);
		assertTrue(version < changeset.version);
	}

	@Test
	public void testAll() throws ServerFault {

		List<ItemValue<VTodo>> vtodos = getService(defaultSecurityContext).all();
		assertNotNull(vtodos);
		assertEquals(0, vtodos.size());

		VTodo todo = defaultVTodo();
		String uid = "todo-one";
		getService(defaultSecurityContext).create(uid, todo);
		todo = defaultVTodo();
		uid = "todo-two";
		getService(defaultSecurityContext).create(uid, todo);
		todo = defaultVTodo();
		uid = "todo-three";
		getService(defaultSecurityContext).create(uid, todo);
		// test anonymous
		try {
			getService(SecurityContext.ANONYMOUS).all();
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		vtodos = getService(defaultSecurityContext).all();
		assertNotNull(vtodos);

		assertEquals(3, vtodos.size());
	}

	@Test
	public void testDailyOccurrences() throws ServerFault {
		ITodoList service = getService(defaultSecurityContext);

		VTodo todo = defaultVTodo();
		todo.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 13, 8, 0, 0, 0, tz), Precision.DateTime);
		todo.due = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 14, 8, 0, 0, 0, tz), Precision.DateTime);
		VTodo.RRule rrule = new VTodo.RRule();
		rrule.frequency = VTodo.RRule.Frequency.DAILY;
		rrule.interval = 1;
		rrule.count = 5;
		todo.rrule = rrule;

		String uid = "testDailyOccurrences_" + System.nanoTime();

		service.create(uid, todo);
		refreshIndex();

		BmDateTime dateMin = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 1, 0, 0, 0, 0, tz), Precision.DateTime);
		BmDateTime dateMax = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 28, 0, 0, 0, 0, tz),
				Precision.DateTime);

		VTodoQuery query = VTodoQuery.create(dateMin, dateMax);
		ListResult<ItemValue<VTodo>> res = service.search(query);
		assertEquals(5, res.values.size());

		List<ZonedDateTime> foundDtstart = new ArrayList<ZonedDateTime>(12);

		List<ZonedDateTime> foundDue = new ArrayList<ZonedDateTime>(12); // savoyarde

		for (ItemValue<VTodo> item : res.values) {
			assertEquals(uid, item.uid);
			foundDtstart.add(new BmDateTimeWrapper(item.value.dtstart).toDateTime());
			foundDue.add(new BmDateTimeWrapper(item.value.due).toDateTime());
		}

		assertTrue(foundDtstart.contains(ZonedDateTime.of(2014, 2, 13, 8, 0, 0, 0, tz)));
		assertTrue(foundDtstart.contains(ZonedDateTime.of(2014, 2, 14, 8, 0, 0, 0, tz)));
		assertTrue(foundDtstart.contains(ZonedDateTime.of(2014, 2, 15, 8, 0, 0, 0, tz)));
		assertTrue(foundDtstart.contains(ZonedDateTime.of(2014, 2, 16, 8, 0, 0, 0, tz)));
		assertTrue(foundDtstart.contains(ZonedDateTime.of(2014, 2, 17, 8, 0, 0, 0, tz)));

		assertTrue(foundDue.contains(ZonedDateTime.of(2014, 2, 14, 8, 0, 0, 0, tz)));
		assertTrue(foundDue.contains(ZonedDateTime.of(2014, 2, 15, 8, 0, 0, 0, tz)));
		assertTrue(foundDue.contains(ZonedDateTime.of(2014, 2, 16, 8, 0, 0, 0, tz)));
		assertTrue(foundDue.contains(ZonedDateTime.of(2014, 2, 17, 8, 0, 0, 0, tz)));
		assertTrue(foundDue.contains(ZonedDateTime.of(2014, 2, 18, 8, 0, 0, 0, tz)));

		dateMin = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 19, 0, 0, 0, 0, tz), Precision.DateTime);
		dateMax = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 28, 0, 0, 0, 0, tz), Precision.DateTime);

		query = VTodoQuery.create(dateMin, dateMax);
		res = service.search(query);
		assertEquals(0, res.values.size());
	}

	@Test
	public void testWeeklyOccurrence() throws ServerFault {

		ITodoList service = getService(defaultSecurityContext);

		VTodo todo = defaultVTodo();
		todo.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 5, 29, 0, 0, 0, 0, tz), Precision.DateTime);
		todo.due = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 5, 29, 0, 0, 0, 0, tz), Precision.DateTime);

		VTodo.RRule rrule = new VTodo.RRule();
		rrule.frequency = VTodo.RRule.Frequency.WEEKLY;
		rrule.until = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 6, 4, 0, 0, 0, 0, tz), Precision.DateTime);
		List<VTodo.RRule.WeekDay> weekDay = new ArrayList<VTodo.RRule.WeekDay>(4);
		weekDay.add(VTodo.RRule.WeekDay.MO);
		weekDay.add(VTodo.RRule.WeekDay.TU);
		weekDay.add(VTodo.RRule.WeekDay.TH);
		weekDay.add(VTodo.RRule.WeekDay.FR);
		rrule.byDay = weekDay;
		todo.rrule = rrule;

		String uid = "testBug3286_" + System.nanoTime();

		service.create(uid, todo);
		refreshIndex();

		net.bluemind.core.api.date.BmDateTime dateMin = BmDateTimeWrapper
				.create(ZonedDateTime.of(2014, 5, 26, 0, 0, 0, 0, tz), Precision.DateTime);

		net.bluemind.core.api.date.BmDateTime dateMax = BmDateTimeWrapper
				.create(ZonedDateTime.of(2014, 6, 1, 23, 0, 0, 0, tz), Precision.DateTime);

		VTodoQuery query = VTodoQuery.create(dateMin, dateMax);

		ListResult<ItemValue<VTodo>> res = service.search(query);

		res.values.forEach(v -> {
			System.err.println(v.value.dtstart.toString());
		});

		assertEquals(2, res.values.size());

		net.bluemind.core.api.date.BmDateTime expectedOccurrence1 = BmDateTimeWrapper
				.create(ZonedDateTime.of(2014, 5, 29, 0, 0, 0, 0, tz), Precision.DateTime);
		net.bluemind.core.api.date.BmDateTime expectedOccurrence2 = BmDateTimeWrapper
				.create(ZonedDateTime.of(2014, 5, 30, 0, 0, 0, 0, tz), Precision.DateTime);

		boolean f1 = false;
		boolean f2 = false;

		for (ItemValue<VTodo> item : res.values) {
			assertEquals(uid, item.uid);
			if (expectedOccurrence1.equals(item.value.dtstart)) {
				f1 = true;
			}
			if (expectedOccurrence2.equals(item.value.dtstart)) {
				f2 = true;
			}
		}

		assertTrue(f1);
		assertTrue(f2);

		dateMin = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 6, 5, 0, 0, 0, 0, tz), Precision.Date);
		dateMax = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 6, 20, 0, 0, 0, 0, tz), Precision.Date);
		query = VTodoQuery.create(dateMin, dateMax);
		res = service.search(query);
		assertEquals(0, res.values.size());
	}

	@Test
	public void testMonthlyByDayOccurrences() throws ServerFault {

		ITodoList service = getService(defaultSecurityContext);

		VTodo todo = defaultVTodo();

		todo.summary = "monthlyByDay";
		todo.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2010, 2, 4, 17, 0, 0, 0, tz), Precision.DateTime);
		todo.due = BmDateTimeWrapper.create(ZonedDateTime.of(2010, 2, 4, 18, 0, 0, 0, tz), Precision.DateTime);

		// Every _1st_ thurday
		VTodo.RRule rrule = new VTodo.RRule();
		rrule.frequency = VTodo.RRule.Frequency.MONTHLY;
		List<VTodo.RRule.WeekDay> weekDay = new ArrayList<VTodo.RRule.WeekDay>(1);
		weekDay.add(new VTodo.RRule.WeekDay("TH", 1));
		rrule.byDay = weekDay;
		rrule.interval = 1;
		todo.rrule = rrule;

		String uid = "testMonthlyByDayOccurrences_" + System.nanoTime();

		service.create(uid, todo);
		refreshIndex();

		BmDateTime dateMin = BmDateTimeWrapper.create(ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, tz), Precision.DateTime);
		BmDateTime dateMax = BmDateTimeWrapper.create(ZonedDateTime.of(2011, 12, 31, 0, 0, 0, 0, tz),
				Precision.DateTime);
		VTodoQuery query = VTodoQuery.create(dateMin, dateMax);
		ListResult<ItemValue<VTodo>> res = service.search(query);
		assertEquals(12, res.values.size());

		List<ZonedDateTime> found = new ArrayList<ZonedDateTime>(12);
		for (ItemValue<VTodo> item : res.values) {
			assertEquals(uid, item.uid);
			found.add(new BmDateTimeWrapper(item.value.dtstart).toDateTime());
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
		rrule = new VTodo.RRule();
		rrule.frequency = VTodo.RRule.Frequency.MONTHLY;
		weekDay = new ArrayList<VTodo.RRule.WeekDay>(1);
		weekDay.add(new VTodo.RRule.WeekDay("MO", -1));
		rrule.byDay = weekDay;
		rrule.interval = 1;
		todo.rrule = rrule;

		Set<net.bluemind.core.api.date.BmDateTime> exdate = new HashSet<>(1);
		exdate.add(BmDateTimeWrapper.create(ZonedDateTime.of(2011, 2, 28, 17, 0, 0, 0, tz), Precision.DateTime));
		todo.exdate = exdate;

		uid = "testMonthlyByDayOccurrences_" + System.nanoTime();

		service.create(uid, todo);
		refreshIndex();

		res = service.search(query);
		assertEquals(23, res.values.size());

		found = new ArrayList<ZonedDateTime>(23);
		for (ItemValue<VTodo> item : res.values) {
			found.add(new BmDateTimeWrapper(item.value.dtstart).toDateTime());
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
	public void testMonthlyOccurrences() throws ServerFault {
		ITodoList service = getService(defaultSecurityContext);

		VTodo todo = defaultVTodo();

		todo.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 1, 1, 8, 0, 0, 0, tz), Precision.DateTime);
		todo.due = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 1, 1, 8, 0, 0, 0, tz), Precision.DateTime);

		VTodo.RRule rrule = new VTodo.RRule();
		rrule.frequency = VTodo.RRule.Frequency.MONTHLY;
		rrule.interval = 1;
		todo.rrule = rrule;

		String uid = "testMonthlyOccurrences_" + System.nanoTime();

		service.create(uid, todo);
		refreshIndex();

		BmDateTime dateMin = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 1, 0, 0, 0, 0, tz), Precision.DateTime);
		BmDateTime dateMax = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 28, 0, 0, 0, 0, tz),
				Precision.DateTime);

		VTodoQuery query = VTodoQuery.create(dateMin, dateMax);
		ListResult<ItemValue<VTodo>> res = service.search(query);
		assertEquals(1, res.values.size());

		ZonedDateTime expectedOccurrence1 = ZonedDateTime.of(2014, 2, 1, 8, 0, 0, 0, tz);

		boolean f1 = false;

		for (ItemValue<VTodo> item : res.values) {
			assertEquals(uid, item.uid);

			if (expectedOccurrence1.equals(new BmDateTimeWrapper(item.value.dtstart).toDateTime())) {
				f1 = true;
			}
		}

		assertTrue(f1);

		dateMin = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 1, 1, 0, 0, 0, 0, tz), Precision.DateTime);
		dateMax = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 12, 31, 0, 0, 0, 0, tz), Precision.DateTime);
		query = VTodoQuery.create(dateMin, dateMax);
		res = service.search(query);
		assertEquals(12, res.values.size());

		dateMin = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 18, 0, 0, 0, 0, tz), Precision.DateTime);
		dateMax = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 2, 28, 0, 0, 0, 0, tz), Precision.DateTime);

		query = VTodoQuery.create(dateMin, dateMax);
		res = service.search(query);
		assertEquals(0, res.values.size());
	}

	@Test
	public void testMonthlyOccurrencesException() throws ServerFault {
		ITodoList service = getService(defaultSecurityContext);

		VTodo todo = defaultVTodo();

		todo.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 1, 1, 8, 0, 0, 0, tz), Precision.DateTime);
		todo.due = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 1, 1, 8, 0, 0, 0, tz), Precision.DateTime);

		VTodo.RRule rrule = new VTodo.RRule();
		rrule.frequency = VTodo.RRule.Frequency.MONTHLY;
		rrule.interval = 1;
		todo.rrule = rrule;

		Set<net.bluemind.core.api.date.BmDateTime> exdate = new HashSet<>(1);
		BmDateTime exDate = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 6, 1, 8, 0, 0, 0, tz), Precision.DateTime);
		exdate.add(exDate);
		todo.exdate = exdate;

		String uid = "testMonthlyOccurrencesException_" + System.nanoTime();

		service.create(uid, todo);
		refreshIndex();

		BmDateTime dateMin = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 1, 1, 0, 0, 0, 0, tz), Precision.DateTime);
		BmDateTime dateMax = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 12, 31, 0, 0, 0, 0, tz),
				Precision.DateTime);

		VTodoQuery query = VTodoQuery.create(dateMin, dateMax);
		ListResult<ItemValue<VTodo>> res = service.search(query);
		assertEquals(11, res.values.size());

		boolean found = false;
		for (ItemValue<VTodo> item : res.values) {
			assertEquals(uid, item.uid);

			if (exDate.equals(item.value.dtstart)) {
				found = true;
			}
		}
		assertFalse(found);
	}

	@Test
	public void testYearlyOccurrences() throws ServerFault {
		ITodoList service = getService(defaultSecurityContext);

		VTodo todo = defaultVTodo();
		todo.summary = "Yearly";

		todo.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2000, 12, 25, 19, 0, 0, 0, tz), Precision.DateTime);
		todo.due = BmDateTimeWrapper.create(ZonedDateTime.of(2000, 12, 25, 20, 0, 0, 0, tz), Precision.DateTime);
		VTodo.RRule rrule = new VTodo.RRule();
		rrule.frequency = VTodo.RRule.Frequency.YEARLY;
		rrule.interval = 1;
		todo.rrule = rrule;

		String uid = "testYearlyOccurrences_" + System.nanoTime();

		service.create(uid, todo);
		refreshIndex();

		BmDateTime dateMin = BmDateTimeWrapper.create(ZonedDateTime.of(2002, 1, 1, 0, 0, 0, 0, tz), Precision.DateTime);
		BmDateTime dateMax = BmDateTimeWrapper.create(ZonedDateTime.of(2023, 12, 31, 0, 0, 0, 0, tz),
				Precision.DateTime);
		VTodoQuery query = VTodoQuery.create(dateMin, dateMax);
		ListResult<ItemValue<VTodo>> res = service.search(query);
		assertEquals(22, res.values.size());

		List<ZonedDateTime> found = new ArrayList<ZonedDateTime>(12);
		for (ItemValue<VTodo> item : res.values) {
			assertEquals(uid, item.uid);
			found.add(new BmDateTimeWrapper(item.value.dtstart).toDateTime());
		}

		for (int i = 2002; i < 2024; i++) {
			assertTrue(found.contains(ZonedDateTime.of(i, 12, 25, 19, 0, 0, 0, tz)));
		}
	}

	@Test
	public void testRDate() throws ServerFault {
		ITodoList service = getService(defaultSecurityContext);

		VTodo todo = defaultVTodo();

		todo.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 1, 1, 8, 0, 0, 0, tz), Precision.DateTime);
		todo.due = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 1, 1, 8, 0, 0, 0, tz), Precision.DateTime);

		VTodo.RRule rrule = new VTodo.RRule();
		rrule.frequency = VTodo.RRule.Frequency.MONTHLY;
		rrule.interval = 1;
		todo.rrule = rrule;

		Set<net.bluemind.core.api.date.BmDateTime> exdate = new HashSet<>(1);
		BmDateTime exDate = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 6, 1, 8, 0, 0, 0, tz), Precision.DateTime);
		exdate.add(exDate);
		todo.exdate = exdate;

		Set<net.bluemind.core.api.date.BmDateTime> rdates = new HashSet<>(1);
		BmDateTime rdate = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 6, 4, 8, 0, 0, 0, tz), Precision.DateTime);
		rdates.add(rdate);
		BmDateTime rdate2 = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 7, 4, 8, 0, 0, 0, tz), Precision.DateTime);
		rdates.add(rdate2);
		todo.rdate = rdates;

		String uid = "testMonthlyOccurrencesException_" + System.nanoTime();

		service.create(uid, todo);
		refreshIndex();

		BmDateTime dateMin = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 1, 1, 0, 0, 0, 0, tz), Precision.DateTime);
		BmDateTime dateMax = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 12, 31, 0, 0, 0, 0, tz),
				Precision.DateTime);

		VTodoQuery query = VTodoQuery.create(dateMin, dateMax);
		ListResult<ItemValue<VTodo>> res = service.search(query);
		assertEquals(13, res.values.size());

		boolean exDateFound = false;
		boolean rDateFound = false;
		boolean rDate2Found = false;

		for (ItemValue<VTodo> item : res.values) {
			assertEquals(uid, item.uid);

			if (exDate.equals(item.value.dtstart)) {
				exDateFound = true;
			}

			if (rdate.equals(item.value.dtstart)) {
				rDateFound = true;
			}

			if (rdate2.equals(item.value.dtstart)) {
				rDate2Found = true;
			}

		}
		assertFalse(exDateFound);
		assertTrue(rDateFound);
		assertTrue(rDate2Found);
	}

	@Test
	public void nullDtStart() throws Exception {
		VTodo todo = defaultVTodo();
		todo.dtstart = null;
		String uid = "nullDtStart_" + System.nanoTime();

		getService(defaultSecurityContext).create(uid, todo);

		Item item = itemStore.get(uid);
		assertNotNull(item);

		VTodo vtodo = vtodoStore.get(item);
		assertNotNull(vtodo);
		assertNull(vtodo.dtstart);
	}

	@Test
	public void testMultipleGet() throws ServerFault {
		VTodo todo = defaultVTodo();
		String uid = UUID.randomUUID().toString();
		getService(defaultSecurityContext).create(uid, todo);

		todo = defaultVTodo();
		String uid2 = UUID.randomUUID().toString();
		getService(defaultSecurityContext).create(uid2, todo);

		List<ItemValue<VTodo>> items = getService(defaultSecurityContext).multipleGet(Arrays.asList(uid, uid2));
		assertNotNull(items);
		assertEquals(2, items.size());

		items = getService(defaultSecurityContext).multipleGet(Arrays.asList("nonExistant"));

		assertNotNull(items);
		assertEquals(0, items.size());

		try {
			getService(SecurityContext.ANONYMOUS).multipleGet(Arrays.asList(uid, uid2));
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void testMultipleGetById() throws ServerFault {
		VTodo todo = defaultVTodo();
		String uid = UUID.randomUUID().toString();
		getService(defaultSecurityContext).create(uid, todo);

		todo = defaultVTodo();
		String uid2 = UUID.randomUUID().toString();
		getService(defaultSecurityContext).create(uid2, todo);

		List<ItemValue<VTodo>> items = getService(defaultSecurityContext).multipleGet(Arrays.asList(uid, uid2));
		assertNotNull(items);
		assertEquals(2, items.size());

		try {
			getService(SecurityContext.ANONYMOUS)
					.multipleGetById(Arrays.asList(items.get(0).internalId, items.get(1).internalId));
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		items = getService(defaultSecurityContext)
				.multipleGetById(Arrays.asList(items.get(0).internalId, items.get(1).internalId));
		assertNotNull(items);
		assertEquals(2, items.size());

		items = getService(defaultSecurityContext).multipleGetById(Arrays.asList(9876543L, 34567L));
		assertNotNull(items);
		assertEquals(0, items.size());

	}

	@Override
	protected ITodoList getService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(ITodoList.class, container.uid);
	}

	private ItemValue<VTodo> defaultVTodoItem(long id) throws ParseException {
		Item item = new Item();
		item.id = id;
		item.uid = "test_" + System.nanoTime();
		item.externalId = "externalId" + System.nanoTime();
		item.displayName = "test";
		item.created = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:44:21");
		item.updated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:46:00");
		item.version = 17;
		return ItemValue.create(item, defaultVTodo());
	}

	private static <T> void assertItemEquals(Item expected, Item actual) {
		assertNotNull(actual);
		assertEquals(expected.id, actual.id);
		assertEquals(expected.uid, actual.uid);
		assertEquals(expected.externalId, actual.externalId);
		assertEquals(expected.updated, actual.updated);
		assertEquals(expected.version, actual.version);
	}
}
