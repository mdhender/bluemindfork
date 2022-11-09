/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.conversation.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import net.bluemind.backend.cyrus.replication.testhelper.CyrusGUID;
import net.bluemind.backend.mail.api.Conversation;
import net.bluemind.backend.mail.api.Conversation.MessageRef;
import net.bluemind.backend.mail.api.IMailConversation;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.core.api.Stream;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.container.model.SortDescriptor.Direction;
import net.bluemind.core.container.model.SortDescriptor.Field;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.core.rest.vertx.VertxStream;

public class MailConversationServiceTests extends AbstractConversationTests {
	private int index = 1;

	@Test
	public void obtainService() {
		IMailConversation mailConversationService = getConversationService(user1Uid);
		assertNotNull(mailConversationService);
	}

	@Test
	public void create() {
		create(Long.toHexString(123456789L));
	}

	@Test
	public void byConversationId() {
		String conversationId = Long.toHexString(123456789L);
		create(conversationId);

		MessageRef messageId = new MessageRef();
		messageId.folderUid = user1InboxUid;
		messageId.itemId = 42L;
		messageId.date = new Date(1);
		MessageRef messageId2 = new MessageRef();
		messageId2.folderUid = user1InboxUid;
		messageId2.itemId = 66L;
		messageId2.date = new Date(2);
		String conversationId2 = Long.toHexString(999999L);
		Conversation created = create(conversationId2, messageId, messageId2);

		Conversation conversation = getConversationService(user1Uid).get(conversationId2);
		assertNotNull(conversation);
		assertEquals(conversationId2, conversation.conversationUid);
		assertEquals(2, conversation.messageRefs.size());
		assertEquals(created.messageRefs.get(0).folderUid, conversation.messageRefs.get(0).folderUid);
		assertEquals(created.messageRefs.get(0).itemId, conversation.messageRefs.get(0).itemId);
		assertEquals(new Date(1), conversation.messageRefs.get(0).date);
	}

	@Test
	public void byFolder() {
		String conversationId = Long.toHexString(123456789L);
		MessageRef ref1 = new MessageRef();
		ref1.folderUid = user1InboxUid;
		ref1.itemId = 42L;
		ref1.date = new Date(1);
		MessageRef ref2 = new MessageRef();
		ref2.folderUid = user1InboxUid;
		ref2.itemId = 66L;
		ref2.date = new Date(2);
		create(conversationId, ref1, ref2);

		String conversationId2 = Long.toHexString(88888888L);
		MessageRef ref3 = new MessageRef();
		ref3.folderUid = user1InboxUid;
		ref3.itemId = 111L;
		ref3.date = new Date(3);
		MessageRef ref4 = new MessageRef();
		ref4.folderUid = user1InboxUid;
		ref4.itemId = 51L;
		ref4.date = new Date(4);
		create(conversationId2, ref3, ref4);

		IDbMessageBodies mboxes = getBodies(SecurityContext.SYSTEM);
		assertNotNull(mboxes);
		ReadStream<Buffer> emlReadStream = openResource("data/with_inlines.eml");
		Stream bmStream = VertxStream.stream(emlReadStream);
		String bodyUid = CyrusGUID.randomGuid();
		mboxes.create(bodyUid, bmStream);

		IDbMailboxRecords recordService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDbMailboxRecords.class, user1InboxUid);
		MailboxRecord rec1 = new MailboxRecord();
		rec1.conversationId = 123456789L;
		rec1.imapUid = 1;
		rec1.internalDate = new Date();
		rec1.lastUpdated = new Date();
		rec1.messageBody = bodyUid;
		recordService.create(UUID.randomUUID().toString(), rec1);

		MailboxRecord rec2 = new MailboxRecord();
		rec2.conversationId = 88888888L;
		rec2.imapUid = 2;
		rec2.internalDate = new Date();
		rec2.lastUpdated = new Date();
		rec2.messageBody = bodyUid;
		recordService.create(UUID.randomUUID().toString(), rec2);

		List<String> conversations = getConversationService(user1Uid).byFolder(user1InboxUid,
				createSortDescriptor(ItemFlagFilter.all()));
		assertEquals(2, conversations.size());
	}

	@Test
	public void byFolderBySort() {
		long conversation1 = 1234567891L;
		long conversation2 = 888888881L;
		String conversationId1 = Long.toHexString(conversation1);
		String conversationId2 = Long.toHexString(conversation2);

		// test bodies
		IDbMessageBodies mboxes = getBodies(SecurityContext.SYSTEM);
		assertNotNull(mboxes);
		ReadStream<Buffer> emlReadStream = openResource("data/sort_1.eml");
		Stream bmStream = VertxStream.stream(emlReadStream);
		String bodySortUid1 = CyrusGUID.randomGuid();
		mboxes.create(bodySortUid1, bmStream);

		emlReadStream = openResource("data/sort_2.eml");
		bmStream = VertxStream.stream(emlReadStream);
		String bodySortUid2 = CyrusGUID.randomGuid();
		mboxes.create(bodySortUid2, bmStream);

		emlReadStream = openResource("data/sort_3.eml");
		bmStream = VertxStream.stream(emlReadStream);
		String bodySortUid3 = CyrusGUID.randomGuid();
		mboxes.create(bodySortUid3, bmStream);

		IDbMailboxRecords recordService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDbMailboxRecords.class, user1InboxUid);

		// conversation 1 having 2 messages
		MailboxRecord rec1 = new MailboxRecord();
		rec1.conversationId = conversation1;
		rec1.imapUid = 1;
		rec1.internalDate = adaptDate(2);
		rec1.lastUpdated = rec1.internalDate;
		rec1.messageBody = bodySortUid1;
		String rec1Uid = UUID.randomUUID().toString();
		recordService.create(rec1Uid, rec1);
		long rec1Id = recordService.getComplete(rec1Uid).internalId;

		MailboxRecord rec2 = new MailboxRecord();
		rec2.conversationId = conversation1;
		rec2.imapUid = 2;
		rec2.internalDate = adaptDate(3);
		rec2.lastUpdated = rec2.internalDate;
		rec2.messageBody = bodySortUid2;
		String rec2Uid = UUID.randomUUID().toString();
		recordService.create(rec2Uid, rec2);
		long rec2Id = recordService.getComplete(rec2Uid).internalId;

		MessageRef ref1 = new MessageRef();
		ref1.folderUid = user1InboxUid;
		ref1.itemId = rec1Id;
		ref1.date = new Date(1);

		MessageRef ref2 = new MessageRef();
		ref2.folderUid = user1InboxUid;
		ref2.itemId = rec2Id;
		ref2.date = new Date(2);

		create(conversationId1, ref1, ref2);

		// conversation 2 having 1 message
		MailboxRecord rec3 = new MailboxRecord();
		rec3.conversationId = conversation2;
		rec3.imapUid = 1;
		rec3.internalDate = adaptDate(4);
		rec3.lastUpdated = rec3.internalDate;
		rec3.messageBody = bodySortUid3;
		String rec3Uid = UUID.randomUUID().toString();
		recordService.create(rec3Uid, rec3);
		long rec3Id = recordService.getComplete(rec3Uid).internalId;

		MessageRef messageRef3 = new MessageRef();
		messageRef3.folderUid = user1InboxUid;
		messageRef3.itemId = rec3Id;
		messageRef3.date = new Date(3);

		create(conversationId2, messageRef3);

		SortDescriptor sortDesc = createSortDescriptor(null);
		Field sortField = new Field();
		sortField.column = "date";
		sortField.dir = Direction.Desc;
		sortDesc.fields = Arrays.asList(sortField);
		List<String> conversations = getConversationService(user1Uid).byFolder(user1InboxUid, sortDesc);
		assertEquals(2, conversations.size());
		assertEquals(conversationId1, conversations.get(0));
		assertEquals(conversationId2, conversations.get(1));

		sortField = new Field();
		sortField.column = "size";
		sortField.dir = Direction.Asc;
		sortDesc.fields = Arrays.asList(sortField);
		conversations = getConversationService(user1Uid).byFolder(user1InboxUid, sortDesc);
		assertEquals(2, conversations.size());
		assertEquals(conversationId1, conversations.get(0));
		assertEquals(conversationId2, conversations.get(1));

		sortField = new Field();
		sortField.column = "subject";
		sortField.dir = Direction.Desc;
		sortDesc.fields = Arrays.asList(sortField);
		conversations = getConversationService(user1Uid).byFolder(user1InboxUid, sortDesc);
		assertEquals(2, conversations.size());
		assertEquals(conversationId2, conversations.get(0));
		assertEquals(conversationId1, conversations.get(1));

	}

	@Test
	public void byFolderSubject() throws SQLException {
		long conversation1 = 1212121212L;
		long conversation2 = 8989898989L;
		long conversation3 = 2323232323L;
		String conversationId1 = Long.toHexString(conversation1);
		String conversationId2 = Long.toHexString(conversation2);
		String conversationId3 = Long.toHexString(conversation3);

		IDbMessageBodies mboxes = getBodies(SecurityContext.SYSTEM);
		assertNotNull(mboxes);

		// test bodies
		ReadStream<Buffer> emlReadStream = openResource("data/test_subject1.eml");
		Stream bmStream = VertxStream.stream(emlReadStream);
		String bodySortUid1 = CyrusGUID.randomGuid();
		mboxes.create(bodySortUid1, bmStream);

		emlReadStream = openResource("data/test_subject2.eml");
		bmStream = VertxStream.stream(emlReadStream);
		String bodySortUid2 = CyrusGUID.randomGuid();
		mboxes.create(bodySortUid2, bmStream);

		emlReadStream = openResource("data/test_subject3.eml");
		bmStream = VertxStream.stream(emlReadStream);
		String bodySortUid3 = CyrusGUID.randomGuid();
		mboxes.create(bodySortUid3, bmStream);

		IDbMailboxRecords recordService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDbMailboxRecords.class, user1InboxUid);

		// conversation 1
		MailboxRecord rec1 = new MailboxRecord();
		rec1.conversationId = conversation1;
		rec1.imapUid = 1;
		rec1.internalDate = adaptDate(2);
		rec1.lastUpdated = rec1.internalDate;
		rec1.messageBody = bodySortUid1;
		String rec1Uid = UUID.randomUUID().toString();
		recordService.create(rec1Uid, rec1);
		ItemValue<MailboxRecord> rec1ItemVal = recordService.getComplete(rec1Uid);
		long rec1Id = rec1ItemVal.internalId;

		MessageRef messageRef1 = new MessageRef();
		messageRef1.folderUid = user1InboxUid;
		messageRef1.itemId = rec1Id;
		messageRef1.date = new Date(1);

		create(conversationId1, messageRef1);

		// conversation 2
		MailboxRecord rec2 = new MailboxRecord();
		rec2.conversationId = conversation2;
		rec2.imapUid = 2;
		rec2.internalDate = adaptDate(4);
		rec2.lastUpdated = rec2.internalDate;
		rec2.messageBody = bodySortUid2;
		String rec2Uid = UUID.randomUUID().toString();
		recordService.create(rec2Uid, rec2);
		ItemValue<MailboxRecord> rec2ItemVal = recordService.getComplete(rec2Uid);
		long rec2Id = rec2ItemVal.internalId;

		MessageRef messageRef2 = new MessageRef();
		messageRef2.folderUid = user1InboxUid;
		messageRef2.itemId = rec2Id;
		messageRef2.date = new Date(2);

		create(conversationId2, messageRef2);

		// conversation 3
		MailboxRecord rec3 = new MailboxRecord();
		rec3.conversationId = conversation3;
		rec3.imapUid = 3;
		rec3.internalDate = adaptDate(6);
		rec3.lastUpdated = rec3.internalDate;
		rec3.messageBody = bodySortUid3;
		String rec3Uid = UUID.randomUUID().toString();
		recordService.create(rec3Uid, rec3);
		ItemValue<MailboxRecord> rec3ItemVal = recordService.getComplete(rec3Uid);
		long rec3Id = rec3ItemVal.internalId;

		MessageRef messageRef3 = new MessageRef();
		messageRef3.folderUid = user1InboxUid;
		messageRef3.itemId = rec3Id;
		messageRef3.date = new Date(3);

		create(conversationId3, messageRef3);

		// asserts
		List<String> conversations = getConversationService(user1Uid).byFolder(user1InboxUid,
				createSortDescriptor(ItemFlagFilter.all()));
		assertEquals(3, conversations.size());

		String request = "select folder_id, conversation_id, subject from v_conversation_by_folder where conversation_id = ?";
		try (Connection con = datasource.getConnection()) {
			try (PreparedStatement query = con.prepareStatement(request)) {
				query.setLong(1, conversation1);
				ResultSet resultSet = query.executeQuery();
				resultSet.next();
				String subject = resultSet.getString(3);
				assertEquals("Esubjecte", subject);
			}

			try (PreparedStatement query = con.prepareStatement(request)) {
				query.setLong(1, conversation2);
				ResultSet resultSet = query.executeQuery();
				resultSet.next();
				String subject = resultSet.getString(3);
				assertEquals("asubject", subject);
			}

			try (PreparedStatement query = con.prepareStatement(request)) {
				query.setLong(1, conversation3);
				ResultSet resultSet = query.executeQuery();
				resultSet.next();
				String subject = resultSet.getString(3);
				assertEquals("subject", subject);
			}
		}
	}

	@Test
	public void getMultiple() {
		String conversationId = Long.toHexString(123456789L);
		create(conversationId, MessageRef.of(user1InboxUid, 322, new Date(551)));

		String conversationId2 = Long.toHexString(999999L);
		create(conversationId2, MessageRef.of(user1InboxUid, 42L, new Date(1)),
				MessageRef.of(user1InboxUid, 66L, new Date(2)));

		List<Conversation> conversationItems = getConversationService(user1Uid)
				.multipleGet(Arrays.asList(conversationId, conversationId2));
		assertEquals(2, conversationItems.size());
		boolean ok1 = false;
		boolean ok2 = false;
		for (Conversation conversation : conversationItems) {
			if (conversation.conversationUid.equals(conversationId)) {
				ok1 = true;
			} else if (conversation.conversationUid.equals(conversationId2)) {
				if (conversation.messageRefs.size() == 2) {
					ok2 = true;
				}
			}
		}

		assertTrue(ok1);
		assertTrue(ok2);
	}

	protected Conversation create(String conversationUid, MessageRef... messageRefs) {
		if (messageRefs != null) {
			for (MessageRef id : messageRefs) {
				IDbMailboxRecords recordService = provider(user1Uid).instance(IDbMailboxRecords.class, id.folderUid);

				IDbMessageBodies mboxes = getBodies(SecurityContext.SYSTEM);
				Stream bmStream = GenericStream.simpleValue("test", String::getBytes);
				String bodyUid = CyrusGUID.randomGuid();
				mboxes.create(bodyUid, bmStream);

				MailboxRecord tmpRecord = new MailboxRecord();
				tmpRecord.imapUid = index++;
				tmpRecord.internalDate = id.date;
				tmpRecord.lastUpdated = tmpRecord.internalDate;
				tmpRecord.messageBody = bodyUid;
				tmpRecord.conversationId = Long.parseUnsignedLong(conversationUid, 16);
				String mailUid = "uid." + index;
				recordService.create(mailUid, tmpRecord);
				ItemValue<MailboxRecord> complete = recordService.getComplete(mailUid);
				id.itemId = complete.internalId;
			}
		}
		return getConversationService(user1Uid).get(conversationUid);
	}
}
