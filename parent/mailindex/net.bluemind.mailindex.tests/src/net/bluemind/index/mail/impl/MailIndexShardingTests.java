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
package net.bluemind.index.mail.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Files;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.indices.GetAliasResponse;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import io.vertx.core.buffer.Buffer;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.indexing.IndexedMessageBody;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.task.service.NullTaskMonitor;
import net.bluemind.index.MailIndexActivator;
import net.bluemind.index.mail.MailIndexService;
import net.bluemind.index.mail.impl.IndexTestHelper.TestDomainOptions;
import net.bluemind.lib.elasticsearch.ESearchActivator;

public class MailIndexShardingTests {

	private String domainUid;
	private IndexTestHelper testHelper;
	private ElasticsearchClient client;

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
	public void testDefaultInstallationCreates25Indexes() throws Exception {
		populate(1);
		GetIndexResponse resp = client.indices().get(g -> g.index("mailspool*"));
		List<String> shards = resp.result().keySet().stream().filter(i -> !i.equals("mailspool_pending")).toList();

		assertEquals(25, shards.size());
	}

	@Test
	public void testNewAliasesGetDistributed() throws Exception {
		populate(50);

		List<String> shards = new ArrayList<>();
		for (int i = 1; i < 26; i++) {
			shards.add("mailspool_" + i);
		}
		Collections.sort(shards);

		for (int i = 0; i < 50; i++) {
			String uid = String.format("user%02d", i);
			assertEquals(shards.get(i % 25), getUserAliasIndex(uid));
		}
	}

	@Test
	public void testRepairRepairsAlias() throws Exception {
		populate(10);

		String uid = "user05";
		String index = getUserAliasIndex(uid);
		assertNotNull(index);
		client.indices().deleteAlias(d -> d.index(index).name(getIndexAliasName(uid)));
		assertFalse(getUserAliasExists(uid));

		new MailIndexService().repairMailbox(uid, new NullTaskMonitor());
		assertTrue(getUserAliasExists(uid));
	}

	@Test
	public void testMoveAlias() throws Exception {
		populate(3);

		String uid = "user02";
		String indexBefore = getUserAliasIndex(uid);
		assertEquals("mailspool_11", indexBefore);

		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		String bodyUid = UUID.randomUUID().toString();
		storeBody(bodyUid, eml);
		storeMessage(uid, uid, bodyUid, 1, Collections.emptyList());
		ESearchActivator.refreshIndex(indexBefore);

		SearchResponse<Void> resp = client.search(s -> s //
				.index(indexBefore) //
				.query(q -> q.queryString(st -> st.query("id:\"" + uid + ":" + 44 + "\""))), Void.class);
		assertEquals(1L, resp.hits().total().value());

		new MailIndexService().moveMailbox(uid, "mailspool_20");

		String indexAfter = getUserAliasIndex(uid);
		assertEquals("mailspool_20", indexAfter);

		resp = client.search(s -> s //
				.index(indexAfter) //
				.query(q -> q.queryString(st -> st.query("id:\"" + uid + ":" + 44 + "\""))), Void.class);
		assertEquals(1L, resp.hits().total().value());
	}

	private String getUserAliasIndex(String userUid) throws ElasticsearchException, IOException {
		GetAliasResponse t = client.indices().getAlias(a -> a.name(getIndexAliasName(userUid)));
		return t.result().keySet().iterator().next();
	}

	private boolean getUserAliasExists(String userUid) {
		try {
			return !client.indices().getAlias(a -> a.name(getIndexAliasName(userUid))).result().isEmpty();
		} catch (ElasticsearchException | IOException e) {
			return false;
		}
	}

	private String getIndexAliasName(String entityId) {
		return "mailspool_alias_" + entityId;
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

	private void storeMessage(String mailboxUniqueId, String userUid, String bodyUid, long imapUid,
			List<MailboxItemFlag> flags) {
		MailboxRecord mail = new MailboxRecord();
		mail.messageBody = bodyUid;
		mail.imapUid = imapUid;
		mail.flags = flags;

		ItemValue<MailboxRecord> item = new ItemValue<>();
		item.internalId = 44L;
		item.value = mail;
		MailIndexActivator.getService().storeMessage(mailboxUniqueId, item, userUid);
	}

}
