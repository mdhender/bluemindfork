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
package net.bluemind.directory.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirectoryContainerType;
import net.bluemind.directory.api.OrgUnit;
import net.bluemind.directory.api.OrgUnitPath;

public class OrgUnitStoreTests {

	private ItemStore itemStore;
	private OrgUnitStore ouStore;
	private DirEntryStore dirStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
		JdbcTestHelper.getInstance().beforeTest();

		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerHome = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				securityContext);
		String containerId = "test_" + System.nanoTime();
		Container container = Container.create(containerId, DirectoryContainerType.TYPE, "test", "me", true);
		container = containerHome.create(container);

		assertNotNull(container);

		itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), container, securityContext);

		ouStore = new OrgUnitStore(JdbcTestHelper.getInstance().getDataSource(), container);
		dirStore = new DirEntryStore(JdbcTestHelper.getInstance().getDataSource(), container);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCreate() throws Exception {
		OrgUnit ou = new OrgUnit();
		ou.name = "checkthat";
		ou.parentUid = null;

		Item item = itemStore.create(Item.create("test1", null));
		ouStore.create(item, ou);

		// with parent
		item = itemStore.create(Item.create("test2", null));
		ou = new OrgUnit();
		ou.name = "checkthatChild";
		ou.parentUid = "test1";
		ouStore.create(item, ou);

		// with bad parent
		item = itemStore.create(Item.create("test3", null));
		ou = new OrgUnit();
		ou.name = "checkthatChild";
		ou.parentUid = "testBadParent";
		try {
			ouStore.create(item, ou);
			fail("should fail");
		} catch (ServerFault e) {

		}

		// with parent
		item = itemStore.create(Item.create("test4", null));
		ou = new OrgUnit();
		ou.name = "checkthatChild2";
		ou.parentUid = "test2";
		ouStore.create(item, ou);

	}

	@Test
	public void testSearch() throws Exception {
		OrgUnit ou = new OrgUnit();
		ou.name = "checkthat";
		ou.parentUid = null;

		Item item = itemStore.create(Item.create("test1", null));
		ouStore.create(item, ou);

		// with parent
		item = itemStore.create(Item.create("test2", null));
		ou = new OrgUnit();
		ou.name = "CHECKTHATCHILD";
		ou.parentUid = "test1";
		ouStore.create(item, ou);
		List<String> paths = ouStore.search("CheckThatChild");
		assertEquals(paths.size(), 1);
		paths = ouStore.search("CheckThatC");
		assertEquals(paths.size(), 1);
		paths = ouStore.search("CheckThatChilde");
		assertEquals(paths.size(), 0);
		paths = ouStore.search("CheckThat");
		assertEquals(paths.size(), 2);
		paths = ouStore.search("That");
		assertEquals(paths.size(), 2);
	}

	@Test
	public void testSearchLimitToSomeOu() throws Exception {
		OrgUnit ou = new OrgUnit();
		ou.name = "checkthat";
		ou.parentUid = null;

		Item item = itemStore.create(Item.create("test1", null));
		ouStore.create(item, ou);

		ou = new OrgUnit();
		ou.name = "CheckFthat";
		ou.parentUid = null;

		item = itemStore.create(Item.create("fthat", null));
		ouStore.create(item, ou);

		// with parent
		item = itemStore.create(Item.create("test2", null));
		ou = new OrgUnit();
		ou.name = "CHECKTHATCHILD";
		ou.parentUid = "test1";
		ouStore.create(item, ou);

		item = itemStore.create(Item.create("test22", null));
		ou = new OrgUnit();
		ou.name = "CCD2";
		ou.parentUid = "test1";
		ouStore.create(item, ou);

		item = itemStore.create(Item.create("test222", null));
		ou = new OrgUnit();
		ou.name = "CHECKTHATCHILD2";
		ou.parentUid = "test2";
		ouStore.create(item, ou);

		item = itemStore.create(Item.create("fthatC", null));
		ou = new OrgUnit();
		ou.name = "CHECKTHATnot";
		ou.parentUid = "fthat";
		ouStore.create(item, ou);

		List<String> paths = ouStore.search("CheckThatChild", Arrays.asList("test1", "fthat"));
		assertEquals(2, paths.size());

		paths = ouStore.search("CheckThatChild", Arrays.asList("test222"));
		assertEquals(1, paths.size());

		paths = ouStore.search("CHECKTHAT", Arrays.asList("fthat"));
		assertEquals(1, paths.size());

		paths = ouStore.search("CheckF", Arrays.asList("fthat"));
		assertEquals(2, paths.size());

		paths = ouStore.search("Check", Arrays.asList("test1", "fthat"));
		assertEquals(6, paths.size());
		paths = ouStore.search("Check", Arrays.asList("test2", "fthat"));
		assertEquals(4, paths.size());
	}

	@Test
	public void testUpdate() throws Exception {
		OrgUnit ou = new OrgUnit();
		ou.name = "checkthat";
		ou.parentUid = null;
		Item item = itemStore.create(Item.create("test1", null));
		ouStore.create(item, ou);

		ou.name = "updated";
		ouStore.update(item, ou);
		ou = ouStore.get(item);
		assertNotNull(ou);
		assertEquals("updated", ou.name);
	}

	@Test
	public void testGet() throws Exception {
		OrgUnit ou = new OrgUnit();
		ou.name = "checkthat";
		ou.parentUid = null;
		Item item = itemStore.create(Item.create("test1", null));
		ouStore.create(item, ou);

		ou = ouStore.get(item);
		assertNotNull(ou);
		assertEquals("checkthat", ou.name);

		// with parent
		item = itemStore.create(Item.create("test2", null));
		ou = new OrgUnit();
		ou.name = "checkthatChild";
		ou.parentUid = "test1";
		ouStore.create(item, ou);
		ou = ouStore.get(item);
		assertNotNull(ou);
		assertEquals("checkthatChild", ou.name);
		assertEquals("test1", ou.parentUid);

	}

	@Test
	public void testGetChildren() throws Exception {
		OrgUnit ou = new OrgUnit();
		ou.name = "checkthat";
		ou.parentUid = null;
		Item parent = itemStore.create(Item.create("test1", null));
		ouStore.create(parent, ou);

		// with parent
		Item item = itemStore.create(Item.create("test2", null));
		ou = new OrgUnit();
		ou.name = "checkthatChild1";
		ou.parentUid = "test1";
		ouStore.create(item, ou);
		// with parent
		item = itemStore.create(Item.create("test3", null));
		ou = new OrgUnit();
		ou.name = "checkthatChild2";
		ou.parentUid = "test1";
		ouStore.create(item, ou);

		List<String> children = ouStore.getChildren(parent);
		assertTrue(children.contains("test2"));
		assertTrue(children.contains("test3"));
	}

	@Test
	public void testGetPath() throws Exception {
		OrgUnit ou = new OrgUnit();
		ou.name = "checkthat";
		ou.parentUid = null;
		Item item = itemStore.create(Item.create("test1", null));
		ouStore.create(item, ou);

		item = itemStore.create(Item.create("test2", null));
		ou = new OrgUnit();
		ou.name = "checkthatChild";
		ou.parentUid = "test1";
		ouStore.create(item, ou);

		item = itemStore.create(Item.create("test3", null));
		ou = new OrgUnit();
		ou.name = "checkthatChildChild";
		ou.parentUid = "test2";
		ouStore.create(item, ou);

		OrgUnitPath path = ouStore.getPath(item);
		assertNotNull(path);
		assertEquals("test3", path.uid);
		assertEquals("checkthatChildChild", path.name);
		assertNotNull(path.parent);
		assertEquals("test2", path.parent.uid);
		assertEquals("checkthatChild", path.parent.name);
		assertNotNull(path.parent.parent);
		assertEquals("test1", path.parent.parent.uid);
		assertEquals("checkthat", path.parent.parent.name);

		path = ouStore.getPath(itemStore.get("test2"));
		assertNotNull(path);
		assertEquals("checkthatChild", path.name);
		assertNotNull(path.parent);
		assertEquals("test1", path.parent.uid);
		assertEquals("checkthat", path.parent.name);

		path = ouStore.getPathByUid("test2");
		assertNotNull(path);
		assertEquals("checkthatChild", path.name);
		assertNotNull(path.parent);
		assertEquals("test1", path.parent.uid);
		assertEquals("checkthat", path.parent.name);

	}

	@Test
	public void testSetGetAdminRoles() throws Exception {
		OrgUnit ou = new OrgUnit();
		ou.name = "checkthat";
		ou.parentUid = null;
		Item item = itemStore.create(Item.create("test1", null));
		ouStore.create(item, ou);

		Item adminItem = itemStore.create(Item.create("adminTest1", null));

		ouStore.setAdminRoles(item, adminItem, new HashSet<>(Arrays.asList("role1", "role2", "role3")));
		assertEquals(new HashSet<>(Arrays.asList("role1", "role2", "role3")),
				ouStore.getAdminRoles(item, Arrays.asList(adminItem)));

		ouStore.setAdminRoles(item, adminItem, new HashSet<>());
		assertEquals(Collections.emptySet(), ouStore.getAdminRoles(item, Arrays.asList(adminItem)));
	}

	@Test
	public void testGetAdministrators() throws Exception {
		OrgUnit ou = new OrgUnit();
		ou.name = "checkthat";
		ou.parentUid = null;
		Item item = itemStore.create(Item.create("test1", null));
		ouStore.create(item, ou);

		Item adminItem = itemStore.create(Item.create("adminTest1", null));

		Item admin2Item = itemStore.create(Item.create("adminTest2", null));

		ouStore.setAdminRoles(item, adminItem, new HashSet<>(Arrays.asList("role1", "role2", "role3")));
		ouStore.setAdminRoles(item, admin2Item, new HashSet<>(Arrays.asList("role1", "role2", "role3")));

		Set<String> res = ouStore.getAdministrators(item);
		assertEquals(new HashSet<>(Arrays.asList(adminItem.uid, admin2Item.uid)), res);
		ouStore.setAdminRoles(item, adminItem, new HashSet<>());
		res = ouStore.getAdministrators(item);
		assertEquals(new HashSet<>(Arrays.asList(admin2Item.uid)), res);
	}

	@Test
	public void testListByAdministrator() throws Exception {
		OrgUnit ou = new OrgUnit();
		ou.name = "checkthat";
		ou.parentUid = null;
		Item item = itemStore.create(Item.create("test1", null));
		ouStore.create(item, ou);

		OrgUnit ou2 = new OrgUnit();
		ou2.name = "checkthat";
		ou2.parentUid = null;
		Item item2 = itemStore.create(Item.create("test2", null));
		ouStore.create(item2, ou2);

		Item adminItem = itemStore.create(Item.create("adminTest1", null));
		ouStore.setAdminRoles(item, adminItem, new HashSet<>(Arrays.asList("role1", "role2", "role3")));
		List<String> res = ouStore.listByAdministrator(Arrays.asList(adminItem));
		assertEquals(1, res.size());
		assertEquals("test1", res.get(0));

		ouStore.setAdminRoles(item2, adminItem, new HashSet<>(Arrays.asList("role1", "role2", "role3")));
		res = ouStore.listByAdministrator(Arrays.asList(adminItem));
		assertEquals(2, res.size());

	}

	@Test
	public void testHasChilds() throws Exception {
		OrgUnit ou = new OrgUnit();
		ou.name = "checkthat";
		ou.parentUid = null;
		Item item = itemStore.create(Item.create("test1", null));
		ouStore.create(item, ou);

		OrgUnit ou2 = new OrgUnit();
		ou2.name = "checkthat2";
		ou2.parentUid = "test1";
		Item item2 = itemStore.create(Item.create("test2", null));
		ouStore.create(item2, ou2);

		assertTrue(ouStore.hasChilds(item));
		assertFalse(ouStore.hasChilds(item2));
	}

	@Test
	public void testHasMembers() throws Exception {
		OrgUnit ou = new OrgUnit();
		ou.name = "checkthat";
		ou.parentUid = null;
		Item item = itemStore.create(Item.create("test1", null));
		ouStore.create(item, ou);

		Item adminItem = itemStore.create(Item.create("adminTest1", null));
		dirStore.create(adminItem, DirEntry.create("test1", null, Kind.USER, "adminTest1", "adminTest1", "admin@bm.lan",
				false, false, false));

		OrgUnit ou2 = new OrgUnit();
		ou2.name = "checkthat2";
		ou2.parentUid = "test1";
		Item item2 = itemStore.create(Item.create("test2", null));
		ouStore.create(item2, ou2);

		assertFalse(ouStore.hasMembers(item2));
		assertTrue(ouStore.hasMembers(item));
	}

	@Test
	public void testHasAdministrator() throws Exception {
		OrgUnit ou = new OrgUnit();
		ou.name = "checkthat";
		ou.parentUid = null;
		Item item = itemStore.create(Item.create("test1", null));
		ouStore.create(item, ou);

		Item adminItem = itemStore.create(Item.create("adminTest1", null));

		Item admin2Item = itemStore.create(Item.create("adminTest2", null));

		ouStore.setAdminRoles(item, adminItem, new HashSet<>(Arrays.asList("role1", "role2", "role3")));
		ouStore.setAdminRoles(item, admin2Item, new HashSet<>(Arrays.asList("role1", "role2", "role3")));

		OrgUnit ou2 = new OrgUnit();
		ou2.name = "checkthat2";
		ou2.parentUid = "test1";
		Item item2 = itemStore.create(Item.create("test2", null));
		ouStore.create(item2, ou2);

		assertTrue(ouStore.hasAdministrator(item));
		assertFalse(ouStore.hasAdministrator(item2));
	}

	@Test
	public void testRemovingAdministrator() throws Exception {
		OrgUnit ou = new OrgUnit();
		ou.name = "checkthat";
		ou.parentUid = null;
		Item item = itemStore.create(Item.create("test1", null));
		ouStore.create(item, ou);

		Item adminItem = itemStore.create(Item.create("adminTest1", null));
		Item admin2Item = itemStore.create(Item.create("adminTest2", null));

		ouStore.setAdminRoles(item, adminItem, new HashSet<>(Arrays.asList("role1", "role2", "role3")));
		ouStore.setAdminRoles(item, admin2Item, new HashSet<>(Arrays.asList("role1", "role2", "role3")));

		assertEquals(ouStore.getAdministrators(item).size(), 2);

		ouStore.removeAdministrator("adminTest1");

		assertEquals(ouStore.getAdministrators(item).size(), 1);
		assertEquals(ouStore.getAdministrators(item).iterator().next(), "adminTest2");
	}

}
