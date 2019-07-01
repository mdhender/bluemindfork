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
package net.bluemind.core.container.persistance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

import net.bluemind.core.container.model.ChangeLogEntry;
import net.bluemind.core.container.model.ChangeLogEntry.Type;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.persistance.ChangelogStore.LogEntry;
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
	public void testAllItemsDeleted() throws Exception {
		itemStore.create(Item.create("t1", "ext1"));
		itemStore.create(Item.create("t2", "ext2"));
		itemStore.create(Item.create("t3", "ext3"));
		itemStore.create(Item.create("t4", "ext4"));
		itemStore.create(Item.create("t5", "ext5"));
		ContainerChangeset<String> c = changelogStore.changeset(0, Long.MAX_VALUE);
		long lastVersion = c.version;
		try {
			changelogStore.allItemsDeleted("toto", "titi");
		} catch (SQLException e1) {
			logger.error(e1.getMessage(), e1);
			fail(e1.getMessage());
		}
		ContainerChangelog changelog = changelogStore.changelog(c.version, Long.MAX_VALUE);

		List<Long> versions = changelog.entries.stream().map(e -> e.version).sorted().distinct()
				.collect(Collectors.toList());
		// each entry have different version
		assertEquals(5, versions.size());
		// version is > to last known version
		assertTrue(versions.stream().allMatch(v -> v > lastVersion));
		assertTrue(changelog.entries.stream().allMatch(e -> e.type == ChangeLogEntry.Type.Deleted));
		assertTrue(changelog.entries.stream().allMatch(e -> e.origin.equals("titi")));
		assertTrue(changelog.entries.stream().allMatch(e -> e.author.equals("toto")));
		assertEquals(changelog.entries.stream().map(e -> e.itemUid).collect(Collectors.toSet()),
				ImmutableSet.of("t1", "t2", "t3", "t4", "t5"));
	}

	@Test
	public void testChangelog() throws SQLException {
		changelogStore.itemCreated(LogEntry.create(1, "test", "extId1", "author", "junit-testChangelog", 42, 0L));
		changelogStore.itemUpdated(LogEntry.create(2, "test2", "extId2", "author2", "junit-testChangelog2", 43, 0L));
		changelogStore.itemDeleted(LogEntry.create(3, "test", "extId1", "author", "junit-testChangelog", 42, 0L));

		ContainerChangelog changelog = changelogStore.changelog(0, 3);
		assertEquals(3, changelog.entries.size());

		assertEntryEquals(42, 1, "test", "extId1", ChangeLogEntry.Type.Created, "author", "junit-testChangelog",
				changelog.entries.get(0));
		assertEntryEquals(43, 2, "test2", "extId2", ChangeLogEntry.Type.Updated, "author2", "junit-testChangelog2",
				changelog.entries.get(1));
		assertEntryEquals(3, "test", ChangeLogEntry.Type.Deleted, "author", changelog.entries.get(2));

	}

	@Test
	public void testItemChangelog() throws SQLException {
		changelogStore.itemCreated(LogEntry.create(1, "test", "extId1", "author", "junit-testChangelog", 42, 0L));
		changelogStore.itemUpdated(LogEntry.create(2, "test2", "extId2", "author2", "junit-testChangelog2", 43, 0L));
		changelogStore.itemDeleted(LogEntry.create(3, "test", "extId1", "author", "junit-testChangelog", 42, 0L));

		ContainerChangelog changelog = changelogStore.changelog(0, 3);
		assertEquals(3, changelog.entries.size());

		ItemChangelog itemChangelog = changelogStore.itemChangelog("test", 0, Long.MAX_VALUE);
		assertEquals(2, itemChangelog.entries.size());

		assertEntryEquals(42, 1, "test", "extId1", ChangeLogEntry.Type.Created, "author", "junit-testChangelog",
				itemChangelog.entries.get(0));
		assertEntryEquals(3, "test", ChangeLogEntry.Type.Deleted, "author", itemChangelog.entries.get(1));

		itemChangelog = changelogStore.itemChangelog("test2", 0, Long.MAX_VALUE);
		assertEquals(1, itemChangelog.entries.size());

		assertEntryEquals(43, 2, "test2", "extId2", ChangeLogEntry.Type.Updated, "author2", "junit-testChangelog2",
				itemChangelog.entries.get(0));
	}

	@Test
	public void testChangeset() throws SQLException {
		changelogStore.itemCreated(LogEntry.create(1, "test", "extId1", "author", "junit-testChangeset", 42, 0L));
		changelogStore.itemUpdated(LogEntry.create(2, "test", "extId1", "author", "junit-testChangeset", 42, 0L));
		changelogStore.itemUpdated(LogEntry.create(3, "test2", "extId2", "author2", "junit-testChangeset", 43, 0L));
		changelogStore.itemDeleted(LogEntry.create(4, "test", "extId1", "author", "junit-testChangeset", 42, 0L));

		ContainerChangeset<String> changeset = changelogStore.changeset(0, Long.MAX_VALUE);
		assertEquals(4, changeset.version);
		assertEquals(0, changeset.created.size());
		assertEquals(1, changeset.updated.size());
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
		assertEquals(0, changeset.created.size());
		assertEquals(1, changeset.updated.size());
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
		changelogStore.itemUpdated(LogEntry.create(2, "test", "extId1", "author", "junit-testChangeset", 42, 0L));
		changelogStore.itemUpdated(LogEntry.create(3, "test2", "extId2", "author2", "junit-testChangeset", 43, 0L));
		changelogStore.itemDeleted(LogEntry.create(4, "test", "extId1", "author", "junit-testChangeset", 42, 0L));

		ItemFlagFilter allFilter = ItemFlagFilter.all();
		ContainerChangeset<ItemVersion> changeset = changelogStore.changesetById(0, 4, allFilter);
		assertEquals(4, changeset.version);
		assertEquals(0, changeset.created.size());
		assertEquals(1, changeset.updated.size());
		assertEquals(0, changeset.deleted.size());
		assertEquals(43, changeset.updated.get(0).id);

		changeset = changelogStore.changesetById(1, 4, allFilter);
		assertEquals(4, changeset.version);
		assertEquals(0, changeset.created.size());
		assertEquals(1, changeset.updated.size());
		assertEquals(1, changeset.deleted.size());

		changeset = changelogStore.changesetById(0, 4, allFilter);
		assertEquals(4, changeset.version);
		assertEquals(0, changeset.created.size());
		assertEquals(1, changeset.updated.size());
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
