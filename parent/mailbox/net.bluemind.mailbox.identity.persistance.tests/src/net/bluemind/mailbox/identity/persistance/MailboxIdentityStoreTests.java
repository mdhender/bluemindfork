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
package net.bluemind.mailbox.identity.persistance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.persistance.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.identity.api.Identity;
import net.bluemind.mailbox.identity.api.IdentityDescription;
import net.bluemind.mailbox.identity.api.SignatureFormat;
import net.bluemind.mailbox.persistance.MailboxStore;

public class MailboxIdentityStoreTests {

	private MailboxIdentityStore store;
	private MailboxStore mailboxesStore;
	private ItemStore itemStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		String containerId = "test_" + System.nanoTime() + ".fr";

		ContainerStore containerStore = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				securityContext);

		Container mailboxes = Container.create(containerId, "mailbox", containerId, "me", true);
		mailboxes = containerStore.create(mailboxes);

		store = new MailboxIdentityStore(JdbcTestHelper.getInstance().getDataSource());

		itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), mailboxes, securityContext);

		mailboxesStore = new MailboxStore(JdbcTestHelper.getInstance().getDataSource(), mailboxes);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCreateAndGet() throws SQLException {
		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, null));
		Item item = itemStore.get(uid);
		Mailbox u = getDefaultMailbox();
		mailboxesStore.create(item, u);

		store.create(item, "work", defaultIdentifier(u));

		Identity res = store.get(item, "work");
		assertNotNull(res);
		assertIdentityEquals(defaultIdentifier(u), res);
	}

	@Test
	public void testUpdate() throws SQLException {
		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, null));
		Item item = itemStore.get(uid);
		Mailbox u = getDefaultMailbox();
		mailboxesStore.create(item, u);

		store.create(item, "work", defaultIdentifier(u));

		Identity identity = defaultIdentifier(u);
		identity.displayname = "gg";
		store.update(item, "work", identity);
		Identity res = store.get(item, "work");
		assertIdentityEquals(identity, res);

	}

	@Test
	public void testDelete() throws SQLException {
		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, null));
		Item item = itemStore.get(uid);
		Mailbox u = getDefaultMailbox();
		mailboxesStore.create(item, u);

		store.create(item, "work", defaultIdentifier(u));

		store.delete(item, "work");
		assertNull(store.get(item, "work"));
	}

	@Test
	public void testGetDescriptions() throws SQLException {
		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, null));
		Item item = itemStore.get(uid);
		Mailbox u = getDefaultMailbox();
		mailboxesStore.create(item, u);

		store.create(item, "work", defaultIdentifier(u));
		store.create(item, "personal", defaultIdentifier(u));
		store.create(item, "forfun", defaultIdentifier(u));

		final Mailbox mailbox = new Mailbox();
		mailbox.name = "mboxName007";
		final ItemValue<Mailbox> mboxItemValue = ItemValue.create(item, mailbox);
		List<IdentityDescription> descriptions = store.getDescriptions(mboxItemValue);
		assertNotNull(descriptions);
		assertEquals(3, descriptions.size());
		descriptions.forEach(desc -> {
			assertNotNull(desc.isDefault);
		});
	}

	private void assertIdentityEquals(Identity expected, Identity value) {
		assertEquals(expected.name, value.name);
		assertEquals(expected.displayname, value.displayname);
		assertEquals(expected.email, value.email);
		assertEquals(expected.format, value.format);
		assertEquals(expected.sentFolder, value.sentFolder);
	}

	private Identity defaultIdentifier(Mailbox mbox) {
		Identity ret = new Identity();
		ret.displayname = "test displayname";
		ret.name = mbox.name;
		ret.email = mbox.defaultEmail().address;
		ret.sentFolder = "Sent";
		ret.signature = "Check that";
		ret.format = SignatureFormat.PLAIN;
		return ret;
	}

	private Mailbox getDefaultMailbox() {
		Mailbox m = new Mailbox();
		m.name = "test" + System.nanoTime();
		m.type = Mailbox.Type.user;
		m.routing = Mailbox.Routing.internal;
		m.hidden = false;
		m.system = false;
		Email e = new Email();
		e.address = m.name + "@blue-mind.loc";
		e.isDefault = true;
		m.emails = Arrays.asList(e);
		m.dataLocation = "testUid";
		return m;
	}
}
