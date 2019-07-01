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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.backend.cyrus.replication.testhelper.CyrusGUID;
import net.bluemind.backend.cyrus.replication.testhelper.MailboxUniqueId;
import net.bluemind.backend.mail.api.DispositionType;
import net.bluemind.backend.mail.api.MailboxItem.SystemFlag;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.Header;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordStore;
import net.bluemind.backend.mail.replica.persistence.MessageBodyStore;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.persistance.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.utils.JsonUtils;

public class MessageBodyStoreTests {

	private MessageBodyStore bodyStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();

		bodyStore = new MessageBodyStore(JdbcTestHelper.getInstance().getDataSource());
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
	public void testCrudSimple() throws SQLException {
		String guid = CyrusGUID.randomGuid();
		MessageBody mb = simpleTextBody(guid);
		bodyStore.create(mb);
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
		bodyStore.update(reloaded);
		MessageBody reloaded2 = bodyStore.get(guid);
		assertEquals("updated", reloaded2.subject);

		bodyStore.delete(guid);
		reloaded = bodyStore.get(guid);
		assertNull(reloaded);
	}

	@Test
	public void testdeleteOrphan() throws SQLException {
		ContainerStore containerHome = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM);
		String boxUniqueId = MailboxUniqueId.random();
		String containerId = IMailReplicaUids.mboxRecords(boxUniqueId);
		Container container = Container.create(containerId, IMailReplicaUids.MAILBOX_RECORDS, "test", "me", true);
		container = containerHome.create(container);

		ItemStore itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), container,
				SecurityContext.SYSTEM);
		MailboxRecordStore boxRecordStore = new MailboxRecordStore(JdbcTestHelper.getInstance().getDataSource(),
				container);

		String guid = CyrusGUID.randomGuid();
		MessageBody mb = simpleTextBody(guid);
		bodyStore.create(mb);
		MessageBody reloaded = bodyStore.get(guid);
		assertNotNull(reloaded);

		MailboxRecord record = new MailboxRecord();
		record.imapUid = 42;
		record.messageBody = guid;
		record.internalDate = new Date();
		record.lastUpdated = new Date();
		record.systemFlags = EnumSet.of(SystemFlag.seen);
		String uniqueId = "rec" + System.currentTimeMillis();
		itemStore.create(Item.create(uniqueId, null));
		Item it = itemStore.get(uniqueId);
		boxRecordStore.create(it, record);

		String guid2 = CyrusGUID.randomGuid();
		mb = simpleTextBody(guid2);
		mb.subject = "expired";
		bodyStore.create(mb);
		adjustCreationDate(mb.subject);
		reloaded = bodyStore.get(guid2);
		assertNotNull(reloaded);

		String guid3 = CyrusGUID.randomGuid();
		mb = simpleTextBody(guid3);
		bodyStore.create(mb);
		reloaded = bodyStore.get(guid3);
		assertNotNull(reloaded);

		bodyStore.deleteOrphanBodies();

		assertNotNull(bodyStore.get(guid));
		assertNull(bodyStore.get(guid2));
		assertNotNull(bodyStore.get(guid3));
	}

	private void adjustCreationDate(String subject) throws SQLException {
		try (Connection con = JdbcTestHelper.getInstance().getDataSource().getConnection()) {
			PreparedStatement stm = con.prepareStatement(
					"update t_message_body set created = NOW() - INTERVAL '1 year' where subject = ?");
			stm.setString(1, subject);
			stm.executeUpdate();
		}
	}

	@Test
	public void testDeleteAll() throws SQLException {
		String guid = CyrusGUID.randomGuid();
		MessageBody mb = simpleTextBody(guid);
		bodyStore.create(mb);
		assertNotNull(bodyStore.get(guid));

		String guid2 = CyrusGUID.randomGuid();
		mb = simpleTextBody(guid2);
		bodyStore.create(mb);
		assertNotNull(bodyStore.get(guid2));

		bodyStore.deleteAll();

		assertNull(bodyStore.get(guid));
		assertNull(bodyStore.get(guid2));
	}

	@Test
	public void testCrudWithAttachment() throws SQLException {
		String guid = CyrusGUID.randomGuid();
		MessageBody mb = simpleTextBody(guid);
		mb.structure.children.add(Part.create("mia_callista.png", "image/png", "1.2"));
		bodyStore.create(mb);
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
