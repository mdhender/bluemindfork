/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.backend.mail.replica.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.join.query.JoinQueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.ByteStreams;

import io.vertx.core.json.JsonObject;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.indexing.IMailIndexService;
import net.bluemind.backend.mail.replica.indexing.RecordIndexActivator;
import net.bluemind.backend.mail.replica.service.tests.ReplicationEventsRecorder.Hierarchy;
import net.bluemind.backend.mail.replica.service.tests.compat.ExpectCommand;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPException;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class ReplicationIndexingTests extends AbstractRollingReplicationTests {

	private ExpectCommand expectCommand;

	@Before
	public void before() throws Exception {
		super.before();
		RecordIndexActivator.reload();
		Optional<IMailIndexService> indexer = RecordIndexActivator.getIndexer();
		assertTrue("Indexing support is missing", indexer.isPresent());

		long delay = System.currentTimeMillis();
		Hierarchy hierarchy = null;
		do {
			Thread.sleep(200);
			hierarchy = rec.hierarchy(domainUid, userUid);
			System.out.println("Hierarchy version is " + hierarchy.exactVersion);
			if (System.currentTimeMillis() - delay > 10000) {
				throw new TimeoutException("Hierarchy init took more than 10sec");
			}
		} while (hierarchy.exactVersion < 6);
		System.out.println("Hierarchy is now at version " + hierarchy.exactVersion);
		System.err.println("before is complete, starting test.");
		this.expectCommand = new ExpectCommand();
	}

	@After
	public void after() throws Exception {
		System.err.println("Test is over, after starts...");
		super.after();
	}

	private IServiceProvider suProvider() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	}

	static final int MAIL_COUNT = 500;
	static final int UPDATE_FLAGS_LOOPS = 20;

	@Test
	public void applyMailboxSpeed() throws IMAPException, InterruptedException, ExecutionException, TimeoutException {
		IUser userApi = suProvider().instance(IUser.class, domainUid);
		ItemValue<User> theUser = userApi.getComplete(userUid);
		assertNotNull(theUser);

		IServiceProvider prov = provider();

		IMailboxFolders userMboxesApi = prov.instance(IMailboxFolders.class, partition, mboxRoot);
		ItemValue<MailboxFolder> inboxFolder = userMboxesApi.byName("INBOX");
		assertNotNull(inboxFolder);

		FlagsList seen = new FlagsList();
		seen.add(Flag.SEEN);

		List<Integer> freshUids = new ArrayList<>();
		int lastUid = 0;
		CompletableFuture<Void> msgCompletion = expectCommand.onNextApplyMessage();
		byte[] eml = ("From: toto@bm.lan\r\nSubject: ma bite" + UUID.randomUUID().toString() + "\r\n")
				.getBytes(StandardCharsets.US_ASCII);
		for (int i = 0; i < MAIL_COUNT; i++) {
			lastUid = imapAsUser(sc -> {
				return sc.append("INBOX", new ByteArrayInputStream(eml), seen);
			});
			assertTrue(lastUid > 0);
			freshUids.add(lastUid);
		}
		msgCompletion.get(10, TimeUnit.SECONDS);

		Client client = ESearchActivator.getClient();
		int attempt = 0;
		SearchResponse found = null;

		BoolQueryBuilder freshMailQuery = QueryBuilders.boolQuery()//
				.must(QueryBuilders.termQuery("uid", lastUid));

		do {
			Thread.sleep(100);
			found = client.prepareSearch("mailspool_alias_" + userUid).setQuery(freshMailQuery).execute().actionGet();
		} while (found.getHits().getTotalHits().value == 0 && ++attempt < 400);
		assertTrue("We tried " + attempt + " times & didn't found the doc with uid " + lastUid,
				found.getHits().getTotalHits().value > 0);

		System.err.println("Flags change starts");
		long time = System.currentTimeMillis();
		FlagsList flagChange = new FlagsList();
		flagChange.add(Flag.SEEN);
		flagChange.add(Flag.FLAGGED);
		for (int i = 0; i < UPDATE_FLAGS_LOOPS; i++) {
			CompletableFuture<Void> applyMailboxCompletetion = expectCommand.onNextApplyMailbox(inboxFolder.uid);
			final boolean set = (i % 2) == 0;
			final int iteration = i + 1;
			boolean trashedOk = imapAsUser(sc -> {
				sc.select("INBOX");
				System.err.println("Submit flag CMD " + iteration);
				boolean ret = sc.uidStore(freshUids, flagChange, set);
				sc.select("Trash");
				return ret;
			});
			assertTrue(trashedOk);
			System.err.println("Waiting for apply mailbox completion...");
			applyMailboxCompletetion.get(1, TimeUnit.MINUTES);
		}
		time = System.currentTimeMillis() - time;
		System.err.print(UPDATE_FLAGS_LOOPS + " loop(s) ends after " + time + "ms.");
	}

	@Test
	public void appendMailDeleteRecordExpunge()
			throws IMAPException, InterruptedException, ExecutionException, TimeoutException {
		IUser userApi = suProvider().instance(IUser.class, domainUid);
		ItemValue<User> theUser = userApi.getComplete(userUid);
		assertNotNull(theUser);

		IServiceProvider prov = provider();

		IMailboxFolders userMboxesApi = prov.instance(IMailboxFolders.class, partition, mboxRoot);
		ItemValue<MailboxFolder> inboxFolder = userMboxesApi.byName("INBOX");
		assertNotNull(inboxFolder);

		byte[] eml = "From: toto@bm.lan\r\nSubject: ma bite\r\n".getBytes(StandardCharsets.US_ASCII);

		FlagsList seen = new FlagsList();
		seen.add(Flag.SEEN);
		int addedUid = imapAsUser(sc -> {
			return sc.append("INBOX", new ByteArrayInputStream(eml), seen);
		});
		assertTrue(addedUid > 0);

		Client client = ESearchActivator.getClient();
		int attempt = 0;
		SearchResponse found = null;

		BoolQueryBuilder freshMailQuery = QueryBuilders.boolQuery()//
				.must(QueryBuilders.termQuery("uid", addedUid));

		do {
			Thread.sleep(50);
			found = client.prepareSearch("mailspool_alias_" + userUid).setQuery(freshMailQuery).execute().actionGet();
		} while (found.getHits().getTotalHits().value == 0 && ++attempt < 200);
		assertTrue("We tried " + attempt + " times & didn't found the doc with uid " + addedUid,
				found.getHits().getTotalHits().value > 0);

		FlagsList trashed = new FlagsList();
		trashed.add(Flag.SEEN);
		trashed.add(Flag.DELETED);
		boolean trashedOk = imapAsUser(sc -> {
			sc.select("INBOX");
			return sc.uidStore(Arrays.asList(addedUid), trashed, true);
		});
		assertTrue(trashedOk);

		System.err.println("Expunge process starts.....");

		imapAsUser(sc -> {
			sc.select("INBOX");
			sc.expunge();
			return null;
		});
		Thread.sleep(1000);
		System.err.println("TEST ENDS");
	}

	@Test
	public void moveEmail() throws InterruptedException {
		Client client = ESearchActivator.getClient();
		IUser userApi = suProvider().instance(IUser.class, domainUid);
		ItemValue<User> theUser = userApi.getComplete(userUid);
		assertNotNull(theUser);

		String index = getUserAliasIndex("mailspool_alias_" + userUid, client);

		IServiceProvider prov = provider();

		IMailboxFolders userMboxesApi = prov.instance(IMailboxFolders.class, partition, mboxRoot);
		ItemValue<MailboxFolder> inboxFolder = userMboxesApi.byName("INBOX");
		assertNotNull(inboxFolder);

		byte[] eml = "From: toto@bm.lan\r\nSubject: ma bite\r\n".getBytes(StandardCharsets.US_ASCII);

		FlagsList seen = new FlagsList();
		seen.add(Flag.SEEN);
		int addedUid = imapAsUser(sc -> {
			return sc.append("INBOX", new ByteArrayInputStream(eml), seen);
		});
		assertTrue(addedUid > 0);

		int attempt = 0;
		SearchResponse found = null;

		BoolQueryBuilder freshMailQuery = QueryBuilders.boolQuery()//
				.must(QueryBuilders.termQuery("in", inboxFolder.uid))//
				.must(QueryBuilders.termQuery("uid", addedUid));

		do {
			Thread.sleep(50);
			found = client.prepareSearch(index).setQuery(freshMailQuery).execute().actionGet();
		} while (found.getHits().getTotalHits().value == 0 && ++attempt < 200);
		assertTrue("We tried " + attempt + " times & didn't find the doc with uid " + addedUid,
				found.getHits().getTotalHits().value > 0);

		ESearchActivator.refreshIndex(index);

		SearchResponse all = client.prepareSearch(index).setFetchSource(true).execute().actionGet();
		assertEquals(2, all.getHits().getTotalHits().value);

		BoolQueryBuilder orphans = QueryBuilders.boolQuery()
				.mustNot(JoinQueryBuilders.hasChildQuery("record", QueryBuilders.matchAllQuery(), ScoreMode.None)) //
				.must(QueryBuilders.termQuery("body_msg_link", "body"));

		SearchResponse orphanFound = client.prepareSearch(index)//
				.setQuery(orphans).setFetchSource(true)//
				.setTypes("recordOrBody").setFrom(0).setSize(40)//
				.execute().actionGet();

		assertEquals(0, orphanFound.getHits().getTotalHits().value);

		found.getHits().forEach(hit -> {
			System.err.println(" *** DELETE " + hit.getId());
			client.prepareDelete(index, "recordOrBody", hit.getId()).setRefreshPolicy(RefreshPolicy.IMMEDIATE).execute()
					.actionGet();
		});

		Thread.sleep(2000);

		orphanFound = client.prepareSearch(index)//
				.setQuery(orphans).setFetchSource(true)//
				.setTypes("recordOrBody").setFrom(0).setSize(40)//
				.execute().actionGet();

		assertEquals(1, orphanFound.getHits().getTotalHits().value);

		FlagsList trashed = new FlagsList();
		trashed.add(Flag.SEEN);
		trashed.add(Flag.DELETED);
		boolean trashedOk = imapAsUser(sc -> {
			sc.select("INBOX");
			return sc.uidStore(Arrays.asList(addedUid), trashed, true);
		});
		assertTrue(trashedOk);

		System.err.println("Expunge process starts.....");

		imapAsUser(sc -> {
			sc.select("INBOX");
			sc.expunge();
			return null;
		});
		Thread.sleep(1000);
		System.err.println("TEST ENDS");

		orphanFound = client.prepareSearch(index)//
				.setQuery(orphans).setFetchSource(true)//
				.setTypes("recordOrBody").setFrom(0).setSize(40)//
				.execute().actionGet();

		assertEquals(0, orphanFound.getHits().getTotalHits().value);

	}

	@Test
	public void testIndexAttachment() throws Exception {
		Client client = ESearchActivator.getClient();
		IUser userApi = suProvider().instance(IUser.class, domainUid);
		ItemValue<User> theUser = userApi.getComplete(userUid);
		assertNotNull(theUser);

		String index = getUserAliasIndex("mailspool_alias_" + userUid, client);

		IServiceProvider prov = provider();

		IMailboxFolders userMboxesApi = prov.instance(IMailboxFolders.class, partition, mboxRoot);
		ItemValue<MailboxFolder> inboxFolder = userMboxesApi.byName("INBOX");
		assertNotNull(inboxFolder);

		InputStream inputStream = AbstractReplicatedMailboxesServiceTests.class.getClassLoader()
				.getResourceAsStream("data/with_pdf.eml");
		byte[] eml = ByteStreams.toByteArray(inputStream);

		FlagsList seen = new FlagsList();
		seen.add(Flag.SEEN);
		int addedUid = imapAsUser(sc -> {
			return sc.append("INBOX", new ByteArrayInputStream(eml), seen);
		});
		assertTrue(addedUid > 0);

		int attempt = 0;
		SearchResponse found = null;

		BoolQueryBuilder freshMailQuery = QueryBuilders.boolQuery()//
				.must(QueryBuilders.termQuery("in", inboxFolder.uid))//
				.must(QueryBuilders.termQuery("uid", addedUid));

		do {
			Thread.sleep(50);
			found = client.prepareSearch(index).setQuery(freshMailQuery).execute().actionGet();
		} while (found.getHits().getTotalHits().value == 0 && ++attempt < 200);
		assertTrue("We tried " + attempt + " times & didn't find the doc with uid " + addedUid,
				found.getHits().getTotalHits().value > 0);

		SearchHit hit = found.getHits().getAt(0);
		List<String> isValue = (List<String>) hit.getSourceAsMap().get("is");
		assertTrue(new HashSet<>(isValue).contains("seen"));

		JsonObject source = new JsonObject(found.getHits().getAt(0).getSourceAsString());
		String parentId = source.getString("parentId");

		BoolQueryBuilder parentQuery = QueryBuilders.boolQuery()//
				.must(QueryBuilders.idsQuery().addIds(parentId));

		assertTrue(client.prepareSearch(index).setQuery(parentQuery).execute().actionGet().getHits().getAt(0)
				.getSourceAsString().contains("This is analyzed text"));
	}

	private String getUserAliasIndex(String alias, Client client) {
		GetAliasesResponse t = client.admin().indices().prepareGetAliases(alias).execute().actionGet();

		return t.getAliases().keysIt().next();
	}
}
