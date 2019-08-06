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
package net.bluemind.backend.mail.replica.persistence.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.backend.cyrus.replication.testhelper.CyrusGUID;
import net.bluemind.backend.cyrus.replication.testhelper.MailboxUniqueId;
import net.bluemind.backend.mail.api.MailboxItem.SystemFlag;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.ImapBinding;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxRecord.InternalFlag;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordStore;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordStore.MailboxRecordItemV;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.persistance.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;

public class MailboxRecordStoreTests {

	private ItemStore itemStore;
	private MailboxRecordStore boxRecordStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerHome = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				securityContext);
		String boxUniqueId = MailboxUniqueId.random();
		String containerId = IMailReplicaUids.mboxRecords(boxUniqueId);
		Container container = Container.create(containerId, IMailReplicaUids.MAILBOX_RECORDS, "test", "me", true);
		container = containerHome.create(container);

		assertNotNull(container);

		itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), container, securityContext);
		boxRecordStore = new MailboxRecordStore(JdbcTestHelper.getInstance().getDataSource(), container);
		boxRecordStore.deleteAll();
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testFlagsMatch() throws SQLException {
		MailboxRecord mb = simpleRecord();
		mb.systemFlags = Arrays.asList(SystemFlag.seen, SystemFlag.deleted);

		String uniqueId = "rec" + System.currentTimeMillis();
		Item it = Item.create(uniqueId, null);
		it.flags = Arrays.asList(ItemFlag.Seen, ItemFlag.Deleted);
		itemStore.create(it);
		it = itemStore.get(uniqueId);

		boxRecordStore.create(it, mb);
		MailboxRecord reloaded = boxRecordStore.get(it);
		assertNotNull(reloaded);
		assertNotNull(reloaded.systemFlags);
		List<ImapBinding> unread = boxRecordStore.unreadItems();
		assertTrue(unread.isEmpty());

		itemStore.update(it.uid, null, Collections.emptyList());
		unread = boxRecordStore.unreadItems();
		assertFalse(unread.isEmpty());
	}

	@Test
	public void testCrudSimple() throws SQLException {
		MailboxRecord mb = simpleRecord();
		String uniqueId = "rec" + System.currentTimeMillis();
		itemStore.create(Item.create(uniqueId, null));
		Item it = itemStore.get(uniqueId);
		boxRecordStore.create(it, mb);
		MailboxRecord reloaded = boxRecordStore.get(it);
		assertNotNull(reloaded);
		assertNotNull(reloaded.systemFlags);

		List<MailboxRecord> multiple = boxRecordStore.getMultiple(Arrays.asList(it));
		assertTrue(multiple.size() == 1);

		System.out.println("beforeFlags: " + reloaded.systemFlags);
		reloaded.systemFlags = EnumSet.of(SystemFlag.answered);
		reloaded.internalFlags = EnumSet.of(InternalFlag.expunged);
		reloaded.otherFlags = Arrays.asList("$john", "$bang");
		boxRecordStore.update(it, reloaded);
		MailboxRecord reloaded2 = boxRecordStore.get(it);
		assertEquals(reloaded.systemFlags, reloaded2.systemFlags);
		assertEquals(reloaded.internalFlags, reloaded2.internalFlags);
		System.out.println("afterFlags: " + reloaded2.systemFlags + ", internal: " + reloaded2.internalFlags);

		List<ImapBinding> asBindings = boxRecordStore.bindings(Arrays.asList(it.id));
		assertNotNull(asBindings);
		assertFalse(asBindings.isEmpty());

		// we check for 0 & empty list because the bodies are not created
		asBindings = boxRecordStore.havingBodyVersionLowerThan(0);
		assertNotNull(asBindings);
		assertTrue(asBindings.isEmpty());

		boxRecordStore.delete(it);
		reloaded = boxRecordStore.get(it);
		assertNull(reloaded);

	}

	@Test
	public void testRecentItems() throws SQLException {
		MailboxRecord mb = simpleRecord();
		String uniqueId = "rec" + System.currentTimeMillis();
		itemStore.create(Item.create(uniqueId, null));
		Item it = itemStore.get(uniqueId);
		boxRecordStore.create(it, mb);
		MailboxRecord reloaded = boxRecordStore.get(it);
		assertNotNull(reloaded);
		assertNotNull(reloaded.systemFlags);

		List<ImapBinding> existing = boxRecordStore.recentItems(new Date(0));
		Optional<ImapBinding> optRec = existing.stream().filter(ib -> ib.itemId == it.id).findAny();
		assertTrue(optRec.isPresent());

		Date after = new Date(it.created.getTime() + 1000);
		existing = boxRecordStore.recentItems(after);
		assertTrue(existing.isEmpty());
	}

	@Test
	public void testGetExpiredMessagesOnEmptyTableShouldReturnEmptyList() throws Exception {
		assertEquals(0, boxRecordStore.getExpiredItems(7).size());
	}

	@Test
	public void testGetExpiredMessagesOnNonExpiredItemsShouldReturnEmptyList() throws Exception {
		MailboxRecord mb = simpleRecord();
		String uniqueId = "rec" + System.currentTimeMillis();
		itemStore.create(Item.create(uniqueId, null));
		Item it = itemStore.get(uniqueId);
		boxRecordStore.create(it, mb);

		mb = simpleRecord();
		uniqueId = "rec" + System.currentTimeMillis();
		itemStore.create(Item.create(uniqueId, null));
		it = itemStore.get(uniqueId);
		boxRecordStore.create(it, mb);

		assertEquals(0, boxRecordStore.getExpiredItems(7).size());
	}

	@Test
	public void testGetExpiredMessagesOnOldUnexpiredItemsShouldReturnEmptyList() throws Exception {
		MailboxRecord mb = simpleRecord();
		String uniqueId = "rec" + System.currentTimeMillis();
		mb.lastUpdated = adaptDate(10);
		itemStore.create(Item.create(uniqueId, null));
		Item it = itemStore.get(uniqueId);
		boxRecordStore.create(it, mb);

		mb = simpleRecord();
		uniqueId = "rec" + System.currentTimeMillis();
		mb.lastUpdated = adaptDate(6);
		itemStore.create(Item.create(uniqueId, null));
		it = itemStore.get(uniqueId);
		boxRecordStore.create(it, mb);

		assertEquals(0, boxRecordStore.getExpiredItems(7).size());
	}

	@Test
	public void testGetExpiredMessagesOnExpiredItems() throws Exception {
		List<Long> uids = new ArrayList<>();

		MailboxRecord mb = simpleRecord();
		String uniqueId = "rec" + System.currentTimeMillis();
		mb.imapUid = 50;
		mb.internalFlags = Arrays.asList(MailboxRecord.InternalFlag.expunged);
		mb.lastUpdated = adaptDate(10);
		itemStore.create(Item.create(uniqueId, null));
		Item it = itemStore.get(uniqueId);
		boxRecordStore.create(it, mb);

		uids.add(mb.imapUid);

		mb = simpleRecord();
		uniqueId = "rec" + System.currentTimeMillis();
		mb.lastUpdated = adaptDate(8);
		mb.internalFlags = Arrays.asList(MailboxRecord.InternalFlag.expunged);
		mb.imapUid = 51;
		itemStore.create(Item.create(uniqueId, null));
		it = itemStore.get(uniqueId);
		boxRecordStore.create(it, mb);

		uids.add(mb.imapUid);

		mb = simpleRecord();
		uniqueId = "rec" + System.currentTimeMillis();
		mb.lastUpdated = adaptDate(6);
		mb.internalFlags = Arrays.asList(MailboxRecord.InternalFlag.expunged);
		itemStore.create(Item.create(uniqueId, null));
		it = itemStore.get(uniqueId);
		boxRecordStore.create(it, mb);

		List<MailboxRecordItemV> expiredItems = boxRecordStore.getExpiredItems(7);

		assertEquals(2, expiredItems.size());

		for (MailboxRecordItemV mailboxRecordItemV : expiredItems) {
			assertTrue(uids.contains(mailboxRecordItemV.item.value.imapUid));
		}
	}

	private Date adaptDate(int daysBeforeNow) {
		LocalDate localDate = LocalDate.now();
		LocalDate adapted = localDate.minusDays(daysBeforeNow);
		return adapted.toDate();
	}

	private MailboxRecord simpleRecord() {
		MailboxRecord record = new MailboxRecord();
		record.imapUid = 42;
		record.messageBody = CyrusGUID.randomGuid();
		record.internalDate = new Date();
		record.lastUpdated = new Date();
		record.systemFlags = EnumSet.of(SystemFlag.seen);
		return record;
	}

}
