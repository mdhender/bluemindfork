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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.directory.service.xfer.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.icalendar.api.ICalendarElement.Status;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.todolist.api.VTodo;
import net.bluemind.todolist.api.VTodoChanges;
import net.bluemind.todolist.api.VTodoChanges.ItemDelete;

public class TodolistXferTests extends AbstractMultibackendTests {
	@Test
	public void testXferTodolist() {
		String container = ITodoUids.defaultUserTodoList(userUid);

		ITodoList service = ServerSideServiceProvider.getProvider(context).instance(ITodoList.class, container);

		VTodo new1 = defaultVTodo();
		VTodo new2 = defaultVTodo();
		String new1UID = "test1_" + System.nanoTime();
		String new2UID = "test2_" + System.nanoTime();

		VTodo update = defaultVTodo();
		String updateUID = "test_" + System.nanoTime();
		service.create(updateUID, update);
		update.summary = "update" + System.currentTimeMillis();

		VTodo delete = defaultVTodo();
		String deleteUID = "test_" + System.nanoTime();
		service.create(deleteUID, delete);

		VTodoChanges.ItemAdd add1 = VTodoChanges.ItemAdd.create(new1UID, new1, false);
		VTodoChanges.ItemAdd add2 = VTodoChanges.ItemAdd.create(new2UID, new2, false);

		VTodoChanges.ItemModify modify = VTodoChanges.ItemModify.create(updateUID, update, false);

		ItemDelete itemDelete = VTodoChanges.ItemDelete.create(deleteUID, false);

		VTodoChanges changes = VTodoChanges.create(Arrays.asList(add1, add2), Arrays.asList(modify),
				Arrays.asList(itemDelete));

		service.updates(changes);

		// initial container state
		int nbItems = service.all().size();
		assertEquals(3, nbItems);
		long version = service.getVersion();
		assertEquals(6, version);

		TaskRef tr = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDirectory.class, domainUid)
				.xfer(userUid, shardIp);
		waitTaskEnd(tr);

		// current service should return nothing
		assertTrue(service.all().isEmpty());

		// new ITodoList instance
		service = ServerSideServiceProvider.getProvider(context).instance(ITodoList.class, container);

		assertEquals(nbItems, service.all().size());
		assertEquals(3L, service.getVersion());

		service.create("new-one", defaultVTodo());

		ContainerChangeset<String> changeset = service.changeset(3L);
		assertEquals(1, changeset.created.size());
		assertEquals("new-one", changeset.created.get(0));
		assertTrue(changeset.updated.isEmpty());
		assertTrue(changeset.deleted.isEmpty());
	}

	protected VTodo defaultVTodo() {
		VTodo todo = new VTodo();
		todo.uid = UUID.randomUUID().toString();
		ZonedDateTime temp = ZonedDateTime.of(2024, 12, 28, 0, 0, 0, 0, ZoneId.of("UTC"));
		todo.dtstart = BmDateTimeWrapper.create(temp, Precision.DateTime);
		todo.due = BmDateTimeWrapper.create(temp.plusMonths(1), Precision.DateTime);
		todo.summary = "Test Todo";
		todo.location = "Toulouse";
		todo.description = "Lorem ipsum";
		todo.classification = VTodo.Classification.Private;
		todo.status = Status.NeedsAction;
		todo.priority = 3;

		todo.organizer = new VTodo.Organizer("mehdi@bm.lan");

		List<VTodo.Attendee> attendees = new ArrayList<>(2);

		VTodo.Attendee john = VTodo.Attendee.create(VTodo.CUType.Individual, "", VTodo.Role.Chair,
				VTodo.ParticipationStatus.Accepted, true, "", "", "", "John Bang", "", "", "uid1", "john.bang@bm.lan");
		attendees.add(john);

		VTodo.Attendee jane = VTodo.Attendee.create(VTodo.CUType.Individual, "", VTodo.Role.RequiredParticipant,
				VTodo.ParticipationStatus.NeedsAction, true, "", "", "", "Jane Bang", "", "", "uid2",
				"jane.bang@bm.lan");
		attendees.add(jane);

		todo.attendees = attendees;
		return todo;
	}

}
