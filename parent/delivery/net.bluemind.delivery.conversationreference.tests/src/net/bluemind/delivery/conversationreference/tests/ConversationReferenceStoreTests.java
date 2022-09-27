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

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.delivery.conversationreference.api.IConversationReference;
import net.bluemind.delivery.conversationreference.persistence.ConversationReference;

public class ConversationReferenceStoreTests {

	IConversationReference conversationReferenceService;

	private SecurityContext secCtxUser1;

	private ConversationReferenceStoreWithExpiresDates dataStore;

	private Long mailboxId1 = 10L;
	private Long mailboxId2 = 20L;

	private Long hashMessage1Id;

	private long conversation1Id;

	private String message1Id;

	private long hashMessage2Id;

	private String message2Id;

	@Before
	public void before() throws Exception {
		message1Id = "<6a2b976c0f1f14876917aea6ebfb457f@f8de2c4a.internal>";
		message2Id = "<09f8c8e65442062c0d1d23022a5be532@f8de2c4a.internal>";
		String message3Id = "<09f8c8e65552062c0d1d23033a5be533@f8de2c4a.internal>";

		conversation1Id = -1060821470570927639L;
		Long conversation2Id = 8745557296093279025L;

		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());
		HashFunction hf = Hashing.sipHash24();
		hashMessage1Id = hf.hashBytes(message1Id.getBytes()).asLong();
		hashMessage2Id = hf.hashBytes(message2Id.getBytes()).asLong();
		long hashMessage3Id = hf.hashBytes(message3Id.getBytes()).asLong();

		dataStore = new ConversationReferenceStoreWithExpiresDates(
				JdbcTestHelper.getInstance().getMailboxDataDataSource()); // bj-data

		dataStore.create(ConversationReference.of(hashMessage1Id, conversation1Id, mailboxId1));
		dataStore.create(ConversationReference.of(hashMessage2Id, conversation1Id, mailboxId1));
		dataStore.create(ConversationReference.of(hashMessage1Id, conversation2Id, mailboxId2));
		dataStore.create(ConversationReference.of(hashMessage2Id, conversation2Id, mailboxId2));
		dataStore.insertWithExpires(mailboxId2, hashMessage3Id, conversation2Id);
	}

	@Test
	public void testForBadDatasource() throws Exception {
		var store = new ConversationReferenceStoreWithExpiresDates(JdbcActivator.getInstance().getDataSource()); // bj

		List<Long> list = new ArrayList<>();
		list.add(hashMessage1Id);
		list.add(hashMessage2Id);
		long conversationIdResult = store.get(mailboxId1, list);
		Assert.assertEquals(0L, conversationIdResult);

	}

	@Test
	public void testStoreForExistingConversationId() throws Exception {
		List<Long> list = new ArrayList<>();
		list.add(hashMessage1Id);
		list.add(hashMessage2Id);
		long conversationIdResult = dataStore.get(mailboxId1, list);
		Assert.assertEquals(conversation1Id, conversationIdResult);
	}

	@Test
	public void testStoreForNonExistingConversationId() throws Exception {
		List<Long> list = new ArrayList<>();
		list.add(hashMessage1Id);
		list.add(hashMessage2Id);
		long conversationIdResult = dataStore.get(30L, list);
		Assert.assertEquals(0L, conversationIdResult);

	}

	@Test
	public void testDeleteEntriesOlderThanOneYear() throws Exception {
		long deletedEntries = dataStore.deleteEntriesOlderThanOneYear();
		Assert.assertEquals(1L, deletedEntries);
	}

	protected IServiceProvider provider() {
		return ServerSideServiceProvider.getProvider(secCtxUser1);
	}

	protected String uniqueUidPart() {
		return System.currentTimeMillis() + "";
	}
}
