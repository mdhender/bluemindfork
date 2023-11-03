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
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.ImapBinding;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxRecord.InternalFlag;
import net.bluemind.backend.mail.replica.api.RawImapBinding;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordStore;
import net.bluemind.backend.mail.replica.persistence.MessageBodyStore;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
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

public class MailboxRecordStoreTests {
	protected String partition;
	protected String user1Uid;
	protected String user1MboxRoot;
	protected String domainUid = "test" + System.currentTimeMillis() + ".lab";

	private ItemStore itemStore;
	private MailboxRecordStore boxRecordStore;
	private MessageBodyStore bodyStore;

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
		partition = CyrusPartition.forServerAndDomain(pipo.ip, domainUid).name;
		DataSource datasource = JdbcTestHelper.getInstance().getMailboxDataDataSource();
		JdbcActivator.getInstance().addMailboxDataSource(pipo.ip, datasource);
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
		Container container = containerHome.get(IMailReplicaUids.mboxRecords(user1InboxUid));

		assertNotNull(container);

		itemStore = new ItemStore(JdbcTestHelper.getInstance().getMailboxDataDataSource(), container, securityContext);
		IMailboxes mailboxesApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IMailboxes.class, container.domainUid);
		ItemValue<Mailbox> mailbox = mailboxesApi.getComplete(container.owner);
		if (mailbox == null) {
			throw ServerFault.notFound("mailbox of " + container.owner + " not found");
		}
		String subtreeContainerUid = IMailReplicaUids.subtreeUid(container.domainUid, mailbox);
		Container subtreeContainer = containerStore.get(subtreeContainerUid);
		if (subtreeContainer == null) {
			throw ServerFault.notFound("subtree " + subtreeContainerUid);
		}

		boxRecordStore = new MailboxRecordStore(JdbcTestHelper.getInstance().getMailboxDataDataSource(), container,
				subtreeContainer);
		boxRecordStore.deleteAll();
		bodyStore = new MessageBodyStore(JdbcTestHelper.getInstance().getMailboxDataDataSource());
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testFlagsMatch() throws SQLException {
		MailboxRecord mb = simpleRecord();
		mb.flags = Arrays.asList(MailboxItemFlag.System.Seen.value(), MailboxItemFlag.System.Deleted.value());

		String uniqueId = "rec" + System.currentTimeMillis();
		Item it = Item.create(uniqueId, null);
		it.flags = Arrays.asList(ItemFlag.Seen, ItemFlag.Deleted);
		itemStore.create(it);
		it = itemStore.get(uniqueId);

		boxRecordStore.create(it, mb);
		MailboxRecord reloaded = boxRecordStore.get(it);
		assertNotNull(reloaded);
		assertNotNull(reloaded.flags);
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
		assertNotNull(reloaded.flags);
		assertEquals(mb.messageBody, reloaded.messageBody);

		List<MailboxRecord> multiple = boxRecordStore.getMultiple(Arrays.asList(it));
		assertTrue(multiple.size() == 1);

		System.out.println("beforeFlags: " + reloaded.flags);
		reloaded.internalFlags = EnumSet.of(InternalFlag.expunged);
		reloaded.flags = Arrays.asList(MailboxItemFlag.System.Answered.value(), new MailboxItemFlag("$john"),
				new MailboxItemFlag("$bang"));
		boxRecordStore.update(it, reloaded);
		MailboxRecord reloaded2 = boxRecordStore.get(it);
		assertEquals(reloaded.flags, reloaded2.flags);
		assertEquals(reloaded.internalFlags, reloaded2.internalFlags);
		System.out.println("afterFlags: " + reloaded2.flags + ", internal: " + reloaded2.internalFlags);

		List<ImapBinding> asBindings = boxRecordStore.bindings(Arrays.asList(it.id));
		assertNotNull(asBindings);
		assertFalse(asBindings.isEmpty());

		List<String> labels = boxRecordStore.labels();
		System.err.println("labels: " + labels);
		assertEquals("an expunged record should not produce any label", labels.size(), 0);

		// we check for 0 & empty list because the bodies are not created
		asBindings = boxRecordStore.havingBodyVersionLowerThan(0);
		assertNotNull(asBindings);
		assertTrue(asBindings.isEmpty());

		List<RawImapBinding> set = boxRecordStore.imapIdset("1:*", ItemFlagFilter.all());
		assertEquals(0, set.size());
		System.err.println("set: " + set);
		set = boxRecordStore.imapIdset("42,43", ItemFlagFilter.all());
		assertEquals(0, set.size());
		set = boxRecordStore.imapIdset("42", ItemFlagFilter.create().mustNot(ItemFlag.Deleted));
		assertEquals(0, set.size());

		boxRecordStore.delete(it);
		reloaded = boxRecordStore.get(it);
		assertNull(reloaded);

	}

	@Test
	public void testRecentItems() throws SQLException {
		MailboxRecord mb = simpleRecord();
		MessageBody body = body(mb.messageBody, new Date());
		bodyStore.store(body);

		String uniqueId = "rec" + System.currentTimeMillis();
		itemStore.create(Item.create(uniqueId, null));
		Item it = itemStore.get(uniqueId);
		boxRecordStore.create(it, mb);
		MailboxRecord reloaded = boxRecordStore.get(it);
		assertNotNull(reloaded);
		assertNotNull(reloaded.flags);

		List<ImapBinding> existing = boxRecordStore.recentItems(new Date(0));
		Optional<ImapBinding> optRec = existing.stream().filter(ib -> ib.itemId == it.id).findAny();
		assertTrue(optRec.isPresent());

		Date after = new Date(it.created.getTime() + 1000);
		existing = boxRecordStore.recentItems(after);
		assertTrue(existing.isEmpty());
	}

	@Test
	public void testLabelAdd() throws SQLException {
		assertTrue("labels should be empty", boxRecordStore.labels().isEmpty());
		for (int i = 0; i < 5; i++) {
			MailboxRecord mb = simpleRecord();
			mb.flags = Arrays.asList(MailboxItemFlag.of("$Junk", 0));
			MessageBody body = body(mb.messageBody, new Date());
			bodyStore.store(body);

			String uniqueId = "rec" + System.nanoTime();
			itemStore.create(Item.create(uniqueId, null));
			Item it = itemStore.get(uniqueId);
			boxRecordStore.create(it, mb);
		}
		assertEquals("labels should have $Junk once only", boxRecordStore.labels().size(), 1);
		assertEquals("labels should have $Junk once only", boxRecordStore.labels().get(0), "$Junk");
	}

	@Test
	public void testLabelRemove() throws SQLException {
		assertTrue("labels should be empty", boxRecordStore.labels().isEmpty());
		List<Item> mailboxRecordItems = new ArrayList<>();
		for (int i = 0; i < 2; i++) {
			MailboxRecord mb = simpleRecord();
			mb.flags = Arrays.asList(MailboxItemFlag.of("$Junk", 0));
			MessageBody body = body(mb.messageBody, new Date());
			bodyStore.store(body);

			String uniqueId = "rec" + System.nanoTime();
			itemStore.create(Item.create(uniqueId, null));
			Item it = itemStore.get(uniqueId);
			boxRecordStore.create(it, mb);
			mailboxRecordItems.add(it);
		}
		assertEquals("labels should have $Junk once only", boxRecordStore.labels().size(), 1);
		assertEquals("labels should have $Junk once only", boxRecordStore.labels().get(0), "$Junk");

		// delete all records: label should be there
		MailboxRecord rec1 = boxRecordStore.get(mailboxRecordItems.get(0));
		rec1.flags = Lists.newCopyOnWriteArrayList(rec1.flags);
		rec1.flags.add(MailboxItemFlag.System.Deleted.value());
		MailboxRecord rec2 = boxRecordStore.get(mailboxRecordItems.get(1));
		rec2.flags = Lists.newCopyOnWriteArrayList(rec2.flags);
		rec2.flags.add(MailboxItemFlag.System.Deleted.value());
		assertEquals("labels should have $Junk once only", boxRecordStore.labels().size(), 1);
		assertEquals("labels should have $Junk once only", boxRecordStore.labels().get(0), "$Junk");

		// expunge one record: label should be there
		rec1 = boxRecordStore.get(mailboxRecordItems.get(0));
		rec1.internalFlags = EnumSet.of(InternalFlag.expunged);

		boxRecordStore.update(mailboxRecordItems.get(0), rec1);
		assertEquals("labels should have $Junk once only", boxRecordStore.labels().size(), 1);
		assertEquals("labels should have $Junk once only", boxRecordStore.labels().get(0), "$Junk");

		// expunge every record: label should remain as its permanent
		rec2 = boxRecordStore.get(mailboxRecordItems.get(1));
		rec2.internalFlags = EnumSet.of(InternalFlag.expunged);
		boxRecordStore.update(mailboxRecordItems.get(1), rec2);
		assertFalse("labels should not be empty", boxRecordStore.labels().isEmpty());
	}

	private MailboxRecord simpleRecord() {
		MailboxRecord record = new MailboxRecord();
		record.imapUid = 42;
		record.messageBody = UUID.randomUUID().toString().replace("-", "");
		record.internalDate = new Date();
		record.lastUpdated = new Date();
		record.flags = Arrays.asList(MailboxItemFlag.System.Seen.value());
		return record;
	}

	private MessageBody body(String guid, Date d) {
		MessageBody mb = new MessageBody();
		mb.date = d;
		mb.guid = guid;
		return mb;
	}

}
