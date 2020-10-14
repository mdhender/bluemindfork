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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import net.bluemind.backend.cyrus.replication.testhelper.CyrusGUID;
import net.bluemind.backend.mail.api.Conversation;
import net.bluemind.backend.mail.api.Conversation.MessageRef;
import net.bluemind.backend.mail.api.IMailConversation;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.IInternalMailConversation;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.core.api.Stream;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.base.GenericStream;

public class MailConversationServiceTests extends AbstractMailboxRecordsServiceTests {

	private int index = 1;

	protected IInternalMailConversation getService(SecurityContext ctx) {
		return ServerSideServiceProvider.getProvider(ctx).instance(IInternalMailConversation.class,
				IMailReplicaUids.conversationSubtreeUid(super.dom, "me"));
	}

	@Test
	public void obtainService() {
		IMailConversation mailConversationService = getService(SecurityContext.SYSTEM);
		assertNotNull(mailConversationService);
	}

	@Test
	public void create() {
		create(UUID.randomUUID().toString(), 123456789L);
	}

	private Conversation create(String uid, Long conversationId, MessageRef... MessageRefs) {
		Conversation conversation = new Conversation();
		conversation.conversationId = conversationId;
		if (MessageRefs != null) {
			for (MessageRef id : MessageRefs) {
				IDbMailboxRecords recordService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
						.instance(IDbMailboxRecords.class, mboxUniqueId);

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
				id.folderUid = mboxUniqueId;
				id.itemId = complete.internalId;
			}

			conversation.messageRefs = Arrays.asList(MessageRefs);
		}
		getService(SecurityContext.SYSTEM).create(uid, conversation);
		return conversation;
	}

	@Test
	public void update() {
		String uid = UUID.randomUUID().toString();
		Long conversationId = 123456789L;
		create(uid, conversationId);

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
		conversation.conversationId = conversationId;
		conversation.messageRefs = new ArrayList<>();
		conversation.messageRefs.addAll(Arrays.asList(MessageRefs));
		getService(SecurityContext.SYSTEM).update(uid, conversation);
	}

	@Test
	public void byConversationId() {
		String uid = UUID.randomUUID().toString();
		Long conversationId = 123456789L;
		create(uid, conversationId);

		MessageRef messageId = new MessageRef();
		messageId.folderUid = mboxUniqueId;
		messageId.itemId = 42L;
		messageId.date = new Date(1);
		MessageRef messageId2 = new MessageRef();
		messageId2.folderUid = mboxUniqueId;
		messageId2.itemId = 66L;
		messageId2.date = new Date(2);
		String uid2 = UUID.randomUUID().toString();
		Long conversationId2 = 999999L;
		Conversation created = create(uid2, conversationId2, messageId, messageId2);

		ItemValue<Conversation> conversationItem = getService(SecurityContext.SYSTEM).byConversationId(conversationId2);
		assertNotNull(conversationItem);
		assertEquals(uid2, conversationItem.uid);
		assertEquals(2, conversationItem.value.messageRefs.size());
		assertEquals(created.messageRefs.get(0).folderUid, conversationItem.value.messageRefs.get(0).folderUid);
		assertEquals(created.messageRefs.get(0).itemId, conversationItem.value.messageRefs.get(0).itemId);
		assertEquals(new Date(1), conversationItem.value.messageRefs.get(0).date);
	}

	@Test
	public void byFolder() {
		String uid = UUID.randomUUID().toString();
		Long conversationId = 123456789L;
		MessageRef MessageRef = new MessageRef();
		MessageRef.folderUid = mboxUniqueId;
		MessageRef.itemId = 42L;
		MessageRef.date = new Date(1);
		MessageRef MessageRef2 = new MessageRef();
		MessageRef2.folderUid = mboxUniqueId;
		MessageRef2.itemId = 66L;
		MessageRef2.date = new Date(2);
		create(uid, conversationId, MessageRef, MessageRef2);

		String uid2 = UUID.randomUUID().toString();
		Long conversationId2 = 88888888L;
		MessageRef MessageRef3 = new MessageRef();
		MessageRef3.folderUid = mboxUniqueId;
		MessageRef3.itemId = 111L;
		MessageRef3.date = new Date(3);
		MessageRef MessageRef4 = new MessageRef();
		MessageRef4.folderUid = mboxUniqueId;
		MessageRef4.itemId = 51L;
		MessageRef4.date = new Date(4);
		create(uid2, conversationId2, MessageRef3, MessageRef4);

		List<ItemValue<Conversation>> conversations = getService(SecurityContext.SYSTEM).byFolder(mboxUniqueId,
				ItemFlagFilter.all());
		assertNotNull(conversations);
		assertEquals(2, conversations.size());
	}

	@Test
	public void testRemoveMessage() {
		String uid = UUID.randomUUID().toString();
		Long conversationId = 123456789L;

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

		create(uid, conversationId, messageId, messageId2, messageId3, messageId4);

		IMailConversation service = getService(SecurityContext.SYSTEM);
		ItemValue<Conversation> byConversationId = service.byConversationId(conversationId);
		assertEquals(4, byConversationId.value.messageRefs.size());

		service.removeMessage(messageId2.folderUid, messageId2.itemId);

		byConversationId = service.byConversationId(conversationId);
		assertEquals(3, byConversationId.value.messageRefs.size());

	}

	@Test
	public void testRemoveLastMessage() {
		String uid = UUID.randomUUID().toString();
		Long conversationId = 123456789L;

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

		create(uid, conversationId, messageId, messageId2, messageId3, messageId4);

		IMailConversation service = getService(SecurityContext.SYSTEM);
		ItemValue<Conversation> byConversationId = service.byConversationId(conversationId);
		assertEquals(4, byConversationId.value.messageRefs.size());

		service.removeMessage(messageId.folderUid, messageId.itemId);
		service.removeMessage(messageId2.folderUid, messageId2.itemId);
		service.removeMessage(messageId3.folderUid, messageId3.itemId);
		service.removeMessage(messageId4.folderUid, messageId4.itemId);

		assertNull(service.byConversationId(conversationId));
	}

	protected IDbMessageBodies getBodies(SecurityContext ctx) {
		return ServerSideServiceProvider.getProvider(ctx).instance(IDbMessageBodies.class, partition);
	}

}
