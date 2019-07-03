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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.collect.Lists;

import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.api.TaskStatus.State;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.icalendar.api.ICalendarElement.Status;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.todolist.api.VTodo;
import net.bluemind.todolist.api.VTodoChanges;
import net.bluemind.todolist.api.VTodoChanges.ItemDelete;

public class TodolistXferTests {

	private String domainUid = "bm.lan";
	private String userUid = "test" + System.currentTimeMillis();
	private String shardIp;
	private SecurityContext context;

	@BeforeClass
	public static void oneShotBefore() {
		System.setProperty("es.mailspool.count", "1");
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());
		ElasticsearchTestHelper.getInstance().beforeTest();

		Server imapServer = new Server();
		imapServer.ip = new BmConfIni().get("imap-role");
		imapServer.tags = Lists.newArrayList("mail/imap");

		Server pg2 = new Server();
		shardIp = new BmConfIni().get("pg2");
		pg2.ip = shardIp;
		pg2.tags = Lists.newArrayList("mail/shard");

		PopulateHelper.initGlobalVirt(imapServer, pg2);
		PopulateHelper.createTestDomain(domainUid, imapServer);
		PopulateHelper.addUser(userUid, domainUid);

		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();

		JdbcTestHelper.getInstance().initNewServer(pg2.ip);

		context = new SecurityContext("user", userUid, Arrays.<String>asList(), Arrays.<String>asList(), domainUid);

		Sessions.get().put(context.getSessionId(), context);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

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
		assertEquals(version, service.getVersion());

		service.create("new-one", defaultVTodo());

		ContainerChangeset<String> changeset = service.changeset(version);
		assertEquals(1, changeset.created.size());
		assertEquals("new-one", changeset.created.get(0));
		assertTrue(changeset.updated.isEmpty());
		assertTrue(changeset.deleted.isEmpty());

	}

	private void waitTaskEnd(TaskRef taskRef) throws ServerFault {
		ITask task = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITask.class, taskRef.id);
		while (!task.status().state.ended) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}

		TaskStatus status = task.status();
		if (status.state == State.InError) {
			throw new ServerFault("xfer error");
		}
	}

	protected VTodo defaultVTodo() {

		VTodo todo = new VTodo();
		todo.uid = UUID.randomUUID().toString();
		DateTimeZone tz = DateTimeZone.UTC;
		DateTime temp = new DateTime(2024, 12, 28, 0, 0, 0, tz);
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

		todo.attendees = attendees;

		return todo;
	}

}
