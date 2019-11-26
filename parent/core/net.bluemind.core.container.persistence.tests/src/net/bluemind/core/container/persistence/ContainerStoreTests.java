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
package net.bluemind.core.container.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.Email;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.User;
import net.bluemind.user.persistence.UserStore;

public class ContainerStoreTests {

	private ContainerStore home;
	private AclStore aclStore;
	private String domainUid;
	private ItemStore userItemStore;
	private UserStore userStore;
	private String testUserUid;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		SecurityContext securityContext = new SecurityContext(null, "test", Arrays.<String>asList("groupOfUsers"),
				Arrays.<String>asList(), "fakeDomain");

		home = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(), securityContext);

		aclStore = new AclStore(JdbcTestHelper.getInstance().getDataSource());

		PopulateHelper.initGlobalVirt();

		domainUid = "bm.lan";
		PopulateHelper.createTestDomain(domainUid);

		userItemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), home.get(domainUid),
				securityContext);

		userStore = new UserStore(JdbcTestHelper.getInstance().getDataSource(), home.get(domainUid));

		testUserUid = createUser();

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCreate() {
		String uid = "test_" + System.nanoTime();
		Container container = Container.create(uid, "test", "name", "me", "fakeDomain", false);
		try {
			Container ret = home.create(container);
			assertEquals(uid, ret.uid);
			assertEquals("test", ret.type);

			assertEquals("name", ret.name);
			assertEquals("me", ret.owner);

			assertEquals("test", ret.createdBy);
			assertEquals("test", ret.updatedBy);
			assertEquals("fakeDomain", ret.domainUid);
			assertFalse(ret.defaultContainer);

		} catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		try {
			home.create(container);
			fail("should not be able to create 2 container with the same id");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testCreateDefault() {
		String uid = "test_" + System.nanoTime();
		Container container = Container.create(uid, "test", "name", "me", true);
		try {
			Container ret = home.create(container);
			assertEquals(uid, ret.uid);
			assertEquals("test", ret.type);

			assertEquals("name", ret.name);
			assertEquals("me", ret.owner);

			assertEquals("test", ret.createdBy);
			assertEquals("test", ret.updatedBy);

			assertTrue(ret.defaultContainer);

		} catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		try {
			home.create(container);
			fail("should not be able to create 2 container with the same id");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testFindLocalizedAccessibles() throws Exception {
		Container mycontacts = Container.create(UUID.randomUUID().toString(), "test", "$$mycontacts$$", "me",
				"fakeDomain", true);
		mycontacts = home.create(mycontacts);
		aclStore.store(mycontacts, Arrays.asList(AccessControlEntry.create("test", Verb.Read)));

		Container directory = Container.create(UUID.randomUUID().toString(), "test", "$$domain.addressbook$$", "me",
				"fakeDomain", true);
		directory = home.create(directory);
		aclStore.store(directory, Arrays.asList(AccessControlEntry.create("test", Verb.Read)));

		Container restaurants = Container.create(UUID.randomUUID().toString(), "test", "Restaurants", "me",
				"fakeDomain", true);
		restaurants = home.create(restaurants);
		aclStore.store(restaurants, Arrays.asList(AccessControlEntry.create("test", Verb.Read)));

		ContainerQuery query = new ContainerQuery();
		query.type = "test";
		query.name = "My";
		List<Container> resp = home.findAccessiblesByType(query);
		assertEquals(1, resp.size());

		query.name = "dir";
		resp = home.findAccessiblesByType(query);
		assertEquals(1, resp.size());

		// diREctory and REstaurants
		query.name = "re";
		resp = home.findAccessiblesByType(query);
		assertEquals(2, resp.size());

		query.name = "res";
		resp = home.findAccessiblesByType(query);
		assertEquals(1, resp.size());

		query.name = "Mes";
		resp = home.findAccessiblesByType(query);
		assertEquals(0, resp.size());

		query.name = "Annuaire";
		resp = home.findAccessiblesByType(query);
		assertEquals(0, resp.size());

		// FR securityContext
		SecurityContext securityContext = new SecurityContext(null, "test", Arrays.<String>asList("groupOfUsers"),
				Arrays.<String>asList(), Collections.emptyMap(), "fakeDomain", "fr", "testFindLocalizedAccessibles");
		ContainerStore store = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(), securityContext);

		query = new ContainerQuery();
		query.type = "test";
		query.name = "Mes";

		resp = store.findAccessiblesByType(query);
		assertEquals(1, resp.size());

		query.name = "Annuaire";
		resp = store.findAccessiblesByType(query);
		assertEquals(1, resp.size());

		query.name = "res";
		resp = home.findAccessiblesByType(query);
		assertEquals(1, resp.size());

		// annuaiRE and REstaurants
		query.name = "re";
		resp = home.findAccessiblesByType(query);
		assertEquals(2, resp.size());

		query.name = "My";
		resp = store.findAccessiblesByType(query);
		assertEquals(0, resp.size());

		query.name = "dir";
		resp = store.findAccessiblesByType(query);
		assertEquals(0, resp.size());

	}

	@Test
	public void testFindAccessibles() throws Exception {
		String uid1 = "test_" + System.nanoTime();
		Container container1 = Container.create(uid1, "test", "Name", "me", "fakeDomain", true);
		String uid2 = uid1 + "1";
		Container container2 = Container.create(uid2, "test", "blowUp", "me", "fakeDomain", true);

		String uid3 = uid2 + "1";
		Container container3 = Container.create(uid3, "test", "flameDown", "me", "fakeDomain", true);

		String uid4 = uid3 + "1";
		Container container4 = Container.create(uid4, "test", "nem", "me", "fakeDomain", true);

		String uid5 = uid4 + "1";
		Container container5 = Container.create(uid5, "calendar", "a calendar", "me", "fakeDomain", true);

		String uid6 = uid5 + "1";
		Container container6 = Container.create(uid6, "test", "owme", "test", "fakeDomain", true);

		String uid7 = uid6 + "1";
		Container container7 = Container.create(uid7, "test", "owme", "me", "fakeDomain", true);

		container1 = home.create(container1);
		container2 = home.create(container2);
		container3 = home.create(container3);
		container4 = home.create(container4);
		container5 = home.create(container5);
		container6 = home.create(container6);
		container7 = home.create(container7);

		aclStore.store(container1, Arrays.asList(AccessControlEntry.create("test", Verb.Read)));

		aclStore.store(container2, Arrays.asList(AccessControlEntry.create("groupOfUsers", Verb.Read)));

		aclStore.store(container3, Arrays.asList(AccessControlEntry.create("notAccessibleGroup", Verb.Read)));

		aclStore.store(container4, Arrays.asList(AccessControlEntry.create("test", Verb.Read),
				AccessControlEntry.create("groupOfUsers", Verb.Read)));

		aclStore.store(container5, Arrays.asList(AccessControlEntry.create("test", Verb.Write)));
		aclStore.store(container7, Arrays.asList(AccessControlEntry.create("fakeDomain", Verb.Read)));

		ContainerQuery query = new ContainerQuery();
		query.type = "test";

		List<Container> resp = home.findAccessiblesByType(query);
		assertEquals(5, resp.size());
		Set<String> uids = new HashSet<>();
		uids.add(resp.get(0).uid);
		uids.add(resp.get(1).uid);
		uids.add(resp.get(2).uid);
		uids.add(resp.get(3).uid);
		uids.add(resp.get(4).uid);
		assertTrue(uids.contains(uid1));
		assertTrue(uids.contains(uid2));
		assertFalse(uids.contains(uid3));
		assertTrue(uids.contains(uid4));

		// subject == owner
		assertTrue(uids.contains(uid6));
		// public
		assertTrue(uids.contains(uid7));
		query.name = "nem";
		resp = home.findAccessiblesByType(query);
		assertEquals(1, resp.size());
		assertEquals(uid4, resp.get(0).uid);

		query.name = "nam";
		resp = home.findAccessiblesByType(query);
		assertEquals(1, resp.size());
		assertEquals(uid1, resp.get(0).uid);

		query.name = "blowu";
		resp = home.findAccessiblesByType(query);
		assertEquals(1, resp.size());
		assertEquals(uid2, resp.get(0).uid);

		query.name = "n"; // n%
		resp = home.findAccessiblesByType(query);
		assertEquals(2, resp.size());

		query = new ContainerQuery();
		query.type = "calendar";
		resp = home.findAccessiblesByType(query);
		assertEquals(1, resp.size());
		assertEquals(uid5, resp.get(0).uid);

		query = new ContainerQuery();
		query.type = "calendar";
		query.name = "z";
		resp = home.findAccessiblesByType(query);
		assertEquals(0, resp.size());

		query = new ContainerQuery();
		query.type = "calendar";
		query.name = "a";
		resp = home.findAccessiblesByType(query);
		assertEquals(1, resp.size());
		assertEquals(uid5, resp.get(0).uid);

		query = new ContainerQuery();
		query.type = "calendar";
		query.name = "a";
		query.verb = new ArrayList<Verb>();
		query.verb.add(Verb.Read);
		resp = home.findAccessiblesByType(query);
		assertEquals(0, resp.size());

		query = new ContainerQuery();
		query.type = "calendar";
		query.name = "a";
		query.verb = new ArrayList<Verb>();
		query.verb.add(Verb.Write);
		resp = home.findAccessiblesByType(query);
		assertEquals(1, resp.size());
		assertEquals(uid5, resp.get(0).uid);

		query = new ContainerQuery();
		query.type = "calendar";
		query.verb = new ArrayList<Verb>();
		query.verb.add(Verb.Write);
		resp = home.findAccessiblesByType(query);
		assertEquals(1, resp.size());
		assertEquals(uid5, resp.get(0).uid);

		query = new ContainerQuery();
		query.verb = new ArrayList<Verb>();
		query.verb.add(Verb.Write);
		resp = home.findAccessiblesByType(query);
		assertEquals(2, resp.size());
		uids = new HashSet<>();
		uids.add(resp.get(0).uid);
		uids.add(resp.get(1).uid);
		assertTrue(uids.contains(uid5));
		assertTrue(uids.contains(uid6));
	}

	@Test
	public void testUpdateName() throws SQLException {
		String uid = "test_" + System.nanoTime();
		Container container = Container.create(uid, "test", "name", "me", true);
		home.create(container);

		try {
			home.updateName(uid, "name2");
		} catch (SQLException e) {
			e.printStackTrace();
			fail();
		}

		Container ret = home.get(uid);
		assertEquals("name2", ret.name);
	}

	@Test
	public void testDelete() throws SQLException {
		String uid = "test_" + System.nanoTime();
		Container container = Container.create(uid, "test", "name", "me", true);
		container = home.create(container);

		Container created = home.get(uid);
		assertNotNull(created);

		home.delete(uid);

		created = home.get(uid);
		assertNull(created);
	}

	private String createUser() throws SQLException {
		User u = new User();
		u.login = "test" + System.nanoTime();
		u.password = "password";
		u.routing = Routing.external;
		u.archived = false;
		u.hidden = false;
		u.system = false;
		Email e = new Email();
		e.address = u.login + "@blue-mind.loc";
		u.emails = Arrays.asList(e);
		u.dataLocation = null;

		userItemStore.create(Item.create(u.login, null));
		Item item = userItemStore.get(u.login);
		userStore.create(item, u);

		return u.login;
	}

}
