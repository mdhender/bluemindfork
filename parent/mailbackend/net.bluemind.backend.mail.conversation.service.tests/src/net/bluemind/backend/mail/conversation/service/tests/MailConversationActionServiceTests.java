/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import net.bluemind.backend.mail.api.IMailConversation;
import net.bluemind.backend.mail.api.IMailConversationActions;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.flags.ConversationFlagUpdate;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxRecord.InternalFlag;
import net.bluemind.backend.mail.replica.service.ReplicationEvents;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;

public class MailConversationActionServiceTests extends AbstractConversationTests {
	@Test
	public void testAddFlag() throws Exception {
		IMailConversation user1ConversationService = getConversationService(user1Uid);
		IMailboxFolders user1MboxesApi = provider(user1Uid).instance(IMailboxFolders.class, partition, user1MboxRoot);
		//
		// simulate user1 sends to user2 (should generate a conversation for
		// user1 in Sent)
		//
		createEml("data/user1_send_to_user2.eml", user2Uid, user2MboxRoot, "INBOX");
		//
		// simulate user2 replies to user1 (should generate a conversation for
		// user1 in Inbox)
		//
		createEml("data/user2_reply_to_user1.eml", user2Uid, user2MboxRoot, "Sent");
		//
		// simulate user1 sends another one to user2 (should not generate more
		// conversation for user1 in Inbox, but one more in Sent)
		//
		createEml("data/user1_send_another_to_user2.eml", user1Uid, user1MboxRoot, "Sent");
		createEml("data/user1_send_another_to_user2.eml", user2Uid, user2MboxRoot, "INBOX");
		//
		// place an additional random email in Sent
		createEml("data/user1_send_another_to_user2_2.eml", user1Uid, user1MboxRoot, "Sent");

		ItemValue<MailboxFolder> user1Sent = user1MboxesApi.byName("Sent");
		List<String> user1SentConversations = user1ConversationService.byFolder(user1Sent.uid,
				createSortDescriptor(ItemFlagFilter.all()));

		IDbMailboxRecords records = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDbMailboxRecords.class, user1Sent.uid);
		List<ItemValue<MailboxRecord>> all = records.all();
		assertEquals(2, all.size());
		for (ItemValue<MailboxRecord> rec : all) {
			assertFalse(rec.value.flags.contains(MailboxItemFlag.System.Seen.value()));
		}

		ConversationFlagUpdate flagUpdate = new ConversationFlagUpdate();
		flagUpdate.conversationUids = Arrays.asList(user1SentConversations.get(0));
		flagUpdate.mailboxItemFlag = MailboxItemFlag.System.Seen.value();
		Ack addFlag = getConversationActionsService(user1Uid, user1Sent.uid).addFlag(flagUpdate);
		assertTrue(addFlag.version > 0);

		List<String> unseenConversations = user1ConversationService.byFolder(user1Sent.uid,
				createSortDescriptor(ItemFlagFilter.create().mustNot(ItemFlag.Deleted, ItemFlag.Seen)));
		assertEquals(1, unseenConversations.size());

		all = records.all();
		assertEquals(2, all.size());
		int seen = 0;
		for (ItemValue<MailboxRecord> rec : all) {
			if (rec.value.flags.contains(MailboxItemFlag.System.Seen.value())) {
				seen++;
			}
		}
		assertEquals(1, seen);

		flagUpdate = new ConversationFlagUpdate();
		flagUpdate.conversationUids = Arrays.asList(user1SentConversations.get(0));
		flagUpdate.mailboxItemFlag = MailboxItemFlag.System.Flagged.value();
		addFlag = getConversationActionsService(user1Uid, user1Sent.uid).addFlag(flagUpdate);
		assertTrue(addFlag.version > 0);

		List<String> importantConversations = user1ConversationService.byFolder(user1Sent.uid,
				createSortDescriptor(ItemFlagFilter.create().mustNot(ItemFlag.Deleted).must(ItemFlag.Important)));
		assertEquals(1, importantConversations.size());
	}

	@Test
	public void testRemoveFlag() throws Exception {
		IMailboxFolders user1MboxesApi = provider(user1Uid).instance(IMailboxFolders.class, partition, user1MboxRoot);
		IMailConversation user1ConversationService = getConversationService(user1Uid);
		ItemValue<MailboxFolder> user1Sent = user1MboxesApi.byName("Sent");
		IMailConversationActions user1ConversationActionService = getConversationActionsService(user1Uid,
				user1Sent.uid);

		//
		// simulate user1 sends to user2 (should generate a conversation for
		// user1 in Sent)
		//

		createEml("data/user1_send_to_user2.eml", user1Uid, user1MboxRoot, "Sent");

		ItemFlagFilter mustNotSeen = ItemFlagFilter.create().mustNot(ItemFlag.Deleted, ItemFlag.Seen);

		List<String> user1SentConversations = user1ConversationService.byFolder(user1Sent.uid,
				createSortDescriptor(ItemFlagFilter.all()));
		List<String> user1SentConversationsSeen = user1ConversationService.byFolder(user1Sent.uid,
				createSortDescriptor(mustNotSeen));
		assertEquals(1, user1SentConversations.size());
		assertEquals(1, user1SentConversationsSeen.size());

		ConversationFlagUpdate flagUpdate = new ConversationFlagUpdate();
		flagUpdate.conversationUids = Arrays.asList(user1SentConversations.get(0));
		flagUpdate.mailboxItemFlag = MailboxItemFlag.System.Seen.value();
		Ack addFlag = getConversationActionsService(user1Uid, user1Sent.uid).addFlag(flagUpdate);
		assertTrue(addFlag.version > 0);

		user1SentConversations = user1ConversationService.byFolder(user1Sent.uid,
				createSortDescriptor(ItemFlagFilter.all()));
		user1SentConversationsSeen = user1ConversationService.byFolder(user1Sent.uid,
				createSortDescriptor(mustNotSeen));
		assertEquals(1, user1SentConversations.size());
		assertEquals(0, user1SentConversationsSeen.size());

		Ack removeFlag = user1ConversationActionService.deleteFlag(flagUpdate);
		assertTrue(removeFlag.version > 0);

		user1SentConversations = user1ConversationService.byFolder(user1Sent.uid,
				createSortDescriptor(ItemFlagFilter.all()));
		user1SentConversationsSeen = user1ConversationService.byFolder(user1Sent.uid,
				createSortDescriptor(mustNotSeen));
		assertEquals(1, user1SentConversations.size());
		assertEquals(1, user1SentConversationsSeen.size());
	}

	@Test
	public void testMove() throws Exception {
		IMailConversation user1ConversationService = getConversationService(user1Uid);
		IMailboxFolders user1MboxesApi = provider(user1Uid).instance(IMailboxFolders.class, partition, user1MboxRoot);

		createEml("data/user1_send_to_user2.eml", user1Uid, user1MboxRoot, "INBOX");
		createEml("data/user1_send_another_to_user2.eml", user1Uid, user1MboxRoot, "INBOX");

		ItemValue<MailboxFolder> user1Inbox = user1MboxesApi.byName("INBOX");
		ItemValue<MailboxFolder> user1Sent = user1MboxesApi.byName("Sent");

		IDbMailboxRecords records = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDbMailboxRecords.class, user1Inbox.uid);
		IDbMailboxRecords recordsSent = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDbMailboxRecords.class, user1Sent.uid);

		assertEquals(2, records.all().size());
		assertEquals(0, recordsSent.all().size());

		List<ItemIdentifier> moved = getConversationActionsService(user1Uid, user1Inbox.uid).move(user1Sent.uid,
				user1ConversationService.byFolder(user1Inbox.uid, createSortDescriptor(ItemFlagFilter.all())));

		assertEquals(2, moved.size());

		List<ItemValue<MailboxRecord>> allInInbox = records.all();
		assertEquals(2, allInInbox.size());
		for (ItemValue<MailboxRecord> rec : allInInbox) {
			assertTrue(rec.value.flags.contains(MailboxItemFlag.System.Deleted.value()));
		}
		assertEquals(2, recordsSent.all().size());
	}

	@Test
	public void multipleDelete() throws IOException, InterruptedException, TimeoutException, ExecutionException {
		IMailConversation user1ConversationService = getConversationService(user1Uid);
		IMailboxFolders user1MboxesApi = provider(user1Uid).instance(IMailboxFolders.class, partition, user1MboxRoot);

		createEml("data/user1_send_another_to_user2.eml", user1Uid, user1MboxRoot, "Sent");
		createEml("data/user1_send_another_to_user2_2.eml", user1Uid, user1MboxRoot, "Sent");

		ItemValue<MailboxFolder> user1Sent = user1MboxesApi.byName("Sent");
		List<String> user1SentConversations = user1ConversationService.byFolder(user1Sent.uid,
				createSortDescriptor(ItemFlagFilter.all()));
		IDbMailboxRecords records = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDbMailboxRecords.class, user1Sent.uid);

		CompletableFuture<?> onMailboxChanged = ReplicationEvents.onMailboxChanged(user1Sent.uid);
		getConversationActionsService(user1Uid, user1Sent.uid).multipleDeleteById(user1SentConversations);
		onMailboxChanged.get(3L, TimeUnit.SECONDS);

		List<ItemValue<MailboxRecord>> all = records.all();
		assertEquals(2, all.size());

		for (ItemValue<MailboxRecord> rec : all) {
			if (rec.value.internalFlags.contains(InternalFlag.expunged)) {
				assertTrue(rec.value.flags.contains(MailboxItemFlag.System.Deleted.value()));
				assertTrue(rec.value.internalFlags.contains(InternalFlag.expunged));
			}
		}
	}
}