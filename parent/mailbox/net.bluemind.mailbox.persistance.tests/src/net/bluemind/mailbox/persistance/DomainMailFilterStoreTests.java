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
package net.bluemind.mailbox.persistance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.persistance.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.mailbox.api.MailFilter;

public class DomainMailFilterStoreTests {
	private static Logger logger = LoggerFactory.getLogger(DomainMailFilterStoreTests.class);
	private MailboxStore mailshareStore;
	private ItemStore itemStore;
	private DomainMailFilterStore mailfilterStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		
		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerStore = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				securityContext);
		String containerId = "test_" + System.nanoTime() + ".fr";
		Container mailboxes = Container.create(containerId, "mailshare", containerId, "me", true);
		mailboxes = containerStore.create(mailboxes);

		assertNotNull(mailboxes);

		itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), mailboxes, securityContext);

		mailshareStore = new MailboxStore(JdbcTestHelper.getInstance().getDataSource(), mailboxes);

		mailfilterStore = new DomainMailFilterStore(JdbcTestHelper.getInstance().getDataSource(), mailboxes);
		logger.debug("stores: {} {}", itemStore, mailshareStore);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testSetAndGetAndDelete() throws Exception {

		mailfilterStore.set(MailFilter.create(defaultRule(), defaultRule()));

		MailFilter created = mailfilterStore.get();
		assertNotNull("Nothing found", created);
		assertEquals(2, created.rules.size());

		mailfilterStore.set(MailFilter.create());

		MailFilter updated = mailfilterStore.get();
		assertNotNull("Nothing found", updated);
		assertEquals(0, updated.rules.size());
	}

	private MailFilter.Rule defaultRule() {
		MailFilter.Rule sf = new MailFilter.Rule();
		sf.criteria = "from:bm.junit.roberto@gmail.com";
		sf.star = true;
		return sf;
	}

}
