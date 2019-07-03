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
package net.bluemind.directory.persistance;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.persistance.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirectoryContainerType;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.persistance.DomainStore;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.mailbox.persistance.MailboxStore;

public class DirItemStoreTests {
	private static Logger logger = LoggerFactory.getLogger(DirItemStoreTests.class);
	private ItemStore itemStore;
	private String domainUid = "bm.lan";
	private SecurityContext securityContext;
	private Container container;
	private DirEntryStore dirEntryStore;
	private MailboxStore mailboxStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerHome = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				securityContext);

		String containerId2 = "zob.com";
		container = Container.create(containerId2, DirectoryContainerType.TYPE, "zob", "me", true);
		container.domainUid = "zob.com";
		container = containerHome.create(container);
		assertNotNull(container);

		itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), container, securityContext);
		Item domainItem = Item.create("zob.com", null);
		domainItem = itemStore.create(domainItem);
		DomainStore d = new DomainStore(JdbcTestHelper.getInstance().getDataSource());
		Domain domain = new Domain();
		domain.name = "zob.com";
		domain.label = "zob.com";
		domain.aliases = new HashSet<>(Arrays.asList("boom.com"));
		d.create(domainItem, domain);

		dirEntryStore = new DirEntryStore(JdbcTestHelper.getInstance().getDataSource(), container);

		mailboxStore = new MailboxStore(JdbcTestHelper.getInstance().getDataSource(), container);
		itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), container, securityContext);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testGetByEmail() throws Exception {
		creates(DirEntry.create(null, "test1", DirEntry.Kind.DOMAIN, "test1", "domain", null, true, false, false)
				.withEmails(Arrays.asList(Email.create("zob@test.com", true, true))),
				DirEntry.create(null, "test2", DirEntry.Kind.USER, "jojo", "jojo", "jojo@test.com", false, false, false)
						.withEmails("1jojo@test.com", "test2@test.com", "jojo@test.com"),
				DirEntry.create(null, "test3", DirEntry.Kind.USER, "test3", "zozo", "test3@test.com", true, false, true)
						.withEmails("1test3@test.com", "2test3@test.com", "test3@test.com"),
				DirEntry.create(null, "test4", DirEntry.Kind.USER, "test4", "", "test4@test.com", true, false, true)
						.withEmails(Arrays.asList(Email.create("test4@test.com", true, true))));

		DirItemStore dirItemStore = new DirItemStore(JdbcTestHelper.getInstance().getDataSource(), container,
				securityContext, Kind.DOMAIN);
		assertNotNull(dirItemStore.getByEmail("zob@test.com"));

		dirItemStore = new DirItemStore(JdbcTestHelper.getInstance().getDataSource(), container, securityContext,
				Kind.USER);
		assertNotNull(dirItemStore.getByEmail("1test3@test.com"));
		// test allAliases
		assertNotNull(dirItemStore.getByEmail("test4@boom.com"));
		assertNull(dirItemStore.getByEmail("zob@unknown.com"));

		assertNull(dirItemStore.getByEmail("not@test.com"));

		assertNull(dirItemStore.getByEmail("zob@test.com"));

		assertNull(dirItemStore.getByEmail(null));
		assertNull(dirItemStore.getByEmail(""));
		assertNull(dirItemStore.getByEmail("left"));

	}

	private List<Item> creates(DirEntry... dirEntries) throws SQLException {

		return Arrays.asList(dirEntries).stream().map(entry -> {
			try {
				itemStore.create(Item.create(entry.path, null));
				Item item = itemStore.get(entry.path);
				dirEntryStore.create(item, entry);
				if (entry.emails != null) {
					Mailbox mbox = new Mailbox();
					mbox.dataLocation = "test";
					mbox.emails = entry.emails;
					mbox.name = entry.displayName;
					mbox.routing = Routing.internal;
					mbox.type = Type.user;
					mailboxStore.create(item, mbox);
				}

				return item;
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}).collect(Collectors.toList());
	}
}
