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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Before;

import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.api.flags.SystemFlag;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
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
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class AbstractSearchTests {

	protected String bodyUid = UUID.randomUUID().toString();
	protected String mboxUid = UUID.randomUUID().toString();
	protected String userUid = UUID.randomUUID().toString();
	protected String folderUid = mboxUid;
	public String ALIAS = "mailspool_alias_" + userUid;
	protected String INDEX_NAME = "mailspool_1";

	@Before
	public void before() throws Exception {
		ElasticsearchTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		System.out.println(esServer.ip);
		esServer.tags = Lists.newArrayList("bm/es");

		PopulateHelper.initGlobalVirt(esServer);
		// PopulateHelper.createTestDomain(domainUid, esServer);

		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();

		System.out.println("Ensuring index exists....");
		Client c = ESearchActivator.getClient();

		c.admin().indices().prepareAliases().addAlias(INDEX_NAME, ALIAS, QueryBuilders.termQuery("owner", userUid))
				.execute().actionGet();
		ESearchActivator.deleteByQuery(INDEX_NAME, QueryBuilders.queryStringQuery(("in:" + folderUid)));
		System.out.println("Bootstrap finished....");
	}

	protected void addEml(long imapUid, String path, SystemFlag... flags) throws IOException {
		byte[] eml = Files.toByteArray(new File(path));
		HashCode hash = Hashing.goodFastHash(128).hashBytes(eml);
		String emlUid = hash.toString();
		storeBody(emlUid, eml);
		storeMessage(mboxUid, userUid, emlUid, imapUid, Arrays.asList(flags));
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
		MailboxRecord mail = new MailboxRecord();
		mail.messageBody = bodyUid;
		mail.imapUid = imapUid;
		mail.flags = flags;

		ItemValue<MailboxRecord> item = new ItemValue<>();
		item.internalId = 44L;
		item.value = mail;
		MailIndexActivator.getService().storeMessage(mailboxUniqueId, item, userUid);
	}

	protected String entryId(long imapUid) {
		return folderUid + ":" + imapUid;
	}

}
