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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.backend.mail.api.IMailConversation;
import net.bluemind.backend.mail.api.IMailConversationActions;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.flags.ConversationFlagUpdate;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IInternalMailConversation;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.IReplicatedMailboxesRootMgmt;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxRecord.InternalFlag;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.service.ReplicationEvents;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class MailConversationActionServiceTests extends AbstractRollingReplicationTests {

	@Before
	public void before() throws Exception {
		super.before();
		MailboxReplicaRootDescriptor rd = new MailboxReplicaRootDescriptor();
		rd.ns = Namespace.users;
		rd.name = userUid;
		IReplicatedMailboxesRootMgmt subtreeMgmt = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IReplicatedMailboxesRootMgmt.class, partition);
		subtreeMgmt.create(rd);
	}

	@After
	public void tearDown() throws Exception {
		super.after();
	}

	protected IMailConversationActions getActionService(String replicatedMailBoxUid) {
		BmTestContext testCtx = BmTestContext.contextWithSession("test-sid", userUid, domainUid);
		return ServerSideServiceProvider.getProvider(testCtx).instance(IMailConversationActions.class,
				IMailReplicaUids.conversationSubtreeUid(domainUid, userUid), replicatedMailBoxUid);
	}

	protected IInternalMailConversation getService(String replicatedMailBoxUid) {
		BmTestContext testCtx = BmTestContext.contextWithSession("test-sid", userUid, domainUid);
		return ServerSideServiceProvider.getProvider(testCtx).instance(IInternalMailConversation.class,
				IMailReplicaUids.conversationSubtreeUid(domainUid, userUid), replicatedMailBoxUid);
	}

	public static void main(String[] agrs) {
		long d = 54543534534l;
		String s = Long.toHexString(d);
		long rev = Long.parseUnsignedLong(s, 16);
		System.err.println(rev);
	}

	@Test
	public void testAddFlag() throws Exception {
		String user2Uid = PopulateHelper.addUser("user2", domainUid, Routing.internal);
		IMailConversation user1ConversationService = provider().instance(IMailConversation.class,
				IMailReplicaUids.conversationSubtreeUid(domainUid, userUid));
		IMailboxFolders user1MboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		//
		// simulate user1 sends to user2 (should generate a conversation for
		// user1 in Sent)
		//
		String user2MboxRoot = "user." + user2Uid.replace('.', '^');
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
		createEml("data/user1_send_another_to_user2.eml", userUid, mboxRoot, "Sent");
		createEml("data/user1_send_another_to_user2.eml", user2Uid, user2MboxRoot, "INBOX");
		//
		// place an additional random email in Sent
		createEml("data/user1_send_another_to_user2_2.eml", userUid, mboxRoot, "Sent");

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
		Ack addFlag = getActionService(user1Sent.uid).addFlag(flagUpdate);
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
		addFlag = getActionService(user1Sent.uid).addFlag(flagUpdate);
		assertTrue(addFlag.version > 0);

		List<String> importantConversations = user1ConversationService.byFolder(user1Sent.uid,
				createSortDescriptor(ItemFlagFilter.create().mustNot(ItemFlag.Deleted).must(ItemFlag.Important)));
		assertEquals(1, importantConversations.size());
	}

	@Test
	public void testRemoveFlag() throws Exception {
		IMailConversation user1ConversationService = provider().instance(IMailConversation.class,
				IMailReplicaUids.conversationSubtreeUid(domainUid, userUid));
		IMailboxFolders user1MboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		//
		// simulate user1 sends to user2 (should generate a conversation for
		// user1 in Sent)
		//
		String userMboxRoot = "user." + userUid.replace('.', '^');
		createEml("data/user1_send_to_user2.eml", userUid, userMboxRoot, "Sent");

		ItemValue<MailboxFolder> user1Sent = user1MboxesApi.byName("Sent");
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
		Ack addFlag = getActionService(user1Sent.uid).addFlag(flagUpdate);
		assertTrue(addFlag.version > 0);

		user1SentConversations = user1ConversationService.byFolder(user1Sent.uid,
				createSortDescriptor(ItemFlagFilter.all()));
		user1SentConversationsSeen = user1ConversationService.byFolder(user1Sent.uid,
				createSortDescriptor(mustNotSeen));
		assertEquals(1, user1SentConversations.size());
		assertEquals(0, user1SentConversationsSeen.size());

		Ack removeFlag = getActionService(user1Sent.uid).deleteFlag(flagUpdate);
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
		IMailConversation user1ConversationService = provider().instance(IMailConversation.class,
				IMailReplicaUids.conversationSubtreeUid(domainUid, userUid));
		IMailboxFolders user1MboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);

		createEml("data/user1_send_to_user2.eml", userUid, mboxRoot, "INBOX");
		createEml("data/user1_send_another_to_user2.eml", userUid, mboxRoot, "INBOX");

		ItemValue<MailboxFolder> user1Inbox = user1MboxesApi.byName("INBOX");
		ItemValue<MailboxFolder> user1Sent = user1MboxesApi.byName("Sent");

		IDbMailboxRecords records = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDbMailboxRecords.class, user1Inbox.uid);
		IDbMailboxRecords recordsSent = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDbMailboxRecords.class, user1Sent.uid);

		assertEquals(2, records.all().size());
		assertEquals(0, recordsSent.all().size());

		List<ItemIdentifier> moved = getActionService(user1Inbox.uid).move(user1Sent.uid,
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
		IMailConversation user1ConversationService = provider().instance(IMailConversation.class,
				IMailReplicaUids.conversationSubtreeUid(domainUid, userUid));
		IMailboxFolders user1MboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);

		createEml("data/user1_send_another_to_user2.eml", userUid, mboxRoot, "Sent");
		createEml("data/user1_send_another_to_user2_2.eml", userUid, mboxRoot, "Sent");

		ItemValue<MailboxFolder> user1Sent = user1MboxesApi.byName("Sent");
		List<String> user1SentConversations = user1ConversationService.byFolder(user1Sent.uid,
				createSortDescriptor(ItemFlagFilter.all()));
		IDbMailboxRecords records = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDbMailboxRecords.class, user1Sent.uid);

		CompletableFuture<?> onMailboxChanged = ReplicationEvents.onMailboxChanged(user1Sent.uid);
		getActionService(user1Sent.uid).multipleDeleteById(user1SentConversations);
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
