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
package net.bluemind.todolist.usertodolist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.ITodoLists;
import net.bluemind.todolist.api.VTodo;
import net.bluemind.user.api.User;

public class UserTodoListHookTests {

	private UserTodoListHook hook;

	protected BmTestContext bmContext;

	private String domainUid = "fakeDomainUid";
	private ItemValue<User> userItem;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		domainUid = "bm.lan";

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		PopulateHelper.initGlobalVirt(esServer);

		PopulateHelper.createTestDomain(domainUid, esServer);

		PopulateHelper.addUser("admin", domainUid);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
		ElasticsearchTestHelper.getInstance().beforeTest();

		bmContext = BmTestContext.contextWithSession("sid", "admin", domainUid);
		hook = new UserTodoListHook();

		String uid = PopulateHelper.addUser(UUID.randomUUID().toString(), domainUid);
		User user = new User();
		user.login = uid;
		Item i = Item.create(uid, null);
		userItem = ItemValue.<User>create(i, user);

	}

	@Test
	public void testOnCreated() throws SQLException {

		hook.onUserCreated(bmContext, domainUid, userItem);

		String containerId = UserTodoListHook.getUserTodoListId(userItem);
		ContainerStore cs = new ContainerStore(null, DataSourceRouter.get(bmContext, containerId),
				bmContext.getSecurityContext());
		Container container = cs.get(containerId);
		assertNotNull(container);
		assertEquals("$$mytasks$$", container.name);
	}

	@Test
	public void testOnUpdate() throws SQLException {

		hook.onUserCreated(bmContext, domainUid, userItem);
		userItem.displayName = "Updated test";

		hook.onUserUpdated(bmContext, domainUid, userItem, userItem);

		String containerId = UserTodoListHook.getUserTodoListId(userItem);
		ContainerStore cs = new ContainerStore(null, DataSourceRouter.get(bmContext, containerId),
				bmContext.getSecurityContext());
		Container container = cs.get(containerId);
		assertNotNull(container);
		assertEquals("$$mytasks$$", container.name);

	}

	@Test
	public void testOnDelete() throws SQLException {

		hook.onUserCreated(bmContext, domainUid, userItem);

		ITodoLists td = bmContext.provider().instance(ITodoLists.class);
		ContainerDescriptor descriptor = new ContainerDescriptor();
		descriptor.domainUid = domainUid;
		descriptor.name = "ab1";
		descriptor.owner = userItem.uid;
		td.create("td1", descriptor);

		ContainerStore cs = new ContainerStore(null, DataSourceRouter.get(bmContext, "td1"),
				bmContext.getSecurityContext());
		Container container = cs.get("td1");
		assertNotNull(container);

		ITodoList td1 = bmContext.provider().instance(ITodoList.class, "td1");
		VTodo todo1 = new VTodo();
		todo1.dtstart = BmDateTimeWrapper.fromTimestamp(new Date().getTime());
		td1.create("td1-td", todo1);

		hook.beforeDelete(bmContext, domainUid, userItem.uid, userItem.value);

		cs = new ContainerStore(null, DataSourceRouter.get(bmContext, UserTodoListHook.getUserTodoListId(userItem)),
				bmContext.getSecurityContext());

		container = cs.get(UserTodoListHook.getUserTodoListId(userItem));
		assertNull(container);

		cs = new ContainerStore(null, DataSourceRouter.get(bmContext, "td1"), bmContext.getSecurityContext());
		container = cs.get("td1");
		assertNull(container);

	}

}
