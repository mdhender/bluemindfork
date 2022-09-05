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
package net.bluemind.backend.mail.replica.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
import net.bluemind.backend.mail.api.IMailConversationActions;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.IInternalMailConversation;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
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

public class MailConversationServiceTests extends AbstractMailboxRecordsServiceTests {

	private int index = 1;

	protected IInternalMailConversation getService(SecurityContext ctx) {
		return ServerSideServiceProvider.getProvider(ctx).instance(IInternalMailConversation.class,
				IMailReplicaUids.conversationSubtreeUid(super.dom, "me"));
	}

	protected IMailConversationActions getActionService(SecurityContext ctx, String replicatedMailBoxUid) {
		return ServerSideServiceProvider.getProvider(ctx).instance(IMailConversationActions.class,
				IMailReplicaUids.conversationSubtreeUid(super.dom, "me"), replicatedMailBoxUid);
	}

	@Test
	public void obtainService() {
		IMailConversation mailConversationService = getService(SecurityContext.SYSTEM);
		assertNotNull(mailConversationService);
	}

	@Test
	public void create() {
		create(Long.toHexString(123456789L));
	}

	private Conversation create(String uid, MessageRef... messageRefs) {
		Conversation conversation = new Conversation();
		if (messageRefs != null) {
			for (MessageRef id : messageRefs) {
				IDbMailboxRecords recordService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
						.instance(IDbMailboxRecords.class, id.folderUid);

				IDbMessageBodies mboxes = getBodies(SecurityContext.SYSTEM);
				Stream bmStream = GenericStream.simpleValue("test", t -> t.getBytes());
				String bodyUid = CyrusGUID.randomGuid();
				mboxes.create(bodyUid, bmStream);

				MailboxRecord record = new MailboxRecord();
				record.imapUid = index++;
				record.internalDate = new Date();
				record.lastUpdated = record.internalDate;
				record.messageBody = bodyUid;
				String mailUid = "uid." + index;
				recordService.create(mailUid, record);
				ItemValue<MailboxRecord> complete = recordService.getComplete(mailUid);
				id.itemId = complete.internalId;
			}

			conversation.messageRefs = Arrays.asList(messageRefs);
		}
		getService(SecurityContext.SYSTEM).create(uid, conversation);
		return conversation;
	}

	@Test
	public void update() {
		Long conversationId = 123456789L;
		String uid = Long.toHexString(conversationId);
		create(uid);

		MessageRef messageId = new MessageRef();
		messageId.folderUid = mboxUniqueId;
		messageId.itemId = 42L;
		messageId.date = new Date(1);

		MessageRef messageId2 = new MessageRef();
		messageId2.folderUid = mboxUniqueId;
		messageId2.itemId = 66L;
		messageId2.date = new Date(2);

		update(uid, conversationId, messageId, messageId2);
	}

	private void update(String uid, Long conversationId, MessageRef... MessageRefs) {
		Conversation conversation = new Conversation();
		conversation.messageRefs = new ArrayList<>();
		conversation.messageRefs.addAll(Arrays.asList(MessageRefs));
		getService(SecurityContext.SYSTEM).update(uid, conversation);
	}

	@Test
	public void byConversationId() {
		String conversationId = Long.toHexString(123456789L);
		create(conversationId);

		MessageRef messageId = new MessageRef();
		messageId.folderUid = mboxUniqueId;
		messageId.itemId = 42L;
		messageId.date = new Date(1);
		MessageRef messageId2 = new MessageRef();
		messageId2.folderUid = mboxUniqueId;
		messageId2.itemId = 66L;
		messageId2.date = new Date(2);
		String conversationId2 = Long.toHexString(999999L);
		Conversation created = create(conversationId2, messageId, messageId2);

		ItemValue<Conversation> conversationItem = getService(SecurityContext.SYSTEM).getComplete(conversationId2);
		assertNotNull(conversationItem);
		assertEquals(conversationId2, conversationItem.uid);
		assertEquals(2, conversationItem.value.messageRefs.size());
		assertEquals(created.messageRefs.get(0).folderUid, conversationItem.value.messageRefs.get(0).folderUid);
		assertEquals(created.messageRefs.get(0).itemId, conversationItem.value.messageRefs.get(0).itemId);
		assertEquals(new Date(1), conversationItem.value.messageRefs.get(0).date);
	}

	private SortDescriptor createSortDescriptor(ItemFlagFilter flagFilter) {
		SortDescriptor sortDesc = new SortDescriptor();
		sortDesc.filter = flagFilter;
		return sortDesc;
	}

	@Test
	public void byFolder() {
		String conversationId = Long.toHexString(123456789L);
		MessageRef MessageRef = new MessageRef();
		MessageRef.folderUid = mboxUniqueId;
		MessageRef.itemId = 42L;
		MessageRef.date = new Date(1);
		MessageRef MessageRef2 = new MessageRef();
		MessageRef2.folderUid = mboxUniqueId;
		MessageRef2.itemId = 66L;
		MessageRef2.date = new Date(2);
		create(conversationId, MessageRef, MessageRef2);

		String conversationId2 = Long.toHexString(88888888L);
		MessageRef MessageRef3 = new MessageRef();
		MessageRef3.folderUid = mboxUniqueId;
		MessageRef3.itemId = 111L;
		MessageRef3.date = new Date(3);
		MessageRef MessageRef4 = new MessageRef();
		MessageRef4.folderUid = mboxUniqueId;
		MessageRef4.itemId = 51L;
		MessageRef4.date = new Date(4);
		create(conversationId2, MessageRef3, MessageRef4);

		IDbMessageBodies mboxes = getBodies(SecurityContext.SYSTEM);
		assertNotNull(mboxes);
		ReadStream<Buffer> emlReadStream = openResource("data/with_inlines.eml");
		Stream bmStream = VertxStream.stream(emlReadStream);
		String bodyUid = CyrusGUID.randomGuid();
		mboxes.create(bodyUid, bmStream);

		IDbMailboxRecords recordService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDbMailboxRecords.class, mboxUniqueId);
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

		List<String> conversations = getService(SecurityContext.SYSTEM).byFolder(mboxUniqueId,
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
				.instance(IDbMailboxRecords.class, mboxUniqueId);

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

		MessageRef messageRef1 = new MessageRef();
		messageRef1.folderUid = mboxUniqueId;
		messageRef1.itemId = rec1Id;
		messageRef1.date = new Date(1);

		MessageRef messageRef2 = new MessageRef();
		messageRef2.folderUid = mboxUniqueId;
		messageRef2.itemId = rec2Id;
		messageRef2.date = new Date(2);

		Conversation conversation1Obj = new Conversation();
		conversation1Obj.messageRefs = Arrays.asList(messageRef1, messageRef2);
		getService(SecurityContext.SYSTEM).create(conversationId1, conversation1Obj);

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
		messageRef3.folderUid = mboxUniqueId;
		messageRef3.itemId = rec3Id;
		messageRef3.date = new Date(3);

		Conversation conversation2Obj = new Conversation();
		conversation2Obj.messageRefs = Arrays.asList(messageRef3);
		getService(SecurityContext.SYSTEM).create(conversationId2, conversation2Obj);

		SortDescriptor sortDesc = createSortDescriptor(null);
		Field sortField = new Field();
		sortField.column = "internal_date";
		sortField.dir = Direction.Desc;
		sortDesc.fields = Arrays.asList(sortField);
		List<String> conversations = getService(SecurityContext.SYSTEM).byFolder(mboxUniqueId, sortDesc);
		assertEquals(2, conversations.size());
		assertEquals(conversationId1, conversations.get(0));
		assertEquals(conversationId2, conversations.get(1));

		sortField = new Field();
		sortField.column = "size";
		sortField.dir = Direction.Asc;
		sortDesc.fields = Arrays.asList(sortField);
		conversations = getService(SecurityContext.SYSTEM).byFolder(mboxUniqueId, sortDesc);
		assertEquals(2, conversations.size());
		assertEquals(conversationId1, conversations.get(0));
		assertEquals(conversationId2, conversations.get(1));

		sortField = new Field();
		sortField.column = "subject";
		sortField.dir = Direction.Desc;
		sortDesc.fields = Arrays.asList(sortField);
		conversations = getService(SecurityContext.SYSTEM).byFolder(mboxUniqueId, sortDesc);
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
				.instance(IDbMailboxRecords.class, mboxUniqueId);

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
		messageRef1.folderUid = mboxUniqueId;
		messageRef1.itemId = rec1Id;
		messageRef1.date = new Date(1);

		Conversation conversation1Obj = new Conversation();
		conversation1Obj.messageRefs = Arrays.asList(messageRef1);
		getService(SecurityContext.SYSTEM).create(conversationId1, conversation1Obj);

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
		messageRef2.folderUid = mboxUniqueId;
		messageRef2.itemId = rec2Id;
		messageRef2.date = new Date(2);

		Conversation conversation2Obj = new Conversation();
		conversation2Obj.messageRefs = Arrays.asList(messageRef2);
		getService(SecurityContext.SYSTEM).create(conversationId2, conversation2Obj);

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
		messageRef3.folderUid = mboxUniqueId;
		messageRef3.itemId = rec3Id;
		messageRef3.date = new Date(3);

		Conversation conversation3Obj = new Conversation();
		conversation3Obj.messageRefs = Arrays.asList(messageRef3);
		getService(SecurityContext.SYSTEM).create(conversationId3, conversation3Obj);

		// asserts
		List<String> conversations = getService(SecurityContext.SYSTEM).byFolder(mboxUniqueId,
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
		create(conversationId);

		MessageRef messageId = new MessageRef();
		messageId.folderUid = mboxUniqueId;
		messageId.itemId = 42L;
		messageId.date = new Date(1);
		MessageRef messageId2 = new MessageRef();
		messageId2.folderUid = mboxUniqueId;
		messageId2.itemId = 66L;
		messageId2.date = new Date(2);
		String conversationId2 = Long.toHexString(999999L);
		Conversation created = create(conversationId2, messageId, messageId2);

		List<ItemValue<Conversation>> conversationItems = getService(SecurityContext.SYSTEM)
				.multipleGet(Arrays.asList(conversationId, conversationId2));
		assertEquals(2, conversationItems.size());
		boolean ok1 = false;
		boolean ok2 = false;
		for (ItemValue<Conversation> conversation : conversationItems) {
			if (conversation.uid.equals(conversationId)) {
				ok1 = true;
			} else if (conversation.uid.equals(conversationId2)) {
				if (conversation.value.messageRefs.size() == 2) {
					ok2 = true;
				}
			}
		}

		assertTrue(ok1);
		assertTrue(ok2);
	}

	@Test
	public void testDeleteFolder() {
		Long conversationId = 123456789L;
		MessageRef messageRef = new MessageRef();
		messageRef.folderUid = mboxUniqueId;
		messageRef.itemId = 42L;
		messageRef.date = new Date(1);
		MessageRef messageRef2 = new MessageRef();
		messageRef2.folderUid = mboxUniqueId;
		messageRef2.itemId = 66L;
		messageRef2.date = new Date(2);
		MessageRef messageRef3 = new MessageRef();
		messageRef3.folderUid = mboxUniqueId2;
		messageRef3.itemId = 88L;
		messageRef3.date = new Date(3);
		String uid = Long.toHexString(conversationId);
		create(uid, messageRef, messageRef2, messageRef3);

		ItemValue<Conversation> conversation = getService(SecurityContext.SYSTEM).getComplete(uid);
		assertEquals(3, conversation.value.messageRefs.size());
		getService(SecurityContext.SYSTEM).deleteAll(mboxUniqueId);
		conversation = getService(SecurityContext.SYSTEM).getComplete(uid);
		assertEquals(1, conversation.value.messageRefs.size());
		assertEquals(mboxUniqueId2, conversation.value.messageRefs.get(0).folderUid);
		getService(SecurityContext.SYSTEM).deleteAll(mboxUniqueId2);
		conversation = getService(SecurityContext.SYSTEM).getComplete(uid);
		assertEquals(0, conversation.value.messageRefs.size());
	}

	@Test
	public void testRemoveMessage() {
		String conversationId = Long.toHexString(123456789L);

		MessageRef messageId = new MessageRef();
		messageId.folderUid = mboxUniqueId;
		messageId.itemId = 42L;
		messageId.date = new Date(1);

		MessageRef messageId2 = new MessageRef();
		messageId2.folderUid = mboxUniqueId;
		messageId2.itemId = 66L;
		messageId2.date = new Date(2);

		MessageRef messageId3 = new MessageRef();
		messageId3.folderUid = mboxUniqueId;
		messageId3.itemId = 111L;
		messageId3.date = new Date(3);

		MessageRef messageId4 = new MessageRef();
		messageId4.folderUid = mboxUniqueId;
		messageId4.itemId = 51L;
		messageId4.date = new Date(4);

		create(conversationId, messageId, messageId2, messageId3, messageId4);

		IMailConversation service = getService(SecurityContext.SYSTEM);
		ItemValue<Conversation> byConversationId = service.getComplete(conversationId);
		assertEquals(4, byConversationId.value.messageRefs.size());

		service.removeMessage(messageId2.folderUid, messageId2.itemId);

		byConversationId = service.getComplete(conversationId);
		assertEquals(3, byConversationId.value.messageRefs.size());

		for (MessageRef msg : byConversationId.value.messageRefs) {
			assertFalse(msg.itemId == messageId2.itemId);
		}

	}

	@Test
	public void testRemoveLastMessage() {
		String conversationId = Long.toHexString(123456789L);

		MessageRef messageId = new MessageRef();
		messageId.folderUid = mboxUniqueId;
		messageId.itemId = 42L;
		messageId.date = new Date(1);

		MessageRef messageId2 = new MessageRef();
		messageId2.folderUid = mboxUniqueId;
		messageId2.itemId = 66L;
		messageId2.date = new Date(2);

		MessageRef messageId3 = new MessageRef();
		messageId3.folderUid = mboxUniqueId;
		messageId3.itemId = 111L;
		messageId3.date = new Date(3);

		MessageRef messageId4 = new MessageRef();
		messageId4.folderUid = mboxUniqueId;
		messageId4.itemId = 51L;
		messageId4.date = new Date(4);

		create(conversationId, messageId, messageId2, messageId3, messageId4);

		IMailConversation service = getService(SecurityContext.SYSTEM);
		ItemValue<Conversation> byConversationId = service.getComplete(conversationId);
		assertEquals(4, byConversationId.value.messageRefs.size());

		service.removeMessage(messageId.folderUid, messageId.itemId);
		service.removeMessage(messageId2.folderUid, messageId2.itemId);
		service.removeMessage(messageId3.folderUid, messageId3.itemId);
		service.removeMessage(messageId4.folderUid, messageId4.itemId);

		assertNull(service.getComplete(conversationId));
	}

	protected IDbMessageBodies getBodies(SecurityContext ctx) {
		return ServerSideServiceProvider.getProvider(ctx).instance(IDbMessageBodies.class, partition);
	}
}
