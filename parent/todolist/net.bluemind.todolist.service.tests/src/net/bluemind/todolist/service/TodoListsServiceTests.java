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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemContainerValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.icalendar.api.ICalendarElement.Status;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.server.api.Server;
import net.bluemind.tag.api.TagRef;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.ITodoLists;
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.todolist.api.TodoListsVTodoQuery;
import net.bluemind.todolist.api.VTodo;
import net.bluemind.todolist.api.VTodoQuery;
import net.bluemind.user.persistence.UserSubscriptionStore;

public class TodoListsServiceTests {

	private ContainerStore containerStore;
	private SecurityContext securityContext;
	private AclStore aclStore;
	private String userUid;
	private UserSubscriptionStore userSubscriptionStore;
	private ZoneId utcTz = ZoneId.of("UTC");

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		ElasticsearchTestHelper.getInstance().beforeTest();

		containerStore = new ContainerStore(null, JdbcTestHelper.getInstance().getMailboxDataDataSource(),
				securityContext);
		ContainerStore dirContainerStore = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				securityContext);

		aclStore = new AclStore(JdbcTestHelper.getInstance().getMailboxDataDataSource());

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");
		PopulateHelper.initGlobalVirt(esServer);
		PopulateHelper.createTestDomain("bm.lan");
		userUid = PopulateHelper.addUser("test", "bm.lan");

		securityContext = new SecurityContext("testSessionId", userUid, Arrays.<String>asList(),
				Arrays.<String>asList(), "bm.lan");
		Sessions.get().put(securityContext.getSessionId(), securityContext);

		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();

		userSubscriptionStore = new UserSubscriptionStore(securityContext, JdbcTestHelper.getInstance().getDataSource(),
				dirContainerStore.get("bm.lan"));

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void create() throws Exception {

		List<String> subs = userSubscriptionStore.listSubscriptions(userUid, ITodoUids.TYPE);

		String uid = UUID.randomUUID().toString();
		ContainerDescriptor cd = new ContainerDescriptor();
		cd.defaultContainer = false;
		cd.domainUid = "bm.lan";
		cd.name = "new container";
		cd.type = ITodoUids.TYPE;
		cd.uid = uid;
		cd.owner = userUid;

		getTodoListsService(securityContext).create(uid, cd);

		Container c = containerStore.get(uid);
		assertNotNull(c);
		assertEquals(subs.size() + 1, userSubscriptionStore.listSubscriptions(userUid, ITodoUids.TYPE).size());

		List<AccessControlEntry> acls = aclStore.get(c);
		assertEquals(1, acls.size());
	}

	@Test
	public void delete() throws Exception {
		String uid = UUID.randomUUID().toString();
		ContainerDescriptor cd = new ContainerDescriptor();
		cd.defaultContainer = false;
		cd.domainUid = "bm.lan";
		cd.name = "new container";
		cd.type = ITodoUids.TYPE;
		cd.uid = uid;
		cd.owner = "test";

		getTodoListsService(securityContext).create(uid, cd);

		VTodo todo = new VTodo();
		todo.uid = UUID.randomUUID().toString();
		ZonedDateTime temp = ZonedDateTime.of(2015, 2, 13, 0, 0, 0, 0, utcTz);
		todo.dtstart = BmDateTimeWrapper.create(temp, Precision.DateTime);
		todo.due = BmDateTimeWrapper.create(temp.plusMonths(1), Precision.DateTime);
		todo.summary = "test";
		todo.status = Status.NeedsAction;
		todo.organizer = new VTodo.Organizer("test@bm.lan");
		todo.attendees = Collections.emptyList();
		todo.categories = new ArrayList<TagRef>(0);
		getTodoListService(securityContext, uid).create(uid + "-todo", todo);

		getTodoListsService(securityContext).delete(uid);

		assertNull(containerStore.get(uid));
	}

	@Test
	public void testSearch() throws ServerFault {
		VTodo vtodo = defaultVTodo();
		vtodo.summary = "toto";

		VTodoQuery vtodoQuery = VTodoQuery.create("value.summary:toto");

		String containerUid = UUID.randomUUID().toString();
		String uid = UUID.randomUUID().toString();
		ContainerDescriptor cd = new ContainerDescriptor();
		cd.defaultContainer = false;
		cd.domainUid = "bm.lan";
		cd.name = "new container";
		cd.type = ITodoUids.TYPE;
		cd.uid = containerUid;
		cd.owner = userUid;

		getTodoListsService(securityContext).create(containerUid, cd);

		getTodoListService(securityContext, containerUid).create(uid, vtodo);

		TodoListsVTodoQuery query = TodoListsVTodoQuery.create(vtodoQuery, Arrays.asList(containerUid));
		assertEquals(1, getTodoListService(securityContext, containerUid).search(query.vtodoQuery).total);
		List<ItemContainerValue<VTodo>> res = getTodoListsService(securityContext).search(query);

		assertEquals(1, res.size());
		VTodo found = res.get(0).value;
		assertEquals(vtodo.summary, found.summary);

		query = TodoListsVTodoQuery.create(vtodoQuery, userUid);
		assertEquals(1, getTodoListService(securityContext, containerUid).search(query.vtodoQuery).total);

		res = getTodoListsService(securityContext).search(query);

		assertEquals(1, res.size());
		found = res.get(0).value;
		assertEquals(vtodo.summary, found.summary);

	}

	protected VTodo defaultVTodo() {

		VTodo todo = new VTodo();
		todo.uid = UUID.randomUUID().toString();
		ZonedDateTime temp = ZonedDateTime.of(2024, 12, 28, 0, 0, 0, 0, utcTz);
		todo.dtstart = BmDateTimeWrapper.create(temp, Precision.DateTime);
		todo.due = BmDateTimeWrapper.create(temp.plusMonths(1), Precision.DateTime);
		todo.summary = "Test Todo";
		todo.location = "Toulouse";
		todo.description = "Lorem ipsum";
		todo.classification = VTodo.Classification.Private;
		todo.status = Status.NeedsAction;
		todo.priority = 3;

		return todo;
	}

	protected ITodoLists getTodoListsService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(ITodoLists.class);
	}

	protected ITodoList getTodoListService(SecurityContext context, String containerUid) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(ITodoList.class, containerUid);
	}

}
