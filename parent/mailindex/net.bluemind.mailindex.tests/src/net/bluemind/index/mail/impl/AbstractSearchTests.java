/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.junit.Before;

import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import io.vertx.core.buffer.Buffer;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.indexing.IMailIndexService.BulkOp;
import net.bluemind.backend.mail.replica.indexing.IndexedMessageBody;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.index.MailIndexActivator;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.elasticsearch.IndexAliasMode;
import net.bluemind.lib.elasticsearch.IndexAliasMode.Mode;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.tests.defaultdata.PopulateHelper;

public abstract class AbstractSearchTests {

	protected String bodyUid = UUID.randomUUID().toString();
	protected String bodyUid1 = UUID.randomUUID().toString();
	protected String mboxUid = UUID.randomUUID().toString();
	protected String userUid = UUID.randomUUID().toString();
	protected String userUid2 = UUID.randomUUID().toString();
	protected String mboxUid2 = UUID.randomUUID().toString();
	protected String folderUid = mboxUid;
	public String ALIAS = "mailspool_alias_" + userUid;
	public String ALIAS2 = "mailspool_alias_" + userUid2;
	protected String INDEX_NAME = "mailspool_1";

	@Before
	public void before() throws Exception {
		ElasticsearchTestHelper.getInstance().beforeTest(5);
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		System.out.println(esServer.ip);
		esServer.tags = Lists.newArrayList(TagDescriptor.bm_es.getTag());

		PopulateHelper.initGlobalVirt(esServer);
		// PopulateHelper.createTestDomain(domainUid, esServer);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		System.out.println("Ensuring index exists....");
		ElasticsearchClient esClient = ESearchActivator.getClient();

		if (IndexAliasMode.getMode() == Mode.ONE_TO_ONE) {
			esClient.indices().putAlias(a -> a //
					.index(INDEX_NAME).name(ALIAS).filter(f -> f.term(t -> t.field("owner").value(userUid))));
			esClient.indices().putAlias(a -> a //
					.index(INDEX_NAME).name(ALIAS2).filter(f -> f.term(t -> t.field("owner").value(userUid2))));
			esClient.deleteByQuery(d -> d //
					.index(INDEX_NAME).query(QueryBuilders.queryString(q -> q.query("in:" + folderUid))));
		}

		System.out.println("Bootstrap finished....");
	}

	protected void addEml(long imapUid, String path, long itemId, MailboxItemFlag.System... flags) throws IOException {
		byte[] eml = Files.toByteArray(new File(path));
		HashCode hash = Hashing.goodFastHash(128).hashBytes(eml);
		String emlUid = hash.toString();
		storeBody(emlUid, eml);
		storeMessage(mboxUid, userUid, emlUid, imapUid,
				Arrays.stream(flags).map(MailboxItemFlag.System::value).collect(Collectors.toList()), itemId);
		ESearchActivator.refreshIndex(INDEX_NAME);
	}

	protected void storeBody(String uid, byte[] eml) {
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

	protected void storeMessage(String mailboxUniqueId, String userUid, String bodyUid, long imapUid,
			List<MailboxItemFlag> flags) {
		storeMessage(mailboxUniqueId, userUid, bodyUid, imapUid, flags, 44l);
	}

	protected void storeMessage(String mailboxUniqueId, String userUid, String bodyUid, long imapUid,
			List<MailboxItemFlag> flags, long itemId) {
		MailboxRecord mail = new MailboxRecord();
		mail.messageBody = bodyUid;
		mail.imapUid = imapUid;
		mail.flags = flags;

		ItemValue<MailboxRecord> item = new ItemValue<>();
		item.internalId = itemId;
		item.value = mail;
		MailIndexActivator.getService().storeMessage(mailboxUniqueId, item, userUid);
	}

	protected List<BulkOp> bulkMessage(String mailboxUniqueId, String userUid, String bodyUid, long imapUid,
			List<MailboxItemFlag> flags) {
		return bulkMessage(mailboxUniqueId, userUid, bodyUid, imapUid, flags, 44l);
	}

	protected List<BulkOp> bulkMessage(String mailboxUniqueId, String userUid, String bodyUid, long imapUid,
			List<MailboxItemFlag> flags, long itemId) {
		MailboxRecord mail = new MailboxRecord();
		mail.messageBody = bodyUid;
		mail.imapUid = imapUid;
		mail.flags = flags;

		ItemValue<MailboxRecord> item = new ItemValue<>();
		item.internalId = itemId;
		item.value = mail;
		return MailIndexActivator.getService().storeMessage(mailboxUniqueId, item, userUid, true);
	}

	protected String entryId(long imapUid) {
		return folderUid + ":" + imapUid;
	}

}
