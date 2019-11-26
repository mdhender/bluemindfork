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
package net.bluemind.mailbox.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirectoryContainerType;
import net.bluemind.directory.persistence.DirEntryStore;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;

public class MailboxStoreTests {
	private static Logger logger = LoggerFactory.getLogger(MailboxStoreTests.class);
	private MailboxStore mailboxStore;
	private ItemStore itemStore;
	private String uid;
	private ItemValue<Server> serverValue;
	private String containerId;
	private DirEntryStore dirEntryStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerStore = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				securityContext);
		containerId = "test_" + System.nanoTime() + ".fr";
		Container mailshare = Container.create(containerId, "mailshare", containerId, "me", true);
		mailshare = containerStore.create(mailshare);

		String containerId = "test_" + System.nanoTime();
		Container container = Container.create(containerId, DirectoryContainerType.TYPE, "test", "me", true);
		container.domainUid = containerId;
		container = containerStore.create(container);

		this.uid = "test_" + System.nanoTime();

		assertNotNull(mailshare);

		itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), mailshare, securityContext);

		Server server = new Server();
		BmConfIni conf = new BmConfIni();
		server.fqdn = conf.get("host");
		server.tags = Lists.newArrayList("a/b");

		serverValue = new ItemValue<>();
		serverValue.uid = "server";
		serverValue.value = server;

		mailboxStore = new MailboxStore(JdbcTestHelper.getInstance().getDataSource(), mailshare);
		dirEntryStore = new DirEntryStore(JdbcTestHelper.getInstance().getDataSource(), container);

		logger.debug("stores: {} {}", itemStore, mailboxStore);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCreateGetUpdateDelete() throws Exception {
		itemStore.create(Item.create(uid, null));
		Item item = itemStore.get(uid);
		Mailbox u = getDefaultMailbox();
		dirEntryStore.create(item, DirEntry.create(null, item.uid, DirEntry.Kind.USER, item.uid, "domain", null, true,
				true, true, "datalocation"));

		mailboxStore.create(item, u);

		Mailbox created = mailboxStore.get(item);
		assertNotNull("Nothing found", created);
		created.name = "updated_" + System.nanoTime();
		created.quota = 10;
		mailboxStore.update(item, created);

		Mailbox found = mailboxStore.get(item);
		assertNotNull("Nothing found", found);
		assertEquals(created.name, found.name);
		assertEquals(new Integer(10), created.quota);
		assertEquals(new HashSet<>(u.emails), new HashSet<>(found.emails));

		created.quota = null;
		mailboxStore.update(item, created);
		found = mailboxStore.get(item);
		assertNotNull("Nothing found", found);
		assertNull(created.quota);

		mailboxStore.delete(item);
		found = mailboxStore.get(item);
		assertNull(found);
	}

	@Test
	public void testGet() throws Exception {
		itemStore.create(Item.create("u1", null));
		Item item1 = itemStore.get("u1");
		Mailbox u = getDefaultMailbox();
		dirEntryStore.create(item1, DirEntry.create(null, item1.uid, DirEntry.Kind.USER, item1.uid, "domain", null,
				true, true, true, "datalocation"));
		mailboxStore.create(item1, u);

		itemStore.create(Item.create("u2", null));
		Item item2 = itemStore.get("u2");
		Mailbox u2 = getDefaultMailbox();
		dirEntryStore.create(item2, DirEntry.create(null, item2.uid, DirEntry.Kind.USER, item2.uid, "domain", null,
				true, true, true, "datalocation2"));

		u2.emails = new ArrayList<>();
		mailboxStore.create(item2, u2);

		Mailbox found = mailboxStore.get(item1);
		assertNotNull("Nothing found", found);
		assertEquals("datalocation", found.dataLocation);
		assertEquals(u.name, found.name);
		assertEquals(new HashSet<>(u.emails), new HashSet<>(found.emails));

		found = mailboxStore.get(item2);
		assertNotNull("Nothing found", found);
		assertTrue(found.emails.isEmpty());
		assertEquals("datalocation2", found.dataLocation);
	}

	@Test
	public void testEmailSearch() throws Exception {
		itemStore.create(Item.create(uid, null));
		Item item = itemStore.get(uid);
		Mailbox u = getDefaultMailbox();
		u.emails = Arrays.asList(Email.create("t@" + containerId, true, true),
				Email.create("tt@check.fr", false, false));

		mailboxStore.create(item, u);

		String res = mailboxStore.emailSearch("t@" + containerId);
		assertEquals(uid, res);

		res = mailboxStore.emailSearch("tt@check.fr");
		assertEquals(uid, res);

		res = mailboxStore.emailSearch("tt@" + containerId);
		assertNull(res);
	}

	@Test
	public void testDuplicateEmail() throws SQLException {
		itemStore.create(Item.create(uid, null));
		Item item = itemStore.get(uid);
		Mailbox mb = getDefaultMailbox();
		mb.emails = Arrays.asList(Email.create("t@" + containerId, true, true),
				Email.create("tt@check.fr", false, false));

		mailboxStore.create(item, mb);

		boolean b = mailboxStore.emailAlreadyUsed(item.id, mb.emails);
		assertFalse(b);

		b = mailboxStore.emailAlreadyUsed(42L, mb.emails);
		assertTrue(b);

		b = mailboxStore.emailAlreadyUsed(null, mb.emails);
		assertTrue(b);

		mb.emails = Arrays.asList(Email.create("t@" + containerId, false, false));
		b = mailboxStore.emailAlreadyUsed(null, mb.emails);
		assertTrue(b);

		mb.emails = Arrays.asList(Email.create("t@check.fr", false, false));
		b = mailboxStore.emailAlreadyUsed(null, mb.emails);
		assertTrue(b);

		mb.emails = Arrays.asList(Email.create("tt@check2.fr", false, false));
		b = mailboxStore.emailAlreadyUsed(null, mb.emails);
		assertFalse(b);

		mb.emails = Arrays.asList(Email.create("tt@" + containerId, false, true));
		b = mailboxStore.emailAlreadyUsed(null, mb.emails);
		assertTrue(b);

	}

	// FIXME should test nameSearch
	// FIXME should test typeSearch

	private Mailbox getDefaultMailbox() {
		Mailbox m = new Mailbox();
		m.name = "test" + System.nanoTime();
		m.type = Mailbox.Type.user;
		m.routing = Mailbox.Routing.internal;
		m.hidden = false;
		m.system = false;
		Email e = new Email();
		e.address = m.name + "@blue-mind.loc";
		m.emails = Arrays.asList(e, Email.create(m.name + "_al@blue-mind.loc", false, true));
		m.dataLocation = serverValue.uid;
		return m;
	}

	@Test
	public void byRoutingSearch() throws SQLException {
		itemStore.create(Item.create(uid, null));
		Item item = itemStore.get(uid);
		Mailbox mb = getDefaultMailbox();
		mailboxStore.create(item, mb);

		String uid2 = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid2, null));
		Item item2 = itemStore.get(uid2);
		Mailbox mb2 = getDefaultMailbox();
		mb2.routing = Routing.none;
		mailboxStore.create(item2, mb2);

		List<String> uids = mailboxStore.routingSearch(Routing.internal);
		assertEquals(1, uids.size());
		assertEquals(uid, uids.get(0));
	}

	/**
	 * {@link MailboxStore#allUids()} should only return uids of items linked to a
	 * {@link Mailbox}.
	 */
	@Test
	public void testAllUids() throws Exception {
		// create 4 non-mailbox items
		itemStore.create(Item.create("testAllUids001", null));
		itemStore.create(Item.create("testAllUids002", null));
		itemStore.create(Item.create("testAllUids003", null));
		itemStore.create(Item.create("testAllUids004", null));

		// create 1 mailbox item
		itemStore.create(Item.create(uid, null));
		Item item = itemStore.get(uid);
		Mailbox mb = getDefaultMailbox();
		mailboxStore.create(item, mb);

		// should retrieve 1 uid when calling mailboxStore.allUids()
		final List<String> allMailboxUids = mailboxStore.allUids();
		assertNotNull(allMailboxUids);
		assertEquals(1, allMailboxUids.size());

		// should retrieve 5 uids when calling itemStore.allItemUids()
		final List<String> allItemUids = itemStore.allItemUids();
		assertNotNull(allItemUids);
		assertEquals(5, allItemUids.size());

		// itemStore.allItemUids should containe uids in mailboxStore.allUids
		assertTrue(allItemUids.contains(allMailboxUids.get(0)));

	}
}
