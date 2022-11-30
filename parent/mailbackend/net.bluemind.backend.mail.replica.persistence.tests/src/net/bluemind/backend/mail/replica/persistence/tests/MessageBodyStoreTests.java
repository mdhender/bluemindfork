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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.DispositionType;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.Header;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordStore;
import net.bluemind.backend.mail.replica.persistence.MessageBodyStore;
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
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.server.api.Server;
import net.bluemind.system.state.RunningState;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class MessageBodyStoreTests {
	protected String partition;
	protected String user1Uid;
	protected String user1MboxRoot;
	protected String domainUid = "test" + System.currentTimeMillis() + ".lab";
	protected DataSource datasource;
	private MessageBodyStore bodyStore;

	@BeforeClass
	public static void sysprop() {
		System.setProperty("node.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
		System.setProperty("imap.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
		System.setProperty("imap.port", "1144");
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		Server pipo = new Server();
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;
		pipo.tags = Collections.singletonList("mail/imap");

		VertxPlatform.spawnBlocking(25, TimeUnit.SECONDS);
		partition = CyrusPartition.forServerAndDomain(pipo.ip, domainUid).name;
		datasource = JdbcTestHelper.getInstance().getMailboxDataDataSource();
		JdbcActivator.getInstance().addMailboxDataSource("dataloc", datasource);
		PopulateHelper.initGlobalVirt(pipo);
		PopulateHelper.addDomain(domainUid, Routing.internal);
		user1Uid = PopulateHelper.addUser("u1-" + System.currentTimeMillis(), domainUid, Routing.internal);
		user1MboxRoot = "user." + user1Uid.replace('.', '^');
		assertNotNull(user1Uid);
		StateContext.setInternalState(new RunningState());
		bodyStore = new MessageBodyStore(JdbcTestHelper.getInstance().getMailboxDataDataSource());
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testSchemaIsWellRegistered() {
		assertNotNull(JdbcTestHelper.getInstance().getDbSchemaService().getSchemaDescriptor("mailreplica-schema"));
	}

	@Test
	public void testExisting() throws SQLException {
		String guid = UUID.randomUUID().toString().replace("-", "");
		MessageBody mb = simpleTextBody(guid);
		bodyStore.store(mb);

		assertTrue(bodyStore.exists(guid));
		List<String> existing = bodyStore.existing(Arrays.asList(guid, "DEADDEAD"));

		assertEquals(1, existing.size());
		assertEquals(guid, existing.get(0));
	}

	@Test
	public void testDelete() throws SQLException {
		String guid = UUID.randomUUID().toString().replace("-", "");
		MessageBody mb = simpleTextBody(guid);
		bodyStore.store(mb);
		MessageBody reloaded = bodyStore.get(guid);
		assertNotNull(reloaded);

		bodyStore.delete(guid);
		reloaded = bodyStore.get(guid);
		assertNull(reloaded);

	}

	@Test
	public void testCrudSimple() throws SQLException {
		String guid = UUID.randomUUID().toString().replace("-", "");
		MessageBody mb = simpleTextBody(guid);
		bodyStore.store(mb);
		MessageBody reloaded = bodyStore.get(guid);
		assertNotNull(reloaded);
		assertEquals(1, reloaded.headers.size());
		Header h = reloaded.headers.get(0);
		assertEquals("X-Spam-Flag", h.name);
		assertEquals("NO", h.values.get(0));
		assertEquals("this is a preview", reloaded.preview);
		assertEquals(DispositionType.INLINE, reloaded.structure.dispositionType);
		assertEquals(42, reloaded.bodyVersion);
		for (int i = 0; i < mb.references.size(); i++) {
			assertEquals(mb.references.get(i), reloaded.references.get(i));
		}
		assertEquals(mb.messageId, reloaded.messageId);

		List<MessageBody> mget = bodyStore.multiple(guid);
		assertEquals(1, mget.size());
		List<MessageBody> mget2 = bodyStore.multiple(Arrays.asList(guid));
		assertEquals(1, mget2.size());

		reloaded.subject = "updated";
		bodyStore.store(reloaded);
		MessageBody reloaded2 = bodyStore.get(guid);
		assertEquals("updated", reloaded2.subject);
		assertEquals(guid, reloaded2.guid);

		bodyStore.delete(guid);
		reloaded = bodyStore.get(guid);
		assertNull(reloaded);
	}

	static final int CNT = 10000;

	@Test
	public void testMgetPerf() throws SQLException {
		long time = System.currentTimeMillis();
		String[] existing = new String[CNT];
		for (int i = 0; i < CNT; i++) {
			String guid = UUID.randomUUID().toString().replace("-", "");
			MessageBody mb = simpleTextBody(guid);
			bodyStore.store(mb);
			existing[i] = guid;
		}
		time = System.currentTimeMillis() - time;
		System.err.println("Provisionned " + CNT + " in " + time + "ms.");

		int total = 0;
		time = System.currentTimeMillis();
		for (int i = 0; i < 250; i++) {
			String[] randSlice = randomSlice(existing, 500);
			List<MessageBody> fetched = bodyStore.multiple(randSlice);
			total += fetched.size();
		}
		time = System.currentTimeMillis() - time;
		System.err.println("Fetched bunch (" + total + ") in " + time + "ms.");

	}

	private String[] randomSlice(String[] existing, int len) {
		String[] randSlice = new String[len];
		ThreadLocalRandom rand = ThreadLocalRandom.current();
		for (int i = 0; i < len; i++) {
			randSlice[i] = existing[rand.nextInt(CNT)];
		}
		return randSlice;
	}

	protected IServiceProvider provider(String userUid) {
		SecurityContext secCtx = new SecurityContext("sid-" + userUid, userUid, Collections.emptyList(),
				Collections.emptyList(), domainUid);
		return ServerSideServiceProvider.getProvider(secCtx);
	}

	@Test
	public void testdeleteOrphan() throws SQLException {
		ContainerStore containerHome = new ContainerStore(null, JdbcTestHelper.getInstance().getMailboxDataDataSource(),
				SecurityContext.SYSTEM);
		IMailboxFolders user1MailboxFolderService = provider(user1Uid).instance(IMailboxFolders.class, partition,
				user1MboxRoot);
		String user1InboxUid = user1MailboxFolderService.byName("INBOX").uid;
		Container container = containerHome.get(IMailReplicaUids.mboxRecords(user1InboxUid));

		ItemStore itemStore = new ItemStore(JdbcTestHelper.getInstance().getMailboxDataDataSource(), container,
				SecurityContext.SYSTEM);
		IMailboxes mailboxesApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IMailboxes.class, domainUid);
		// TODO: optimize: we don't need all that stuff, just the mailbox container id
		ItemValue<Mailbox> mailbox = mailboxesApi.getComplete(user1Uid);

		String subtreeContainerUid = IMailReplicaUids.subtreeUid(domainUid, mailbox);
		Container subtreeContainer = containerHome.get(subtreeContainerUid);
		if (subtreeContainer == null) {
			throw ServerFault.notFound("subtree " + subtreeContainerUid);
		}
		MailboxRecordStore boxRecordStore = new MailboxRecordStore(
				JdbcTestHelper.getInstance().getMailboxDataDataSource(), container, subtreeContainer);

		String guid = UUID.randomUUID().toString().replace("-", "");
		MessageBody mb = simpleTextBody(guid);
		bodyStore.store(mb);
		MessageBody reloaded = bodyStore.get(guid);
		assertNotNull(reloaded);

		MailboxRecord record = new MailboxRecord();
		record.imapUid = 42;
		record.messageBody = guid;
		record.internalDate = new Date();
		record.lastUpdated = new Date();
		record.flags = Collections.emptyList();
		String uniqueId = "rec" + System.currentTimeMillis();
		itemStore.create(Item.create(uniqueId, null));
		Item it = itemStore.get(uniqueId);
		boxRecordStore.create(it, record);

		String guid2 = UUID.randomUUID().toString().replace("-", "");
		mb = simpleTextBody(guid2);
		mb.subject = "expired";
		bodyStore.store(mb);
		reloaded = bodyStore.get(guid2);
		assertNotNull(reloaded);

		MailboxRecord record2 = new MailboxRecord();
		record2.imapUid = 43;
		record2.messageBody = guid2;
		record2.internalDate = new Date();
		record2.lastUpdated = new Date();
		record2.flags = Collections.emptyList();
		String uniqueId2 = "rec2" + System.currentTimeMillis();
		itemStore.create(Item.create(uniqueId2, null));
		Item it2 = itemStore.get(uniqueId2);
		boxRecordStore.create(it2, record2);

		String guid3 = UUID.randomUUID().toString().replace("-", "");
		mb = simpleTextBody(guid3);
		bodyStore.store(mb);
		reloaded = bodyStore.get(guid3);
		assertNotNull(reloaded);

		MailboxRecord record3 = new MailboxRecord();
		record3.imapUid = 44;
		record3.messageBody = guid3;
		record3.internalDate = new Date();
		record3.lastUpdated = new Date();
		record3.flags = Collections.emptyList();
		String uniqueId3 = "rec3" + System.currentTimeMillis();
		itemStore.create(Item.create(uniqueId3, null));
		Item it3 = itemStore.get(uniqueId3);
		boxRecordStore.create(it3, record3);

		boxRecordStore.delete(it2);
		adjustCreationDate(guid2);
		bodyStore.deleteOrphanBodies();

		assertNotNull(bodyStore.get(guid));
		assertNull(bodyStore.get(guid2));
		assertNotNull(bodyStore.get(guid3));
	}

	private void adjustCreationDate(String guid) throws SQLException {
		try (Connection con = JdbcTestHelper.getInstance().getMailboxDataDataSource().getConnection();
				PreparedStatement stm = con.prepareStatement(
						"update t_message_body_purge_queue set created = now() - '1 year'::interval where encode(message_body_guid, 'hex') = ? ")) {
			stm.setString(1, guid);
			stm.executeUpdate();
		}
	}

	@Test
	public void testDeleteAll() throws SQLException {
		String guid = UUID.randomUUID().toString().replace("-", "");
		MessageBody mb = simpleTextBody(guid);
		bodyStore.store(mb);
		assertNotNull(bodyStore.get(guid));

		String guid2 = UUID.randomUUID().toString().replace("-", "");
		mb = simpleTextBody(guid2);
		bodyStore.store(mb);
		assertNotNull(bodyStore.get(guid2));

		bodyStore.deleteAll();

		assertNull(bodyStore.get(guid));
		assertNull(bodyStore.get(guid2));
	}

	@Test
	public void testCrudWithAttachment() throws SQLException {
		String guid = UUID.randomUUID().toString().replace("-", "");
		MessageBody mb = simpleTextBody(guid);
		mb.structure.children.add(Part.create("mia_callista.png", "image/png", "1.2"));
		bodyStore.store(mb);
		MessageBody reloaded = bodyStore.get(guid);
		assertNotNull(reloaded);
		assertFalse(reloaded.structure.children.isEmpty());
		assertEquals(1, reloaded.structure.children.size());
		Part attachLoaded = reloaded.structure.children.get(0);
		assertNotNull(attachLoaded);
		assertEquals("mia_callista.png", attachLoaded.fileName);
		assertEquals("image/png", attachLoaded.mime);
		assertEquals("mimeAddr not persisted correctly", "1.2", attachLoaded.address);
		System.out.println("Structure is " + JsonUtils.asString(reloaded.structure));
	}

	private MessageBody simpleTextBody(String guid) {
		MessageBody mb = new MessageBody();
		mb.guid = guid;
		mb.subject = "Yeah " + System.currentTimeMillis();
		Part base = new Part();
		base.mime = "text/plain";
		base.address = "1";
		base.dispositionType = DispositionType.INLINE;
		mb.structure = base;
		mb.date = new Date();
		mb.headers = Arrays.asList(MessageBody.Header.create("X-Spam-Flag", "NO"));
		mb.messageId = "<8653e989ae53fab6039e72ba04fb9caf@blue-mind.net>";
		mb.references = Arrays.asList("<521DD9C9-6E9A-4F51-B809-8FABA51D742B@bluemind.net>",
				"<21174FB9-A2EB-4CD2-8383-1230243FBB2B@bluemind.net>",
				"<E562F887-8BBA-4DAD-B4A2-04E58B3DF4AB@blue-mind.net>",
				"<ADDE5EAC-8374-4CF6-AB1A-35AF676EBE60@blue-mind.net>");
		mb.preview = "this is a preview";
		mb.bodyVersion = 42;
		return mb;
	}

}
