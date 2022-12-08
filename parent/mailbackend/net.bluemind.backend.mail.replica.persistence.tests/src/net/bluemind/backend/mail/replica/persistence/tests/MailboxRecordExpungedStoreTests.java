/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxRecordExpunged;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordExpungedStore;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordStore;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.server.api.Server;
import net.bluemind.system.state.RunningState;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class MailboxRecordExpungedStoreTests {
	protected String partition;
	protected String user1Uid;
	protected String user1MboxRoot;
	protected String domainUid = "test" + System.currentTimeMillis() + ".lab";

	private ItemStore itemStore;
	private MailboxRecordStore boxRecordStore;
	private MailboxRecordExpungedStore expungedRecordStore;
	private Container subtreeContainer;
	private Container container;

	@BeforeClass
	public static void sysprop() {
		System.setProperty("node.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
		System.setProperty("imap.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
		System.setProperty("imap.port", "1144");
	}

	protected IServiceProvider provider(String userUid) {
		SecurityContext secCtx = new SecurityContext("sid-" + userUid, userUid, Collections.emptyList(),
				Collections.emptyList(), domainUid);
		return ServerSideServiceProvider.getProvider(secCtx);
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		Server pipo = new Server();
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;
		pipo.tags = Collections.singletonList("mail/imap");

		VertxPlatform.spawnBlocking(25, TimeUnit.SECONDS);
		partition = "dataloc__" + domainUid.replace('.', '_');
		DataSource datasource = JdbcTestHelper.getInstance().getMailboxDataDataSource();
		JdbcActivator.getInstance().addMailboxDataSource("dataloc", datasource);
		PopulateHelper.initGlobalVirt(pipo);
		PopulateHelper.addDomain(domainUid, Routing.internal);
		user1Uid = PopulateHelper.addUser("u1-" + System.currentTimeMillis(), domainUid, Routing.internal);
		user1MboxRoot = "user." + user1Uid.replace('.', '^');
		assertNotNull(user1Uid);
		StateContext.setInternalState(new RunningState());
		SecurityContext securityContext = SecurityContext.ANONYMOUS;
		ContainerStore containerStore = new ContainerStore(null,
				JdbcTestHelper.getInstance().getMailboxDataDataSource(), securityContext);

		ContainerStore containerHome = new ContainerStore(null, JdbcTestHelper.getInstance().getMailboxDataDataSource(),
				SecurityContext.SYSTEM);
		IMailboxFolders user1MailboxFolderService = provider(user1Uid).instance(IMailboxFolders.class, partition,
				user1MboxRoot);
		String user1InboxUid = user1MailboxFolderService.byName("INBOX").uid;
		container = containerHome.get(IMailReplicaUids.mboxRecords(user1InboxUid));

		assertNotNull(container);

		itemStore = new ItemStore(JdbcTestHelper.getInstance().getMailboxDataDataSource(), container, securityContext);
		IMailboxes mailboxesApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IMailboxes.class, container.domainUid);
		ItemValue<Mailbox> mailbox = mailboxesApi.getComplete(container.owner);
		if (mailbox == null) {
			throw ServerFault.notFound("mailbox of " + container.owner + " not found");
		}
		String subtreeContainerUid = IMailReplicaUids.subtreeUid(container.domainUid, mailbox);
		subtreeContainer = containerStore.get(subtreeContainerUid);
		if (subtreeContainer == null) {
			throw ServerFault.notFound("subtree " + subtreeContainerUid);
		}

		boxRecordStore = new MailboxRecordStore(JdbcTestHelper.getInstance().getMailboxDataDataSource(), container,
				subtreeContainer);
		boxRecordStore.deleteAll();
		expungedRecordStore = new MailboxRecordExpungedStore(JdbcTestHelper.getInstance().getMailboxDataDataSource(),
				container, subtreeContainer);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testGetExpiredMessagesOnEmptyTableShouldReturnEmptyList() throws Exception {
		assertEquals(0, expungedRecordStore.getExpiredItems(7).size());
	}

	@Test
	public void testGetExpiredMessagesOnNonExpiredItemsShouldReturnEmptyList() throws Exception {
		MailboxRecord mb = simpleRecord(10);
		String uniqueId = "rec" + System.currentTimeMillis();
		itemStore.create(Item.create(uniqueId, null));
		Item it = itemStore.get(uniqueId);
		boxRecordStore.create(it, mb);

		mb = simpleRecord(20);
		uniqueId = "rec" + System.currentTimeMillis();
		itemStore.create(Item.create(uniqueId, null));
		it = itemStore.get(uniqueId);
		boxRecordStore.create(it, mb);

		assertEquals(0, expungedRecordStore.getExpiredItems(7).size());
	}

	@Test
	public void testGetExpiredMessagesOnOldUnexpiredItemsShouldReturnEmptyList() throws Exception {
		MailboxRecord mb = simpleRecord(10);
		String uniqueId = "rec" + System.currentTimeMillis();
		mb.lastUpdated = adaptDate(10);
		itemStore.create(Item.create(uniqueId, null));
		Item it = itemStore.get(uniqueId);
		boxRecordStore.create(it, mb);

		mb = simpleRecord(20);
		uniqueId = "rec" + System.currentTimeMillis();
		mb.lastUpdated = adaptDate(6);
		itemStore.create(Item.create(uniqueId, null));
		it = itemStore.get(uniqueId);
		boxRecordStore.create(it, mb);

		assertEquals(0, expungedRecordStore.getExpiredItems(7).size());
	}

	private List<Item> insertExpungedMessages() throws SQLException {
		List<Item> items = new ArrayList<>();
		// insert with expire date -10 days
		Item it = insertExpungedExpiredMessage(10, 10, false);
		items.add(it);
		// update with expire date -8 days
		it = insertExpungedExpiredMessage(20, 8, true);
		items.add(it);
		// insert with expire date -3 days
		it = insertExpungedExpiredMessage(30, 3, false);
		items.add(it);
		return items;
	}

	@Test
	public void testGetExpiredMessagesOnExpiredItems() throws Exception {
		insertExpungedMessages();

		List<MailboxRecordExpunged> expiredItems = expungedRecordStore.getExpiredItems(5);
		assertEquals(2, expiredItems.size());
	}

	@Test
	public void testCheckExpungedQueue() throws Exception {
		insertExpungedMessages();

		List<MailboxRecordExpunged> allExpunged = expungedRecordStore.fetch();
		assertEquals(3, allExpunged.size());
	}

	@Test
	public void testDeleteOnlyExpiredExpunged() throws SQLException {
		insertExpungedMessages();

		List<MailboxRecordExpunged> expiredItems = expungedRecordStore.getExpiredItems(5);
		assertEquals(2, expiredItems.size());

		Map<Integer, List<Long>> mapOfExpunged = expiredItems.stream().collect(Collectors.groupingBy(
				MailboxRecordExpunged::containerId, Collectors.mapping(rec -> rec.imapUid, Collectors.toList())));

		mapOfExpunged.forEach((k, v) -> {
			try {
				expungedRecordStore.deleteExpunged(k, v);
			} catch (SQLException e) {
				fail();
			}
		});

		Long count = expungedRecordStore.count();
		assertEquals(1, count.intValue());
	}

	@Test
	public void testCount() throws SQLException {
		insertExpungedMessages();
		Long count = expungedRecordStore.count();
		assertEquals(3L, count.longValue());
		expungedRecordStore.deleteAll();
	}

	@Test
	public void testFetch() throws SQLException {
		List<Item> items = insertExpungedMessages();
		List<MailboxRecordExpunged> fetch = expungedRecordStore.fetch();
		assertEquals(items.size(), fetch.size());
		expungedRecordStore.deleteAll();
	}

	@Test
	public void testGet() throws SQLException {
		List<Item> items = insertExpungedMessages();
		long id = items.get(0).id;
		MailboxRecordExpunged item = expungedRecordStore.get(id);
		assertEquals(id, item.itemId.longValue());
		expungedRecordStore.deleteAll();
	}

	public void testStore() throws SQLException {
		MailboxRecordExpunged record = new MailboxRecordExpunged();
		Date now = new Date();
		record.itemId = 2L;
		record.imapUid = 3L;
		record.created = now;
		expungedRecordStore.store(record);

		MailboxRecordExpunged item = expungedRecordStore.get(record.itemId);
		assertNotNull(item);
		assertEquals(container.id, item.containerId.longValue());
		assertEquals(subtreeContainer.id, item.subtreeId.longValue());
		assertEquals(2L, item.itemId.longValue());
		assertEquals(3L, item.imapUid.longValue());
		assertEquals(now, item.created);
	}

	private Item insertExpungedExpiredMessage(long imapUid, int daysBeforeNow, boolean update) throws SQLException {
		MailboxRecord mb = simpleRecord(imapUid);
		String uniqueId = "rec" + System.currentTimeMillis();
		if (daysBeforeNow > 0) {
			mb.lastUpdated = adaptDate(daysBeforeNow);
		}
		itemStore.create(Item.create(uniqueId, null));
		Item it = itemStore.get(uniqueId);

		if (update && daysBeforeNow > 0) {
			boxRecordStore.create(it, mb);
			mb.internalFlags = Arrays.asList(MailboxRecord.InternalFlag.expunged);
			boxRecordStore.update(it, mb);

			try (Connection con = JdbcTestHelper.getInstance().getMailboxDataDataSource().getConnection();
					PreparedStatement stm = con
							.prepareStatement("UPDATE q_mailbox_record_expunged set created = ? WHERE item_id = ?;")) {
				stm.setDate(1, adaptSqlDate(daysBeforeNow));
				stm.setLong(2, it.id);
				int updated = stm.executeUpdate();
				assertEquals(1, updated);
			}
		} else {
			mb.internalFlags = Arrays.asList(MailboxRecord.InternalFlag.expunged);
			boxRecordStore.create(it, mb);
		}
		return it;
	}

	private Date adaptDate(int daysBeforeNow) {
		LocalDate localDate = LocalDate.now();
		LocalDate adapted = localDate.minusDays(daysBeforeNow);
		return Date.from(adapted.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	private java.sql.Date adaptSqlDate(int daysBeforeNow) {
		LocalDate localDate = LocalDate.now();
		LocalDate adapted = localDate.minusDays(daysBeforeNow);
		return java.sql.Date.valueOf(adapted);
	}

	private MailboxRecord simpleRecord(long imapUid) {
		MailboxRecord record = new MailboxRecord();
		record.imapUid = imapUid;
		record.messageBody = UUID.randomUUID().toString().replace("-", "");
		record.internalDate = new Date();
		record.lastUpdated = new Date();
		record.flags = Arrays.asList(MailboxItemFlag.System.Seen.value());
		return record;
	}

}
