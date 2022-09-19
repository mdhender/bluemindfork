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
package net.bluemind.delivery.conversationreference.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.delivery.conversationreference.api.IConversationReference;
import net.bluemind.delivery.conversationreference.persistence.ConversationReference;
import net.bluemind.delivery.conversationreference.persistence.ConversationReferenceStore;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class ConversationReferenceServiceTests {

	IConversationReference conversationReferenceService;

	Long mailboxId1 = 10L;
	Long mailboxId2 = 20L;

	private String domainUid;

	private String message1Id;

	private long conversation1Id;

	private long conversation2Id;

	private Long hashMessage1Id;

	private String ownerUid;

	private ConversationReferenceStore store;

	@Before
	public void before() throws Exception {
		domainUid = "test" + System.currentTimeMillis() + ".lab";
		message1Id = "<6a2b976c0f1f14876917aea6ebfb457f@f8de2c4a.internal>";

		conversation1Id = -1060821470570927639L;
		conversation2Id = -1099921370570928545L;

		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		HashFunction hf = Hashing.sipHash24();
		hashMessage1Id = hf.hashBytes(message1Id.getBytes()).asLong();

		Server srv = new Server();
		srv.tags = Collections.singletonList("mail/imap");
		srv.ip = "10.1.2.3";

		PopulateHelper.initGlobalVirt(srv);
		PopulateHelper.addDomain(domainUid, Routing.none);
		ownerUid = PopulateHelper.addUser("john", domainUid, Routing.internal);

		ItemValue<Mailbox> mbox = systemServiceProvider().instance(IMailboxes.class, domainUid).getComplete(ownerUid);
		long mailboxId1 = mbox.internalId;

		store = new ConversationReferenceStore(JdbcTestHelper.getInstance().getMailboxDataDataSource());

		store.create(ConversationReference.of(hashMessage1Id, conversation1Id, mailboxId1));
		store.create(ConversationReference.of(hashMessage1Id, conversation2Id, mailboxId1));
	}

	@Test
	public void testServiceWorks() throws Exception {
		Long result = convRefService().lookup(message1Id, Collections.<String>emptySet());
		assertNotNull(result);
		assertEquals(conversation1Id, result.longValue());
	}

	@Test
	public void testNonExistingMessageId() throws Exception {
		Long result = convRefService().lookup("<6a3b986c0f1f14876917aea6ebfb457f@f8de2c4a.internal>",
				Collections.emptySet());
		assertNotNull(result);
		assertNotEquals(conversation1Id, result.longValue());
	}

	@Test
	public void testWithEmptyList() throws Exception {
		Long result = convRefService().lookup(null, Collections.emptySet());
		assertNotNull(result);
		assertNotEquals(conversation1Id, result.longValue());
	}

	private IConversationReference convRefService() {
		return systemServiceProvider().instance(IConversationReference.class, domainUid, ownerUid);
	}

	protected IServiceProvider systemServiceProvider() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

	}

}
