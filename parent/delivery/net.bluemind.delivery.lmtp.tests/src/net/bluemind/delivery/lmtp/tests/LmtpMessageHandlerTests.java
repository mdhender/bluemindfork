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
package net.bluemind.delivery.lmtp.tests;

import java.io.InputStream;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.delivery.conversationreference.persistence.ConversationReference;
import net.bluemind.delivery.conversationreference.persistence.ConversationReferenceStore;
import net.bluemind.delivery.lmtp.ApiProv;
import net.bluemind.delivery.lmtp.LmtpMessageHandler;
import net.bluemind.delivery.lmtp.MailboxLookup;
import net.bluemind.delivery.lmtp.common.ResolvedBox;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class LmtpMessageHandlerTests {

	private String emailUser1;
	private String emailUser2;

	private long conversation2Id;
	private ItemValue<Mailbox> mboxUser1;
	private ItemValue<Mailbox> mboxUser2;

	private MailboxLookup lookup;
	private SecurityContext defaultSecurityContext;
	private BmTestContext context;

	@Before
	public void before() throws Exception {
		String domainUid = "test" + System.currentTimeMillis() + ".lab";
		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		long conversation1Id = -1060821470570927639L;
		conversation2Id = -1099921370570928545L;

		String message1Id = "<CAKVJn50ZRwe_UpM9geC4dyoRFFtMVWp0WHDhBo4KxNd4H6OZ6Q@bluemind.net>";
		String message3Id = "<CAKVJn50K+4Ei4DNGNUjDXwKTHVWarUrrC-YkB2Z9ooLPqeDCCg@bluemind.net>";
		String message4Id = "<l7yj2hvp.38wakz4kixtz4@bluemind.net>";

		Server pipo = new Server();
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;
		pipo.tags = Collections.singletonList("mail/imap");

		HashFunction hf = Hashing.sipHash24();
		long hashMessage1Id = hf.hashBytes(message1Id.getBytes()).asLong();
		long hashMessage3Id = hf.hashBytes(message3Id.getBytes()).asLong();
		long hashMessage4Id = hf.hashBytes(message4Id.getBytes()).asLong();

		VertxPlatform.spawnBlocking(25, TimeUnit.SECONDS);

		PopulateHelper.initGlobalVirt(pipo);
		PopulateHelper.addDomain(domainUid, Routing.none);
		String user1Uid = PopulateHelper.addUser("user1", domainUid, Routing.internal);
		String user2Uid = PopulateHelper.addUser("user2", domainUid, Routing.internal);

		mboxUser1 = systemServiceProvider().instance(IMailboxes.class, domainUid).getComplete(user1Uid);
		mboxUser2 = systemServiceProvider().instance(IMailboxes.class, domainUid).getComplete(user2Uid);
		Assert.assertNotNull(mboxUser1);
		Assert.assertNotNull(mboxUser2);

		emailUser1 = mboxUser1.value.defaultEmail().address;
		emailUser2 = mboxUser2.value.defaultEmail().address;

		defaultSecurityContext = BmTestContext
				.contextWithSession("testUser", "user2", domainUid, SecurityContext.ROLE_SYSTEM).getSecurityContext();
		context = new BmTestContext(defaultSecurityContext);

		ConversationReferenceStore store = new ConversationReferenceStore(
				JdbcTestHelper.getInstance().getMailboxDataDataSource());

		store.create(ConversationReference.of(hashMessage1Id, conversation1Id, mboxUser1.internalId));
		store.create(ConversationReference.of(hashMessage3Id, conversation1Id, mboxUser1.internalId));
		store.create(ConversationReference.of(hashMessage1Id, conversation2Id, mboxUser2.internalId));
		store.create(ConversationReference.of(hashMessage4Id, conversation2Id, mboxUser2.internalId));

		ApiProv prov = s -> ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		this.lookup = new MailboxLookup(prov);
		Assert.assertNotNull(lookup);
	}

	@Test
	public void testSavedMailHasRightConversationId() throws Exception {
		ApiProv prov = k -> context.getServiceProvider();
		LmtpMessageHandler messageHandler = new LmtpMessageHandler(prov);
		messageHandler.deliver(emailUser1, emailUser2, eml("emls/test_mail.eml"));

		ResolvedBox tgtBox = lookup.lookupEmail(emailUser2);
		Assert.assertNotNull(tgtBox);
		String subtree = IMailReplicaUids.subtreeUid(tgtBox.dom.uid, tgtBox.mbox);
		IDbReplicatedMailboxes treeApi = systemServiceProvider().instance(IDbByContainerReplicatedMailboxes.class,
				subtree);
		Assert.assertNotNull(treeApi);
		ItemValue<MailboxReplica> rootFolder = treeApi
				.byReplicaName(tgtBox.mbox.value.type.sharedNs ? tgtBox.mbox.value.name : "INBOX");
		Assert.assertNotNull(rootFolder);
		IDbMailboxRecords recs = systemServiceProvider().instance(IDbMailboxRecords.class, rootFolder.uid);
		ItemValue<MailboxRecord> mail = recs.getCompleteByImapUid(1L);
		Assert.assertNotNull(mail);
		Assert.assertEquals(conversation2Id, (long) mail.value.conversationId);
	}

	@Test
	public void testSavedMailwithoutReference() throws Exception {
		ApiProv prov = k -> context.getServiceProvider();
		LmtpMessageHandler messageHandler = new LmtpMessageHandler(prov);
		messageHandler.deliver(emailUser1, emailUser2, eml("emls/test_mail_bad_references.eml"));

		ResolvedBox tgtBox = lookup.lookupEmail(emailUser2);
		Assert.assertNotNull(tgtBox);
		String subtree = IMailReplicaUids.subtreeUid(tgtBox.dom.uid, tgtBox.mbox);
		IDbReplicatedMailboxes treeApi = systemServiceProvider().instance(IDbByContainerReplicatedMailboxes.class,
				subtree);
		Assert.assertNotNull(treeApi);
		ItemValue<MailboxReplica> rootFolder = treeApi
				.byReplicaName(tgtBox.mbox.value.type.sharedNs ? tgtBox.mbox.value.name : "INBOX");
		Assert.assertNotNull(rootFolder);
		IDbMailboxRecords recs = systemServiceProvider().instance(IDbMailboxRecords.class, rootFolder.uid);
		ItemValue<MailboxRecord> mail = recs.getCompleteByImapUid(1L);
		Assert.assertNotNull(mail);
		Assert.assertNotEquals(conversation2Id, (long) mail.value.conversationId);
	}

	protected IServiceProvider systemServiceProvider() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	}

	private InputStream eml(String resPath) {
		return LmtpMessageHandlerTests.class.getClassLoader().getResourceAsStream(resPath);
	}
}
