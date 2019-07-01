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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

import net.bluemind.core.api.Email;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.persistance.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.directory.api.BaseDirEntry.AccountType;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.DirEntryQuery.Dir;
import net.bluemind.directory.api.DirEntryQuery.OrderBy;
import net.bluemind.directory.api.DirEntryQuery.StateFilter;
import net.bluemind.directory.api.DirectoryContainerType;
import net.bluemind.directory.api.OrgUnit;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.mailbox.persistance.MailboxStore;
import net.bluemind.user.api.User;
import net.bluemind.user.persistance.UserStore;

public class DirEntryStoreTests {
	private static Logger logger = LoggerFactory.getLogger(DirEntryStoreTests.class);
	private DirEntryStore dirEntryStore;
	private ItemStore itemStore;
	private MailboxStore mailboxStore;
	private UserStore userStore;
	private String domainUid = "bm.lan";
	private OrgUnitStore ouStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
		JdbcTestHelper.getInstance().beforeTest();

		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerHome = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				securityContext);
		String containerId = "test_" + System.nanoTime();
		Container container = Container.create(containerId, DirectoryContainerType.TYPE, "test", "me", true);
		container.domainUid = domainUid;
		container = containerHome.create(container);

		assertNotNull(container);

		itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), container, securityContext);

		dirEntryStore = new DirEntryStore(JdbcTestHelper.getInstance().getDataSource(), container);

		mailboxStore = new MailboxStore(JdbcTestHelper.getInstance().getDataSource(), container);

		userStore = new UserStore(JdbcTestHelper.getInstance().getDataSource(), container);
		ouStore = new OrgUnitStore(JdbcTestHelper.getInstance().getDataSource(), container);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testSchemaIsWellRegsited() {
		assertNotNull(JdbcTestHelper.getInstance().getDbSchemaService().getSchemaDescriptor("directory-schema"));
	}

	@Test
	public void testCreateAndGetAndUpdate() throws Exception {
		itemStore.create(Item.create(domainUid, null));
		Item item = itemStore.get(domainUid);

		try {
			dirEntryStore.create(item,
					DirEntry.create(null, item.uid, DirEntry.Kind.DOMAIN, item.uid, "domain", null, true, true, true));
		} catch (SQLException e) {
			logger.error("error during create", e);
			fail("error during create " + e.getMessage());
		}

		DirEntry entry = dirEntryStore.get(item);
		assertNotNull(entry);
		assertEquals(item.uid, entry.path);
		assertEquals(DirEntry.Kind.DOMAIN, entry.kind);
		assertNull(entry.email);
		assertTrue(entry.hidden);
		assertTrue(entry.system);
		assertTrue(entry.archived);
		entry = createAndGet("addressbook_" + domainUid, DirEntry.Kind.ADDRESSBOOK);
		assertEquals("bm.lan/addressbooks/addressbook_" + domainUid, entry.path);
		assertEquals(DirEntry.Kind.ADDRESSBOOK, entry.kind);
	}

	@Test
	public void testCreateWithOU() throws Exception {
		OrgUnit ou = new OrgUnit();
		ou.name = "checkthat";
		ou.parentUid = null;

		Item itemOu = itemStore.create(Item.create("test1", null));
		ouStore.create(itemOu, ou);

		itemStore.create(Item.create(domainUid, null));
		Item item = itemStore.get(domainUid);

		try {
			dirEntryStore.create(item, DirEntry.create("test1", item.uid, DirEntry.Kind.DOMAIN, item.uid, "domain",
					null, true, true, true));
		} catch (SQLException e) {
			logger.error("error during create", e);
			fail("error during create " + e.getMessage());
		}

		DirEntry entry = dirEntryStore.get(item);
		assertNotNull(entry);
		assertEquals("test1", entry.orgUnitUid);
	}

	@Test
	public void testUpdateWithOU() throws Exception {
		OrgUnit ou = new OrgUnit();
		ou.name = "checkthat";
		ou.parentUid = null;

		Item itemOu = itemStore.create(Item.create("test1", null));
		ouStore.create(itemOu, ou);

		itemStore.create(Item.create(domainUid, null));
		Item item = itemStore.get(domainUid);

		dirEntryStore.create(item,
				DirEntry.create(null, item.uid, DirEntry.Kind.DOMAIN, item.uid, "domain", null, true, true, true));
		DirEntry entry = dirEntryStore.get(item);
		assertNotNull(entry);
		assertNull(entry.orgUnitUid);

		item = itemStore.get(domainUid);
		dirEntryStore.update(item,
				DirEntry.create("test1", item.uid, DirEntry.Kind.DOMAIN, item.uid, "domain", null, true, true, true));

		entry = dirEntryStore.get(item);
		assertNotNull(entry);
		assertEquals("test1", entry.orgUnitUid);
	}

	@Test
	public void testSearchFilter() throws Exception {
		creates(true, DirEntry.create(null, "test1", DirEntry.Kind.DOMAIN, "test1", "domain", null, true, false, false),
				DirEntry.create(null, "test2", DirEntry.Kind.USER, "jojo", "jojo", "jojo@test.com", false, false, false)
						.withEmails("1jojo@test.com", "test2@test.com", "jojo@test.com"),
				DirEntry.create(null, "test3", DirEntry.Kind.USER, "test3", "zozo", "test3@test.com", true, false, true)
						.withEmails("1test3@test.com", "2test3@test.com", "test3@test.com"));

		DirEntryQuery query = DirEntryQuery.filterKind(Kind.DOMAIN);
		query.hiddenFilter = false;
		ListResult<Item> res = dirEntryStore.search(query);
		assertEquals(1, res.total);
		assertEquals(1, res.values.size());
		assertEquals("test1", res.values.get(0).uid);

		query = DirEntryQuery.filterKind(Kind.USER);
		res = dirEntryStore.search(query);
		assertEquals(1, res.total);
		assertEquals(1, res.values.size());
		assertEquals("test2", res.values.get(0).uid);

		query.hiddenFilter = false;
		res = dirEntryStore.search(query);
		assertEquals(2, res.total);
		assertEquals(2, res.values.size());
		assertEquals("test2", res.values.get(0).uid);
		assertEquals("test3", res.values.get(1).uid);

		res = dirEntryStore.search(DirEntryQuery.filterKind(Kind.ADDRESSBOOK));
		assertEquals(0, res.total);

		query = DirEntryQuery.filterName("jo");
		query.kindsFilter = Arrays.asList(Kind.USER);
		res = dirEntryStore.search(query);
		assertEquals(1, res.total);
		assertEquals(1, res.values.size());
		assertEquals("test2", res.values.get(0).uid);

		query = DirEntryQuery.filterName("jo");
		query.kindsFilter = Arrays.asList(Kind.DOMAIN);
		res = dirEntryStore.search(query);
		assertEquals(0, res.total);

		query.hiddenFilter = false;
		res = dirEntryStore.search(query);
		assertEquals(0, res.total);

		// test filter email

		query = DirEntryQuery.filterEmail("jojo@test.com");
		res = dirEntryStore.search(query);
		assertEquals(1, res.total);
		assertEquals(1, res.values.size());
		assertEquals("test2", res.values.get(0).uid);

		query = DirEntryQuery.filterEmail("jojo@fake.com");
		res = dirEntryStore.search(query);
		assertEquals(0, res.total);
		assertEquals(0, res.values.size());

		// test order by direction
		query = DirEntryQuery.filterKind(Kind.USER);
		query.hiddenFilter = false;
		query.order = DirEntryQuery.order(OrderBy.displayname, Dir.asc);
		res = dirEntryStore.search(query);
		assertEquals(2, res.total);
		assertEquals(2, res.values.size());
		assertEquals("test2", res.values.get(0).uid);
		assertEquals("test3", res.values.get(1).uid);

		query.order = DirEntryQuery.order(OrderBy.displayname, Dir.desc);
		res = dirEntryStore.search(query);
		assertEquals(2, res.total);
		assertEquals(2, res.values.size());
		assertEquals("test3", res.values.get(0).uid);
		assertEquals("test2", res.values.get(1).uid);

		// upper case search
		query = DirEntryQuery.filterName("JO");
		query.kindsFilter = Arrays.asList(Kind.USER);
		res = dirEntryStore.search(query);
		assertEquals(1, res.total);
		assertEquals(1, res.values.size());
		assertEquals("test2", res.values.get(0).uid);

		// search by entryUid
		query = DirEntryQuery.filterEntryUid("test1", "i-dont-exist", "test3");
		query.hiddenFilter = false;
		res = dirEntryStore.search(query);
		assertEquals(2, res.total);
		assertEquals(2, res.values.size());
		assertEquals("test1", res.values.get(0).uid);
		assertEquals("test3", res.values.get(1).uid);

		// test search by nameOrEmail
		// upper case search

		query = DirEntryQuery.filterNameOrEmail("test3");
		query.hiddenFilter = false;
		query.kindsFilter = Arrays.asList(Kind.USER);
		res = dirEntryStore.search(query);
		assertEquals(1, res.total);
		assertEquals(1, res.values.size());
		assertEquals("test3", res.values.get(0).uid);

		// full email
		query = DirEntryQuery.filterNameOrEmail("test3@tes");
		query.hiddenFilter = false;
		query.kindsFilter = Arrays.asList(Kind.USER);
		res = dirEntryStore.search(query);
		assertEquals(1, res.total);
		assertEquals(1, res.values.size());
		assertEquals("test3", res.values.get(0).uid);

		query = DirEntryQuery.filterNameOrEmail("test3");
		query.hiddenFilter = false;
		query.kindsFilter = Arrays.asList(Kind.USER);
		res = dirEntryStore.search(query);
		assertEquals(1, res.total);
		assertEquals(1, res.values.size());
		assertEquals("test3", res.values.get(0).uid);

		query = DirEntryQuery.filterNameOrEmail("zo");
		query.kindsFilter = Arrays.asList(Kind.USER);
		query.hiddenFilter = false;
		res = dirEntryStore.search(query);
		assertEquals(1, res.total);
		assertEquals(1, res.values.size());
		assertEquals("test3", res.values.get(0).uid);

		query = new DirEntryQuery();
		query.hiddenFilter = false;
		query.kindsFilter = Arrays.asList(Kind.USER);
		query.stateFilter = StateFilter.Active;
		res = dirEntryStore.search(query);
		assertEquals(1, res.total);
		assertEquals(1, res.values.size());
		assertEquals("test2", res.values.get(0).uid);

		query.stateFilter = StateFilter.All;
		res = dirEntryStore.search(query);
		assertEquals(2, res.total);
		assertEquals(2, res.values.size());

		query.stateFilter = StateFilter.Archived;
		res = dirEntryStore.search(query);
		assertEquals(1, res.total);
		assertEquals(1, res.values.size());
		assertEquals("test3", res.values.get(0).uid);

		// by email
		query = DirEntryQuery.filterEmail("test3@test.com");
		query.hiddenFilter = false;
		query.kindsFilter = Arrays.asList(Kind.USER);
		res = dirEntryStore.search(query);
		assertEquals(1, res.total);
		assertEquals(1, res.values.size());
		assertEquals("test3", res.values.get(0).uid);

		// by email, exclude entry
		query = new DirEntryQuery();
		query.hiddenFilter = false;
		res = dirEntryStore.search(query);
		assertEquals(3, res.total);
		assertEquals(3, res.values.size());
	}

	@Test
	public void testSearchFilterManageable() throws Exception {

		OrgUnit ouRoot = new OrgUnit();
		ouRoot.name = "checkthat";
		ouRoot.parentUid = null;

		Item itemOuRoot = itemStore.create(Item.create("test1Ou", null));
		ouStore.create(itemOuRoot, ouRoot);

		OrgUnit ouRoot2 = new OrgUnit();
		ouRoot2.name = "checkthat2";
		ouRoot2.parentUid = null;

		Item itemOuRoot2 = itemStore.create(Item.create("test2Ou", null));
		ouStore.create(itemOuRoot2, ouRoot2);

		OrgUnit ouRootChild = new OrgUnit();
		ouRootChild.name = "checkthatChild";
		ouRootChild.parentUid = itemOuRoot.uid;

		Item itemOu2 = itemStore.create(Item.create("test1OuChild", null));
		ouStore.create(itemOu2, ouRootChild);

		creates(true, DirEntry.create(null, "test1", DirEntry.Kind.DOMAIN, "test1", "domain", null, true, false, false),
				DirEntry.create(itemOu2.uid, "test2", DirEntry.Kind.USER, "jojo", "jojo", "jojo@test.com", false, false,
						false).withEmails("1jojo@test.com", "test2@test.com", "jojo@test.com"),
				DirEntry.create(itemOuRoot2.uid, "test22", DirEntry.Kind.USER, "jojo2", "jojo2", "jojo2@test.com",
						false, false, false).withEmails("21jojo@test.com", "2test2@test.com", "2jojo@test.com"),
				DirEntry.create(itemOuRoot.uid, "test3", DirEntry.Kind.USER, "test3", "zozo", "test3@test.com", true,
						false, true).withEmails("1test3@test.com", "2test3@test.com", "test3@test.com"),
				DirEntry.create(itemOuRoot.uid, "testms3", DirEntry.Kind.MAILSHARE, "testms3", "zozoms",
						"test3ms@test.com", true, false, true)
						.withEmails("1test3ms@test.com", "2test3ms@test.com", "test3ms@test.com"));

		DirEntryQuery query = new DirEntryQuery();
		query.onlyManagable = true;
		query.hiddenFilter = false;
		query.systemFilter = false;
		List<ManageableOrgUnit> manageable = Arrays.asList(new ManageableOrgUnit(null, ImmutableSet.of(Kind.USER)));
		ListResult<Item> res = dirEntryStore.searchManageable(query, manageable);
		assertEquals(3, res.total);

		manageable = Arrays.asList(new ManageableOrgUnit(null, ImmutableSet.of(Kind.USER, Kind.MAILSHARE)));
		res = dirEntryStore.searchManageable(query, manageable);
		assertEquals(4, res.total);

		manageable = Arrays
				.asList(new ManageableOrgUnit(null, ImmutableSet.of(Kind.USER, Kind.MAILSHARE, Kind.DOMAIN)));
		res = dirEntryStore.searchManageable(query, manageable);
		assertEquals(5, res.total);

		manageable = Arrays.asList(new ManageableOrgUnit(itemOu2.uid, ImmutableSet.of(Kind.USER)));
		res = dirEntryStore.searchManageable(query, manageable);
		assertEquals(1, res.total);

		manageable = Arrays.asList(new ManageableOrgUnit(itemOuRoot2.uid, ImmutableSet.of(Kind.USER)));
		res = dirEntryStore.searchManageable(query, manageable);
		assertEquals(1, res.total);

		manageable = Arrays.asList(new ManageableOrgUnit(itemOu2.uid, ImmutableSet.of(Kind.USER, Kind.MAILSHARE)));
		res = dirEntryStore.searchManageable(query, manageable);
		assertEquals(1, res.total);

		manageable = Arrays.asList(new ManageableOrgUnit(itemOuRoot.uid, ImmutableSet.of(Kind.USER)));
		res = dirEntryStore.searchManageable(query, manageable);
		assertEquals(2, res.total);

		manageable = Arrays.asList(new ManageableOrgUnit(itemOuRoot.uid, ImmutableSet.of(Kind.USER, Kind.MAILSHARE)));
		res = dirEntryStore.searchManageable(query, manageable);
		assertEquals(3, res.total);

		// Entries matched by more than one manageable are listed only once
		manageable = Arrays.asList(new ManageableOrgUnit(null, ImmutableSet.of(Kind.USER)),
				new ManageableOrgUnit(itemOu2.uid, ImmutableSet.of(Kind.USER)));
		res = dirEntryStore.searchManageable(query, manageable);
		assertEquals(3, res.total);

		manageable = Arrays.asList(new ManageableOrgUnit(itemOuRoot.uid, ImmutableSet.of(Kind.USER)),
				new ManageableOrgUnit(itemOu2.uid, ImmutableSet.of(Kind.USER)));
		res = dirEntryStore.searchManageable(query, manageable);
		assertEquals(2, res.total);

		// Join Kind
		manageable = Arrays.asList(new ManageableOrgUnit(itemOuRoot.uid, ImmutableSet.of(Kind.MAILSHARE)),
				new ManageableOrgUnit(itemOu2.uid, ImmutableSet.of(Kind.USER)));
		res = dirEntryStore.searchManageable(query, manageable);
		assertEquals(2, res.total);
	}

	@Test
	public void testSystemFilter() throws SQLException {
		List<Item> items = creates(true,
				DirEntry.create(null, "test2", DirEntry.Kind.USER, "jojo", "jojo", "jojo@test.com", false, false, false)
						.withEmails("1jojo@test.com", "test2@test.com", "jojo@test.com"),
				DirEntry.create(null, "test3", DirEntry.Kind.USER, "zozo", "zozo", "test3@test.com", false, false, true)
						.withEmails("1test3@test.com", "2test3@test.com", "test3@test.com"),
				DirEntry.create(null, "system", DirEntry.Kind.USER, "system", "system", "system@test.com", false, true,
						false).withEmails("system@test.com"));

		User user = new User();
		user.login = "user";
		userStore.create(items.get(1), user);

		DirEntryQuery query = new DirEntryQuery();
		query.kindsFilter = Arrays.asList(Kind.USER);
		query.systemFilter = true;
		ListResult<Item> res = dirEntryStore.search(query);
		assertEquals(2, res.total);
		assertEquals(2, res.values.size());

		query.systemFilter = false;
		res = dirEntryStore.search(query);
		assertEquals(3, res.total);
		assertEquals(3, res.values.size());
	}

	@Test
	public void testSearchByPath() throws Exception {
		creates(true,
				DirEntry.create(null, "bm.lan", DirEntry.Kind.DOMAIN, "bm.lan", "domain", null, true, true, false), //
				DirEntry.create(null, "bm.lan/users/jojo", DirEntry.Kind.USER, "jojo", "jojo", "jojo@test.com", false,
						false, false), //
				DirEntry.create(null, "bm.lan/users/zozo", DirEntry.Kind.USER, "zozo", "zozo", "zozo@test.com", true,
						false, false));

		List<String> res = dirEntryStore.path("bm.lan/users");
		assertEquals(2, res.size());
		assertEquals("bm.lan/users/jojo", res.get(0));
		assertEquals("bm.lan/users/zozo", res.get(1));
	}

	private List<Item> creates(boolean withMailbox, DirEntry... dirEntries) throws SQLException {

		return Arrays.asList(dirEntries).stream().map(entry -> {
			try {
				itemStore.create(Item.create(entry.path, null));
				Item item = itemStore.get(entry.path);
				dirEntryStore.create(item, entry);
				if (entry.emails != null && withMailbox) {
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
				throw new RuntimeException(e);
			}
		}).collect(Collectors.toList());
	}

	@Test
	public void testDelete() throws SQLException {
		DirEntry entry = createAndGet("bm.lan", Kind.DOMAIN);

		try {
			dirEntryStore.delete(itemStore.get(entry.entryUid));
		} catch (SQLException e) {
			logger.error("error during delete", e);
			fail("error during delete " + e.getMessage());
		}
	}

	@Test
	public void testGetByEmail() throws Exception {
		creates(true,
				DirEntry.create(null, "test1", DirEntry.Kind.DOMAIN, "test1", "domain", null, true, false, false)
						.withEmails(Arrays.asList(Email.create("zob@test.com", true, true))),
				DirEntry.create(null, "test2", DirEntry.Kind.USER, "jojo", "jojo", "jojo@test.com", false, false, false)
						.withEmails("1jojo@test.com", "test2@test.com", "jojo@test.com"),
				DirEntry.create(null, "test3", DirEntry.Kind.USER, "test3", "zozo", "test3@test.com", true, false, true)
						.withEmails("1test3@test.com", "2test3@test.com", "test3@test.com"));

		assertNotNull(dirEntryStore.byEmail("1test3@test.com", false));
		assertNotNull(dirEntryStore.byEmail("zob@test.com", false));
		assertNull(dirEntryStore.byEmail("not@test.com", false));

		// test allAliases: by passing isDomainAlias parameter at true, zob.com will be
		// considered as an alias
		assertNotNull(dirEntryStore.byEmail("zob@zob.com", true));
		assertNull(dirEntryStore.byEmail("zob@zob.com", false));

		// test when dir_entry created has no entry on table t_mailbox_email (as
		// external user for example)
		creates(false, DirEntry.create(null, "extuser", DirEntry.Kind.EXTERNALUSER, "extuser", "extuser",
				"ext@user.com", true, false, false));
		assertNotNull(dirEntryStore.byEmail("ext@user.com", false));
	}

	@Test
	public void testSetDataLocation() throws Exception {
		itemStore.create(Item.create(domainUid, null));
		Item item = itemStore.get(domainUid);

		try {
			dirEntryStore.create(item, DirEntry.create(null, item.uid, DirEntry.Kind.DOMAIN, item.uid, "domain", null,
					true, true, true, "datalocation"));
		} catch (SQLException e) {
			logger.error("error during create", e);
			fail("error during create " + e.getMessage());
		}

		DirEntry entry = dirEntryStore.get(item);
		assertEquals("datalocation", entry.dataLocation);
	}

	@Test
	public void testUpdateDataLocation() throws Exception {
		itemStore.create(Item.create(domainUid, null));
		Item item = itemStore.get(domainUid);

		try {
			dirEntryStore.create(item, DirEntry.create(null, item.uid, DirEntry.Kind.DOMAIN, item.uid, "domain", null,
					true, true, true, "datalocation"));
		} catch (SQLException e) {
			logger.error("error during create", e);
			fail("error during create " + e.getMessage());
		}

		DirEntry entry = dirEntryStore.get(item);
		entry.dataLocation = "updated_datalocation";

		dirEntryStore.update(item, entry);
		entry = dirEntryStore.get(item);
		assertEquals("updated_datalocation", entry.dataLocation);

	}

	@Test
	public void testSearch_NullOrder() throws SQLException {
		List<Item> items = creates(true,
				DirEntry.create(null, "test1", DirEntry.Kind.DOMAIN, "test1", "domain", null, true, false, false),
				DirEntry.create(null, "test2", DirEntry.Kind.USER, "jojo", "jojo", "jojo@test.com", false, false, false)
						.withEmails("1jojo@test.com", "test2@test.com", "jojo@test.com"),
				DirEntry.create(null, "test3", DirEntry.Kind.USER, "test3", "zozo", "test3@test.com", true, false, true)
						.withEmails("1test3@test.com", "2test3@test.com", "test3@test.com"));

		User user = new User();
		user.login = "Zarathoustra";
		userStore.create(items.get(1), user);

		// test order by direction
		DirEntryQuery query = DirEntryQuery.filterKind(Kind.USER);
		query.hiddenFilter = false;
		query.order = null;
		ListResult<Item> res = dirEntryStore.search(query);
		assertEquals(2, res.total);
		assertEquals(2, res.values.size());
		assertEquals("test2", res.values.get(0).uid);
		assertEquals("test3", res.values.get(1).uid);
	}

	@Test
	public void testDefaultAccountType() throws SQLException {
		itemStore.create(Item.create(domainUid, null));
		Item item = itemStore.get(domainUid);

		DirEntry de = DirEntry.create(null, item.uid, DirEntry.Kind.USER, item.uid, "test", null, true, true, true,
				"datalocation");
		dirEntryStore.create(item, de);

		DirEntry entry = dirEntryStore.get(item);
		assertNull(entry.accountType);
	}

	@Test
	public void testAccountType() throws SQLException {
		itemStore.create(Item.create(domainUid, null));
		Item item = itemStore.get(domainUid);

		DirEntry de = DirEntry.create(null, item.uid, DirEntry.Kind.USER, item.uid, "test", null, true, true, true,
				"datalocation");
		de.accountType = DirEntry.AccountType.SIMPLE;
		dirEntryStore.create(item, de);

		DirEntry entry = dirEntryStore.get(item);
		assertEquals(DirEntry.AccountType.SIMPLE, entry.accountType);
	}

	@Test
	public void tesUpdatetDirEntry_DoNotUpdateAccountType() throws SQLException {
		itemStore.create(Item.create(domainUid, null));
		Item item = itemStore.get(domainUid);

		DirEntry de = DirEntry.create(null, item.uid, DirEntry.Kind.USER, item.uid, "test", null, true, true, true,
				"datalocation");
		de.accountType = DirEntry.AccountType.SIMPLE;
		dirEntryStore.create(item, de);

		DirEntry entry = dirEntryStore.get(item);
		entry.accountType = AccountType.FULL;
		dirEntryStore.update(item, entry);

		entry = dirEntryStore.get(item);
		assertEquals(DirEntry.AccountType.SIMPLE, entry.accountType);
	}

	@Test
	public void testUpdateAccountType() throws SQLException {
		itemStore.create(Item.create(domainUid, null));
		Item item = itemStore.get(domainUid);

		DirEntry de = DirEntry.create(null, item.uid, DirEntry.Kind.USER, item.uid, "test", null, true, true, true,
				"datalocation");
		de.accountType = DirEntry.AccountType.SIMPLE;
		dirEntryStore.create(item, de);

		dirEntryStore.updateAccountType(item, AccountType.FULL);

		DirEntry entry = dirEntryStore.get(item);
		assertEquals(DirEntry.AccountType.FULL, entry.accountType);
	}

	private DirEntry createAndGet(String path, Kind kind) throws SQLException {
		itemStore.create(Item.create(path, null));
		Item item = itemStore.get(path);
		dirEntryStore.create(item,
				DirEntry.create(null, item.uid, kind, item.uid, "test", "test@test.com", false, false, false));

		DirEntry entry = dirEntryStore.get(item);
		assertNotNull(entry);
		return entry;
	}
}
