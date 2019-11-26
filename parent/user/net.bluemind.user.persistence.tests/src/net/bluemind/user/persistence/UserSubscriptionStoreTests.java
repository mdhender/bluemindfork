/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.user.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.User;

public class UserSubscriptionStoreTests {

	private String domainUid;
	private ItemStore userItemStore;
	private UserStore userStore;
	private String testUserUid;
	private Item testUser;
	private UserSubscriptionStore store;
	private SecurityContext securityContext;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		securityContext = new SecurityContext(null, "test", Arrays.<String>asList("groupOfUsers"),
				Arrays.<String>asList(), "fakeDomain");

		ContainerStore containerStore = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				securityContext);

		PopulateHelper.initGlobalVirt();

		domainUid = "bm.lan";
		PopulateHelper.createTestDomain(domainUid);

		userItemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), containerStore.get(domainUid),
				securityContext);

		userStore = new UserStore(JdbcTestHelper.getInstance().getDataSource(), containerStore.get(domainUid));

		testUserUid = "test";

		testUser = createUser();

		store = new UserSubscriptionStore(SecurityContext.SYSTEM, JdbcTestHelper.getInstance().getDataSource(),
				containerStore.get(domainUid));

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	private Item createUser() throws SQLException {
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

		userItemStore.create(Item.create(testUserUid, null));
		Item item = userItemStore.get(testUserUid);
		userStore.create(item, u);

		return item;
	}

	@Test
	public void testSyncSubscription() throws SQLException {
		String uid = "test_" + System.nanoTime();
		Container container = Container.create(uid, "test", "name", "me", true);

		ContainerStore containerStore = new ContainerStore(JdbcTestHelper.getInstance().getMailboxDataDataSource(),
				securityContext);

		container = containerStore.create(container);
		store.subscribe(testUser.uid, container);

		container = containerStore.get(uid);

		assertTrue(store.isSyncAllowed(testUserUid, container));

		store.allowSynchronization(testUserUid, container, false);

		assertFalse(store.isSyncAllowed(testUserUid, container));
	}

	@Test
	public void testSubscription() throws SQLException {
		String uid = "test_" + System.nanoTime();
		Container container = Container.create(uid, "testType", "name", "me", true);

		ContainerStore containerStore = new ContainerStore(JdbcTestHelper.getInstance().getMailboxDataDataSource(),
				securityContext);

		container = containerStore.create(container);

		List<String> subs = store.listSubscriptions(testUser.uid, "testType");
		assertEquals(0, subs.size());
		store.subscribe(testUser.uid, container);

		try {
			// subscibe twice
			store.subscribe(testUser.uid, container);
			fail();
		} catch (Exception e) {
			e.printStackTrace();
		}

		subs = store.listSubscriptions(testUser.uid, "testType");
		assertEquals(1, subs.size());
		subs = store.listSubscriptions(testUser.uid, null);
		assertEquals(1, subs.size());

		store.unsubscribe(testUser.uid, container);
		subs = store.listSubscriptions(testUser.uid, "testType");
		assertEquals(0, subs.size());
	}

}
