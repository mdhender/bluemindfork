/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.index.mail.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.elasticsearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.elasticsearch.client.Client;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Files;

import io.vertx.core.buffer.Buffer;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxItem.SystemFlag;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.indexing.IndexedMessageBody;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.index.MailIndexActivator;
import net.bluemind.index.mail.MailIndexService;
import net.bluemind.index.mail.integration.IndexTestHelper.TestDomainOptions;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.mailbox.api.ShardStats;
import net.bluemind.mailbox.api.ShardStats.State;

public class MailIndexShardingITTests {

	private String domainUid;
	private IndexTestHelper testHelper;
	private Client client;

	@Before
	public void setup() throws Exception {
		this.domainUid = "junit" + System.currentTimeMillis() + ".cli";
	}

	private void populate(int userCount) throws Exception {
		this.testHelper = IndexTestHelper.builder()//
				.withDomains(domainUid)//
				.withDomainOptions(TestDomainOptions.justUsers(userCount))//
				.build();
		testHelper.beforeTest();
		client = ESearchActivator.getClient();
	}

	@After
	public void teardown() throws Exception {
		testHelper.afterTest();
	}

	@Test
	public void testEmptyIndexShouldGetClassifiedAsEmpty() throws Exception {
		TestIndexSelectionPolicy.distribute = false;
		populate(1);

		List<ShardStats> stats = new MailIndexService().getStats();

		assertEquals(25, stats.size());
		stats.forEach(stat -> {
			assertEquals(State.OK, stat.state);
		});
	}

	@Test
	public void testAddingDataShouldAffectSize() throws Exception {
		TestIndexSelectionPolicy.distribute = false;
		populate(1);
		String index = getUserAliasIndex("user00");

		List<ShardStats> stats = new MailIndexService().getStats();
		assertEquals(25, stats.size());
		long size = -1;
		for (ShardStats stat : stats) {
			if (stat.indexName.equals(index)) {
				size = stat.size;
				break;
			}
		}

		for (int i = 0; i < 100; i++) {
			addMail("user00", index);
		}

		stats = new MailIndexService().getStats();
		assertEquals(25, stats.size());
		long newSize = -1;
		for (ShardStats stat : stats) {
			if (stat.indexName.equals(index)) {
				newSize = stat.size;
				break;
			}
		}

		assertTrue(size > 0);
		assertTrue(newSize > 0);
		assertTrue(newSize > size);
	}

	@Test
	public void testRunWhileOKState() throws Exception {
		TestIndexSelectionPolicy.distribute = false;
		populate(1);
		String index = getUserAliasIndex("user00");

		int count = 0;
		State state = State.OK;
		while (state == state.OK) {
			ShardStats stats = addEmailAndCheck("user00", index, 1000);
			count += 1000;
			System.err.println("Stored " + count + "messages. size: " + stats.size);
			state = stats.state;
		}

	}

	@Test
	public void testRunWhileNotFullState() throws Exception {
		TestIndexSelectionPolicy.distribute = false;
		populate(1);
		String index = getUserAliasIndex("user00");

		int count = 0;
		State state = State.OK;
		while (state == State.OK || state == State.HALF_FULL) {
			ShardStats stats = addEmailAndCheck("user00", index, 1000);
			count += 1000;
			System.err.println("Stored " + count + "messages. size: " + stats.size);
			state = stats.state;
		}
	}

	@Test
	public void testRunDistributedWhileOKState() throws Exception {
		TestIndexSelectionPolicy.distribute = true;
		populate(25);

		State state = State.OK;
		int currentUser = 0;
		int count = 0;
		while (state == State.OK) {
			String userUid = String.format("user%02d", currentUser++);
			String index = getUserAliasIndex(userUid);
			ShardStats stats = addEmailAndCheck(userUid, index, 1000);
			count += 1000;
			System.err.println("Stored " + count + "messages on 25 indexes. size: " + stats.size);
			state = stats.state;
		}

	}

	@Test
	public void testRunDistributedWhileNotFullState() throws Exception {
		TestIndexSelectionPolicy.distribute = true;
		populate(25);

		State state = State.OK;
		int currentUser = 0;
		int count = 0;
		while (state == State.OK || state == State.HALF_FULL) {
			String userUid = String.format("user%02d", currentUser++);
			String index = getUserAliasIndex(userUid);
			ShardStats stats = addEmailAndCheck(userUid, index, 1000);
			count += 1000;
			System.err.println("Stored " + count + "messages on 25 indexes. size: " + stats.size);
			state = stats.state;
		}

	}

	private ShardStats addEmailAndCheck(String uid, String index, int count) throws IOException {
		for (int i = 0; i < count; i++) {
			addMail(uid, index);
		}
		return new MailIndexService().getStats().stream().filter(stat -> stat.indexName.equals(index)).findFirst()
				.get();
	}

	private String getUserAliasIndex(String userUid) {
		GetAliasesResponse t = client.admin().indices().prepareGetAliases(getIndexAliasName(userUid)).execute()
				.actionGet();

		return t.getAliases().keysIt().next();
	}

	private String getIndexAliasName(String entityId) {
		return "mailspool_alias_" + entityId;
	}

	private void addMail(String userUid, String index) throws IOException {
		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		String bodyUid = UUID.randomUUID().toString();
		storeBody(bodyUid, eml);
		storeMessage(userUid, userUid, "INBOX", bodyUid, 1, Collections.emptyList());
	}

	private void storeBody(String uid, byte[] eml) {
		AtomicBoolean done = new AtomicBoolean(false);
		GenericStream<byte[]> stream = new GenericStream<byte[]>() {

			@Override
			protected Buffer serialize(byte[] n) throws Exception {
				return Buffer.buffer(n);
			}

			@Override
			protected StreamState<byte[]> next() throws Exception {
				if (!done.get()) {
					done.set(true);
					return StreamState.data(eml);
				} else {
					return StreamState.end();
				}
			}

		};
		try {
			IndexedMessageBody forIndexing = IndexedMessageBody.createIndexBody(uid, VertxStream.stream(stream));
			MailIndexActivator.getService().storeBody(forIndexing);
		} catch (ServerFault sf) {
			throw sf;
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	private void storeMessage(String mailboxUniqueId, String userUid, String folderUid, String bodyUid, long imapUid,
			Collection<SystemFlag> flags) {
		MailboxFolder folderValue = new MailboxFolder();
		ItemValue<MailboxFolder> folder = ItemValue.create(folderUid, folderValue);
		MailboxRecord mail = new MailboxRecord();
		mail.messageBody = bodyUid;
		mail.imapUid = imapUid;
		mail.systemFlags = flags;

		ItemValue<MailboxRecord> item = new ItemValue<>();
		item.internalId = 44L;
		item.value = mail;
		MailIndexActivator.getService().storeMessage(mailboxUniqueId, item, userUid);
	}

}
