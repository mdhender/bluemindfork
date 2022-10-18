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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.rules.MailFilterRule;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionRedirect;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleCondition;

public class MailFilterStoreTests {
	private static Logger logger = LoggerFactory.getLogger(MailFilterStoreTests.class);
	private MailboxStore mailshareStore;
	private ItemStore itemStore;
	private String uid;
	private MailFilterStore mailfilterStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerStore = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				securityContext);
		String containerId = "test_" + System.nanoTime() + ".fr";
		Container mailboxes = Container.create(containerId, "mailshare", containerId, "me", true);
		mailboxes = containerStore.create(mailboxes);

		this.uid = "test_" + System.nanoTime();

		assertNotNull(mailboxes);

		itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), mailboxes, securityContext);

		mailshareStore = new MailboxStore(JdbcTestHelper.getInstance().getDataSource(), mailboxes);

		mailfilterStore = new MailFilterStore(JdbcTestHelper.getInstance().getDataSource());
		logger.debug("stores: {} {}", itemStore, mailshareStore);

	}

	@After
	public void after() throws Exception {
		// JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testSetAndGetAndDelete() throws Exception {
		itemStore.create(Item.create(uid, null));
		Item item = itemStore.get(uid);
		Mailbox u = getDefaultMailbox();
		mailshareStore.create(item, u);

		MailFilter filter = MailFilter.create(defaultRule(), defaultRule());
		filter.rules.get(0).active = true;
		mailfilterStore.set(item, filter);

		MailFilter created = mailfilterStore.get(item);
		assertNotNull("Nothing found", created);

		assertEquals(2, created.rules.size());
		assertTrue(created.rules.get(0).active);
		assertNotNull(created.rules.get(0).markAsImportant().orElse(null));
		assertFalse(created.rules.get(1).active);
		assertNotNull(created.rules.get(1).markAsImportant().orElse(null));

		mailfilterStore.set(item, MailFilter.create());

		MailFilter updated = mailfilterStore.get(item);
		assertNotNull("Nothing found", updated);
		assertEquals(0, updated.rules.size());

		filter.rules.get(0).active = false;
		mailfilterStore.set(item, filter);
		updated = mailfilterStore.get(item);

		assertEquals(2, updated.rules.size());
		assertFalse(updated.rules.get(0).active);
		assertNotNull(updated.rules.get(0).markAsImportant().orElse(null));
		assertFalse(updated.rules.get(1).active);
		assertNotNull(updated.rules.get(1).markAsImportant().orElse(null));

		mailfilterStore.delete(item);
		MailFilter deleted = mailfilterStore.get(item);
		assertNotNull("Nothing found", deleted);
		assertEquals(0, deleted.rules.size());
	}

	@Test
	public void redirectWithCopy() throws Exception {
		itemStore.create(Item.create(uid, null));
		Item item = itemStore.get(uid);
		Mailbox u = getDefaultMailbox();
		mailshareStore.create(item, u);

		MailFilterRule rule = new MailFilterRule();
		rule.conditions.add(MailFilterRuleCondition.equal("from", "david@bm.com"));
		rule.active = true;
		rule.addRedirect(Arrays.asList("fwd@bm.lan"), true);

		MailFilter filter = MailFilter.create(rule);
		mailfilterStore.set(item, filter);
		filter = mailfilterStore.get(item);
		assertEquals(1, filter.rules.size());
		MailFilterRuleActionRedirect redirect = filter.rules.get(0).redirect().orElse(null);
		assertNotNull(redirect);
		assertTrue(redirect.keepCopy());

		rule = filter.rules.get(0);
		rule.addRedirect(Arrays.asList("fwd@bm.lan"), false);

		mailfilterStore.set(item, filter);
		filter = mailfilterStore.get(item);
		assertEquals(1, filter.rules.size());
		redirect = filter.rules.get(0).redirect().orElse(null);
		assertNotNull(redirect);
		assertFalse(redirect.keepCopy());
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
		m.emails = Arrays.asList(e);
		m.dataLocation = "fakeServerUid";
		return m;
	}

	private MailFilterRule defaultRule() {
		MailFilterRule rule = new MailFilterRule();
		rule.active = false;
		rule.conditions.add(MailFilterRuleCondition.equal("from", "bm.junit.roberto@gmail.com"));
		rule.addMarkAsImportant();
		return rule;
	}
}
