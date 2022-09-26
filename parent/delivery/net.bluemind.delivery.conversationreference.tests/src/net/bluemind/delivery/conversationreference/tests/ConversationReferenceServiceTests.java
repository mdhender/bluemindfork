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
import java.util.Set;

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

	Long mailboxUseraId = 10L;
	Long mailboxUserbId = 20L;

	private String domainUid;

	private String message1Id;

	private long conversation1Id;

	private long conversation2Id;

	private Long hashMessage1Id;
	private long hashMessage2Id;

	private ConversationReferenceStore store;

	private String message2Id;

	private String user1;

	private String user2;

	@Before
	public void before() throws Exception {
		domainUid = "test" + System.currentTimeMillis() + ".lab";
		message1Id = "<6a2b976c0f1f14876917aea6ebfb457f@f8de2c4a.internal>";
		message2Id = "<5a2b977c0f1f14876917aea6ebfb456a@f8de2c4a.internal>";

		conversation1Id = -1060821470570927639L;
		conversation2Id = -1099921370570928545L;

		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		HashFunction hf = Hashing.sipHash24();
		hashMessage1Id = hf.hashBytes(message1Id.getBytes()).asLong();
		hashMessage2Id = hf.hashBytes(message2Id.getBytes()).asLong();

		Server srv = new Server();
		srv.tags = Collections.singletonList("mail/imap");
		srv.ip = "10.1.2.3";

		PopulateHelper.initGlobalVirt(srv);
		PopulateHelper.addDomain(domainUid, Routing.none);
		user1 = PopulateHelper.addUser("user1", domainUid, Routing.internal);
		user2 = PopulateHelper.addUser("user2", domainUid, Routing.internal);

		ItemValue<Mailbox> mboxUser1 = systemServiceProvider().instance(IMailboxes.class, domainUid).getComplete(user1);
		ItemValue<Mailbox> mboxUser2 = systemServiceProvider().instance(IMailboxes.class, domainUid).getComplete(user2);
		long mailboxIdUser1 = mboxUser1.internalId;
		long mailboxIdUser2 = mboxUser2.internalId;

		store = new ConversationReferenceStore(JdbcTestHelper.getInstance().getMailboxDataDataSource());

		store.create(ConversationReference.of(hashMessage1Id, conversation1Id, mailboxIdUser1));
		store.create(ConversationReference.of(hashMessage2Id, conversation1Id, mailboxIdUser1));
		store.create(ConversationReference.of(hashMessage1Id, conversation2Id, mailboxIdUser2));
		store.create(ConversationReference.of(hashMessage2Id, conversation2Id, mailboxIdUser2));
	}

	@Test
	public void testServiceForUser1() throws Exception {
		Long result = convRefServiceUser1().lookup("<6a3b986c0f1f14876917aea6ebfb457f@f8de2c4a.internal>",
				Set.of(message1Id));
		assertNotNull(result);
		assertEquals(conversation1Id, result.longValue());
	}

	@Test
	public void testServiceForUser2() throws Exception {
		Long result = convRefServiceUser2().lookup("<6a3b986c0f1f14876917aea6ebfb457f@f8de2c4a.internal>",
				Set.of(message1Id));
		assertNotNull(result);
		assertEquals(conversation2Id, result.longValue());
	}

	@Test
	public void testNonExistingMessageId() throws Exception {
		Long result = convRefServiceUser1().lookup("<6a3b986c0f1f14876917aea6ebfb457f@f8de2c4a.internal>",
				Collections.emptySet());
		assertNotNull(result);
		assertNotEquals(conversation1Id, result.longValue());
		assertNotEquals(conversation2Id, result.longValue());
	}

	@Test
	public void testWithEmptyList() throws Exception {
		Long result = convRefServiceUser1().lookup(null, Collections.emptySet());
		assertNotNull(result);
		assertNotEquals(conversation1Id, result.longValue());
		assertNotEquals(conversation2Id, result.longValue());
	}

	private IConversationReference convRefServiceUser1() {
		return systemServiceProvider().instance(IConversationReference.class, domainUid, user1);
	}

	private IConversationReference convRefServiceUser2() {
		return systemServiceProvider().instance(IConversationReference.class, domainUid, user2);
	}

	protected IServiceProvider systemServiceProvider() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

	}

}
