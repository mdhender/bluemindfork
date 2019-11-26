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
package net.bluemind.role.persistence.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.role.persistence.RoleStore;

public class RoleStoreTests {

	private ItemStore itemStore;
	private String uid;
	private RoleStore roleStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		
		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerStore = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				securityContext);

		String containerId = "test_" + System.nanoTime() + ".fr";
		Container usersContainer = Container.create(containerId, "user", containerId, "me", true);
		usersContainer = containerStore.create(usersContainer);

		this.uid = "test_" + System.nanoTime();

		assertNotNull(usersContainer);

		itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), usersContainer, securityContext);

		roleStore = new RoleStore(JdbcTestHelper.getInstance().getDataSource(), usersContainer);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testSchemaIsWellRegistered() {
		assertNotNull(JdbcTestHelper.getInstance().getDbSchemaService().getSchemaDescriptor("role-schema"));
	}

	@Test
	public void testSetAndGet() throws Exception {
		itemStore.create(Item.create(uid, null));
		Item item = itemStore.get(uid);

		roleStore.set(item, new HashSet<String>(Arrays.asList("test1", "test2", "test3")));
		Set<String> res = roleStore.get(item);

		assertEquals(new HashSet<String>(Arrays.asList("test1", "test2", "test3")), res);
	}

	@Test
	public void testSetAndGetEmptySet() throws Exception {
		itemStore.create(Item.create(uid, null));
		Item item = itemStore.get(uid);

		roleStore.set(item, new HashSet<String>());
		Set<String> res = roleStore.get(item);

		assertTrue(res.isEmpty());
	}

	@Test
	public void testFindItemsWithRoles() throws Exception {
		String uid1 = uid.concat("____1");
		String uid2 = uid.concat("____2");
		itemStore.create(Item.create(uid1, null));
		itemStore.create(Item.create(uid2, null));
		Item item = itemStore.get(uid1);
		Item item2 = itemStore.get(uid2);

		roleStore.set(item, new HashSet<String>(Arrays.asList("test1", "test2", "test3")));
		roleStore.set(item2, new HashSet<String>(Arrays.asList("test1", "test3")));
		Set<String> res1 = roleStore.getItemsWithRoles(Arrays.asList("test1"));
		Set<String> res2 = roleStore.getItemsWithRoles(Arrays.asList("test1", "test2"));
		Set<String> res3 = roleStore.getItemsWithRoles(Arrays.asList("test1", "test3"));
		Set<String> res4 = roleStore.getItemsWithRoles(Arrays.asList("test2"));

		assertEquals(2, res1.size());
		assertEquals(2, res2.size());
		assertEquals(2, res3.size());
		assertEquals(1, res4.size());
	}

}
