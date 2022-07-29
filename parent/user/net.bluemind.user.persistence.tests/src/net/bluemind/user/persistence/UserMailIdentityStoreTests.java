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
package net.bluemind.user.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.identity.api.IdentityDescription;
import net.bluemind.mailbox.identity.api.SignatureFormat;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.UserMailIdentity;

public class UserMailIdentityStoreTests {

	private UserMailIdentityStore store;
	private String domainUid;
	private Item userItem;
	private String userUid;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();

		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		PopulateHelper.initGlobalVirt();

		domainUid = "test" + System.nanoTime() + ".fr";
		PopulateHelper.createTestDomain(domainUid);

		userUid = PopulateHelper.addUser("test", domainUid);

		ContainerStore containerStore = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				securityContext);

		userItem = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), containerStore.get(domainUid),
				SecurityContext.SYSTEM).get(userUid);

		store = new UserMailIdentityStore(JdbcTestHelper.getInstance().getDataSource(), containerStore.get(domainUid));

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCreateAndGet() throws SQLException {
		store.create(userItem, "work", defaultIdentifier("test", "test@" + domainUid, "test"));

		UserMailIdentity res = store.get(userItem, "work");
		assertNotNull(res);
		assertIdentityEquals(defaultIdentifier("test", "test@" + domainUid, "test"), res);
	}

	@Test
	public void testSetDefault() throws SQLException {
		store.create(userItem, "work", defaultIdentifier("work", "test@" + domainUid, "test"));
		store.create(userItem, "home", defaultIdentifier("home", "test@" + domainUid, "test"));
		store.create(userItem, "shop", defaultIdentifier("shop", "test@" + domainUid, "test"));

		store.setDefault(userItem, "shop");
		List<IdentityDescription> descrs = store.getDescriptions(userItem);
		assertEquals(4, descrs.size());
		assertEquals("shop", descrs.get(0).id);
		assertTrue(descrs.get(0).isDefault);

		store.setDefault(userItem, "home");

		descrs = store.getDescriptions(userItem);
		assertEquals(4, descrs.size());
		assertEquals("home", descrs.get(0).id);
		assertTrue(descrs.get(0).isDefault);

		UserMailIdentity identity = store.get(userItem, "home");
		assertTrue(identity.isDefault);

	}

	@Test
	public void testUpdate() throws SQLException {
		store.create(userItem, "work", defaultIdentifier("test", "test@" + domainUid, "test"));

		UserMailIdentity identity = defaultIdentifier("test", "test@" + domainUid, "test");
		identity.displayname = "gg";
		store.update(userItem, "work", identity);
		UserMailIdentity res = store.get(userItem, "work");
		assertIdentityEquals(identity, res);

	}

	@Test
	public void testDelete() throws SQLException {
		store.create(userItem, "work", defaultIdentifier("test", "test@" + domainUid, "test"));

		store.delete(userItem, "work");
		assertNull(store.get(userItem, "work"));
	}

	@Test
	public void testDeleteAll() throws SQLException {
		store.create(userItem, "work", defaultIdentifier("test", "test@" + domainUid, "test"));
		store.create(userItem, "work2", defaultIdentifier("test", "test@" + domainUid, "test"));

		store.delete(userItem);
		assertNull(store.get(userItem, "work"));
		assertNull(store.get(userItem, "work2"));
	}

	@Test
	public void testGetDescriptions() throws SQLException {

		store.create(userItem, "work", defaultIdentifier("test", "test@" + domainUid, "test"));
		store.create(userItem, "perso", defaultIdentifier("test2", "test2@" + domainUid, "test2"));
		store.create(userItem, "alien", defaultIdentifier("alien", "alien@" + domainUid, "alien_mbox"));

		store.setDefault(userItem, "alien");

		List<IdentityDescription> res = store.getDescriptions(userItem);
		assertEquals(4, res.size()); // 3 created + default identity

		boolean isDefault = false;
		for (IdentityDescription id : res) {
			if ("alien".equals(id.name)) {
				isDefault = id.isDefault;
			}
		}

		assertTrue(isDefault);

	}

	@Test
	public void testDeleteMailboxIdentities() throws Exception {

		String userUid2 = PopulateHelper.addUser("test2", domainUid);

		ContainerStore containerStore = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM);

		Item userItem2 = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), containerStore.get(domainUid),
				SecurityContext.SYSTEM).get(userUid2);

		store.create(userItem, "work", defaultIdentifier("test", "test@" + domainUid, "test"));
		store.create(userItem, "perso", defaultIdentifier("test2", "test2@" + domainUid, "test2"));
		store.create(userItem2, "perso", defaultIdentifier("test3", "test3@" + domainUid, "test2"));
		store.create(userItem2, "work", defaultIdentifier("test", "test@" + domainUid, "test"));
		store.create(userItem2, "alien", defaultIdentifier("alien", "alien@" + domainUid, "alien_mbox"));
		store.deleteMailboxIdentities("test2");
		assertNull(store.get(userItem, "perso"));
		assertNull(store.get(userItem2, "perso"));

		store.deleteMailboxIdentities(userItem, "test");
		assertNull(store.get(userItem, "work"));
		assertNotNull(store.get(userItem2, "work"));

	}

	private void assertIdentityEquals(UserMailIdentity expected, UserMailIdentity value) {
		assertEquals(expected.name, value.name);
		assertEquals(expected.displayname, value.displayname);
		assertEquals(expected.email, value.email);
		assertEquals(expected.format, value.format);
		assertEquals(expected.sentFolder, value.sentFolder);
		assertEquals(expected.mailboxUid, value.mailboxUid);
	}

	private UserMailIdentity defaultIdentifier(String name, String email, String mboxUid) {
		UserMailIdentity ret = new UserMailIdentity();
		ret.displayname = "test displayname";
		ret.name = name;
		ret.email = email;
		ret.sentFolder = "Sent";
		ret.signature = "Check that";
		ret.format = SignatureFormat.PLAIN;
		ret.mailboxUid = mboxUid;
		return ret;
	}

}
