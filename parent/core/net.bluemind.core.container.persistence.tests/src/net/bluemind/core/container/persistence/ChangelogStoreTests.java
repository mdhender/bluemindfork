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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.ChangeLogEntry;
import net.bluemind.core.container.model.ChangeLogEntry.Type;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.persistence.ChangelogStore.LogEntry;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;

public class ChangelogStoreTests {
	private static Logger logger = LoggerFactory.getLogger(ChangelogStoreTests.class);
	private ChangelogStore changelogStore;
	private ContainerStore containerHome;
	private String containerId;
	private Container container;
	private ItemStore itemStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		containerHome = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(), securityContext);

		containerId = "test_" + System.nanoTime();
		container = Container.create(containerId, "test", "test", "me", true);
		container = containerHome.create(container);
		assertNotNull(container);

		changelogStore = new ChangelogStore(JdbcTestHelper.getInstance().getDataSource(), container);
		itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), container, securityContext);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testItemCreated() {

		try {
			changelogStore.itemCreated(LogEntry.create(1, "test", "extid", "author", "junit-testItemCreated", 42, 0L));
		} catch (SQLException e1) {
			logger.error(e1.getMessage(), e1);
			fail(e1.getMessage());
		}
	}

	@Test
	public void testItemUpdated() {

		try {
			changelogStore.itemUpdated(LogEntry.create(1, "test", "extId", "author", "junit-testItemUpdated", 42, 0L));
		} catch (SQLException e1) {
			logger.error(e1.getMessage(), e1);
			fail(e1.getMessage());
		}
	}

	@Test
	public void testItemDeleted() {

		try {
			changelogStore.itemDeleted(LogEntry.create(1, "test", "extId", "author", "junit-testItemDeleted", 42, 0L));
		} catch (SQLException e1) {
			logger.error(e1.getMessage(), e1);
			fail(e1.getMessage());
		}
	}

	@Test
	public void testChangeset() throws SQLException {
		changelogStore.itemCreated(LogEntry.create(1, "test", "extId1", "author", "junit-testChangeset", 42, 0L));
		changelogStore.itemUpdated(LogEntry.create(2, "test", "extId1", "author", "junit-testChangeset", 42, 0L));
		changelogStore.itemUpdated(LogEntry.create(3, "test2", "extId2", "author2", "junit-testChangeset", 43, 0L));
		changelogStore.itemDeleted(LogEntry.create(4, "test", "extId1", "author", "junit-testChangeset", 42, 0L));

		ContainerChangeset<String> changeset = changelogStore.changeset(0, Long.MAX_VALUE);
		assertEquals(4, changeset.version);
		assertEquals(1, changeset.created.size());
		assertEquals(0, changeset.updated.size());
		assertEquals(0, changeset.deleted.size());

		changeset = changelogStore.changeset(1, Long.MAX_VALUE);
		assertEquals(4, changeset.version);
		assertEquals(0, changeset.created.size());
		assertEquals(1, changeset.updated.size());
		assertEquals(1, changeset.deleted.size());

	}

	@Test
	public void testChangesetOrdering() throws SQLException {
		changelogStore.itemCreated(LogEntry.create(1, "test1", "extId1", "author", "junit-testChangeset", 42, 3L));
		changelogStore.itemCreated(LogEntry.create(2, "test2", "extId2", "author", "junit-testChangeset", 43, 2L));
		changelogStore.itemCreated(LogEntry.create(3, "test3", "extId3", "author", "junit-testChangeset", 44, 8L));

		ContainerChangeset<String> changeset = changelogStore.changeset(s -> s, 0, Long.MAX_VALUE);
		assertEquals(3, changeset.created.size());
		assertEquals("test3", changeset.created.get(0));
		assertEquals("test1", changeset.created.get(1));
		assertEquals("test2", changeset.created.get(2));

		changeset = changelogStore.changeset(s -> -s, 0, Long.MAX_VALUE);
		assertEquals("test2", changeset.created.get(0));
		assertEquals("test1", changeset.created.get(1));
		assertEquals("test3", changeset.created.get(2));

	}

	@Test
	public void testFilteredChangesetOrdering() throws SQLException {
		OfflineMgmtStore oms = new OfflineMgmtStore(JdbcTestHelper.getInstance().getDataSource());
		oms.reserveItemIds(42 + 4);
		itemStore.create(Item.create("test1", 42L));
		itemStore.create(Item.create("test2", 43L));
		itemStore.create(Item.create("test3", 44L));
		itemStore.create(Item.create("test4", 45L, ItemFlag.Deleted));
		changelogStore.itemCreated(LogEntry.create(1, "test1", "extId1", "author", "junit-testChangeset", 42, 3L));
		changelogStore.itemCreated(LogEntry.create(2, "test2", "extId2", "author", "junit-testChangeset", 43, 2L));
		changelogStore.itemCreated(LogEntry.create(3, "test3", "extId3", "author", "junit-testChangeset", 44, 8L));
		changelogStore.itemCreated(LogEntry.create(4, "test4", "extId4", "author", "junit-testChangeset", 45, 1L));

		ContainerChangeset<ItemVersion> changeset = changelogStore.changesetById(s -> s, 0, Long.MAX_VALUE,
				ItemFlagFilter.create().mustNot(ItemFlag.Deleted));
		assertEquals(3, changeset.created.size());
		assertEquals(44L, changeset.created.get(0).id);
		assertEquals(42L, changeset.created.get(1).id);
		assertEquals(43L, changeset.created.get(2).id);

		changeset = changelogStore.changesetById(s -> -s, 0, Long.MAX_VALUE,
				ItemFlagFilter.create().mustNot(ItemFlag.Deleted));
		assertEquals(43L, changeset.created.get(0).id);
		assertEquals(42L, changeset.created.get(1).id);
		assertEquals(44L, changeset.created.get(2).id);

	}

	@Test
	public void testChangesetById() throws SQLException {
		changelogStore.itemCreated(LogEntry.create(1, "test", "extId1", "author", "junit-testChangeset", 42, 0L));
		changelogStore.itemUpdated(LogEntry.create(2, "test", "extId1", "author", "junit-testChangeset", 42, 0L));
		changelogStore.itemUpdated(LogEntry.create(3, "test2", "extId2", "author2", "junit-testChangeset", 43, 0L));
		changelogStore.itemDeleted(LogEntry.create(4, "test", "extId1", "author", "junit-testChangeset", 42, 0L));

		ContainerChangeset<Long> changeset = changelogStore.changesetById(1, Long.MAX_VALUE);
		assertEquals(4, changeset.version);
		assertEquals(0, changeset.created.size());
		assertEquals(1, changeset.updated.size());
		assertEquals(1, changeset.deleted.size());

		changeset = changelogStore.changesetById(0, Long.MAX_VALUE);
		assertEquals(4, changeset.version);
		assertEquals(1, changeset.created.size());
		assertEquals(0, changeset.updated.size());
		assertEquals(0, changeset.deleted.size());

	}

	@Test
	public void testZipWithVersions() {
		ChangeLogEntry one = ChangeLogEntry.create(42, 1, "u1", Type.Created);
		ChangeLogEntry two = ChangeLogEntry.create(42, 2, "u1", Type.Updated);
		ChangeLogEntry three = ChangeLogEntry.create(42, 3, "u1", Type.Updated);
		ContainerChangeset<ItemVersion> zipped = ChangelogUtils.toChangeset(s -> s, 0, Arrays.asList(one, two, three),
				e -> {
					return new ItemVersion(e);
				}, ItemFlagFilter.all());
		assertEquals(1, zipped.created.size());
		assertEquals(0, zipped.updated.size());
		assertEquals(3, zipped.created.get(0).version);
	}

	@Test
	public void testChangesetByIdFiltered() throws SQLException {
		changelogStore.itemCreated(LogEntry.create(1, "test", "extId1", "author", "junit-testChangeset", 42, 0L));
		changelogStore.itemUpdated(LogEntry.create(2, "test2", "extId2", "author2", "junit-testChangeset", 43, 0L));
		changelogStore.itemCreated(LogEntry.create(3, "test", "extId1", "author", "junit-testChangeset", 44, 0L));
		changelogStore.itemCreated(LogEntry.create(4, "test", "extId1", "author", "junit-testChangeset", 45, 0L));
		changelogStore.itemUpdated(LogEntry.create(5, "test", "extId1", "author", "junit-testChangeset", 42, 0L));
		changelogStore.itemDeleted(LogEntry.create(6, "test", "extId1", "author", "junit-testChangeset", 42, 0L));
		changelogStore.itemUpdated(LogEntry.create(7, "test", "extId1", "author", "junit-testChangeset", 44, 0L));

		ItemFlagFilter allFilter = ItemFlagFilter.all();
		ContainerChangeset<ItemVersion> changeset = changelogStore.changesetById(0, 7, allFilter);
		assertEquals(7, changeset.version);
		assertEquals(3, changeset.created.size());
		assertEquals(0, changeset.updated.size());
		assertEquals(0, changeset.deleted.size());
		assertTrue(changeset.created.stream().anyMatch(v -> v.id == 43l));
		assertTrue(changeset.created.stream().anyMatch(v -> v.id == 44l));
		assertTrue(changeset.created.stream().anyMatch(v -> v.id == 45l));

		changeset = changelogStore.changesetById(1, 7, allFilter);
		assertEquals(7, changeset.version);
		assertEquals(2, changeset.created.size());
		assertEquals(1, changeset.updated.size());
		assertEquals(1, changeset.deleted.size());
		assertTrue(changeset.created.stream().anyMatch(v -> v.id == 44l));
		assertTrue(changeset.created.stream().anyMatch(v -> v.id == 45l));
		assertTrue(changeset.updated.stream().anyMatch(v -> v.id == 43l));
	}

	@Test
	public void testDuplicatedKeyOnCreate() throws SQLException {
		changelogStore.itemCreated(LogEntry.create(10792, "ED36D298-DEB5-4376-85E2-538FFAE77FD1", "extId1", "author",
				"junit-testChangeset", 10792, 0L));
		changelogStore.itemCreated(LogEntry.create(10792, "ED36D298-DEB5-4376-85E2-538FFAE77FD1", "extId1", "author",
				"junit-testChangeset", 10793, 54646546L));

		ItemFlagFilter allFilter = ItemFlagFilter.all();
		ContainerChangeset<ItemVersion> changeset = changelogStore.changesetById(0, 7, allFilter);
		assertEquals(10792, changeset.version);
		assertEquals(1, changeset.created.size());
		assertEquals(0, changeset.updated.size());
		assertEquals(0, changeset.deleted.size());
		assertTrue(changeset.created.stream().anyMatch(v -> v.id == 10792));
	}

	@Test
	public void testDuplicatedKeyOnUpdate() throws SQLException {
		changelogStore.itemCreated(LogEntry.create(10792, "ED36D298-DEB5-4376-85E2-538FFAE77FD1", "extId1", "author",
				"junit-testChangeset", 10792, 0L));
		changelogStore.itemUpdated(LogEntry.create(10792, "ED36D298-DEB5-4376-85E2-538FFAE77FD1", "extId1", "author",
				"junit-testChangeset", 10793, 54646546L));

		ItemFlagFilter allFilter = ItemFlagFilter.all();
		ContainerChangeset<ItemVersion> changeset = changelogStore.changesetById(0, 7, allFilter);
		assertEquals(10792, changeset.version);
		assertEquals(1, changeset.created.size());
		assertEquals(0, changeset.updated.size());
		assertEquals(0, changeset.deleted.size());
		assertTrue(changeset.created.stream().anyMatch(v -> v.id == 10792));
	}

	@Test
	public void testDuplicatedKeyOnDelete() throws SQLException {
		changelogStore.itemCreated(LogEntry.create(10792, "ED36D298-DEB5-4376-85E2-538FFAE77FD1", "extId1", "author",
				"junit-testChangeset", 10792, 0L));
		changelogStore.itemDeleted(LogEntry.create(10792, "ED36D298-DEB5-4376-85E2-538FFAE77FD1", "extId1", "author",
				"junit-testChangeset", 10793, 54646546L));

		ItemFlagFilter allFilter = ItemFlagFilter.all();
		ContainerChangeset<ItemVersion> changeset = changelogStore.changesetById(0, 7, allFilter);
		assertEquals(10792, changeset.version);
		assertEquals(1, changeset.created.size());
		assertEquals(0, changeset.updated.size());
		assertEquals(0, changeset.deleted.size());
	}

	private void assertEntryEquals(long itemId, long version, String itemUid, String itemExtId,
			ChangeLogEntry.Type type, String author, String origin, ChangeLogEntry entry) {
		assertEntryEquals(version, itemUid, type, author, entry);
		assertEquals(itemExtId, entry.itemExtId);
		assertEquals(origin, entry.origin);
		assertEquals(itemId, entry.internalId);
	}

	private void assertEntryEquals(long version, String itemUid, ChangeLogEntry.Type type, String author,
			ChangeLogEntry entry) {
		assertEquals(version, entry.version);
		assertEquals(itemUid, entry.itemUid);
		assertEquals(type, entry.type);
		assertEquals(author, entry.author);
	}
}
