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
import java.io.IOException;
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Streams;
import com.google.common.io.ByteStreams;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.GetAliasResponse;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.indexing.IMailIndexService;
import net.bluemind.backend.mail.replica.indexing.RecordIndexActivator;
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
	public void applyMailboxSpeed() throws IMAPException, InterruptedException, ExecutionException, TimeoutException,
			ElasticsearchException, IOException {
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

		ElasticsearchClient client = ESearchActivator.getClient();
		int attempt = 0;
		int finalLastUid = lastUid;
		SearchResponse<Void> found = null;
		do {
			Thread.sleep(100);
			found = client.search(s -> s //
					.index("mailspool_alias_" + userUid) //
					.query(q -> q.bool(b -> b.must(m -> m.term(t -> t.field("uid").value(finalLastUid))))), Void.class);
		} while (found.hits().total().value() == 0 && ++attempt < 400);
		assertTrue("We tried " + attempt + " times & didn't found the doc with uid " + lastUid,
				found.hits().total().value() > 0);

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
	public void appendMailDeleteRecordExpunge() throws IMAPException, InterruptedException, ExecutionException,
			TimeoutException, ElasticsearchException, IOException {
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

		ElasticsearchClient client = ESearchActivator.getClient();
		int attempt = 0;
		SearchResponse<Void> found = null;
		do {
			Thread.sleep(50);
			found = client.search(s -> s //
					.index("mailspool_alias_" + userUid) //
					.query(q -> q.bool(b -> b.must(m -> m.term(t -> t.field("uid").value(addedUid))))), Void.class);
		} while (found.hits().total().value() == 0 && ++attempt < 200);
		assertTrue("We tried " + attempt + " times & didn't found the doc with uid " + addedUid,
				found.hits().total().value() > 0);

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
	public void moveEmail() throws InterruptedException, ElasticsearchException, IOException {
		ElasticsearchClient client = ESearchActivator.getClient();
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
		SearchResponse<Void> found = null;
		do {
			Thread.sleep(50);
			found = client.search(s -> s //
					.index(index) //
					.query(q -> q.bool(b -> b //
							.must(m -> m.term(t -> t.field("in").value(inboxFolder.uid))) //
							.must(m -> m.term(t -> t.field("uid").value(addedUid))))),
					Void.class);
		} while (found.hits().total().value() == 0 && ++attempt < 200);
		assertTrue("We tried " + attempt + " times & didn't find the doc with uid " + addedUid,
				found.hits().total().value() > 0);

		ESearchActivator.refreshIndex(index);

		SearchResponse<Void> all = client.search(s -> s.index(index).source(so -> so.fetch(true)), Void.class);
		assertEquals(2, all.hits().total().value());

		Query orphans = new BoolQuery.Builder()
				.mustNot(m -> m
						.hasChild(c -> c.type("record").query(f -> f.matchAll(a -> a)).scoreMode(ChildScoreMode.None))) //
				.must(m -> m.term(t -> t.field("body_msg_link").value("body"))).build()._toQuery();

		SearchResponse<Void> orphanFound = client.search(s -> s //
				.index(index) //
				.query(orphans) //
				.source(so -> so.fetch(true)) //
				.from(0).size(40), Void.class);

		assertEquals(0, orphanFound.hits().total().value());

		found.hits().hits().forEach(hit -> {
			System.err.println(" *** DELETE " + hit.id());
			try {
				client.delete(d -> d.index(index).id(hit.id()).refresh(Refresh.True));
			} catch (ElasticsearchException | IOException e) {
				e.printStackTrace();
			}
		});

		Thread.sleep(2000);

		orphanFound = client.search(s -> s //
				.index(index).query(orphans) //
				.source(so -> so.fetch(true)) //
				.from(0).size(40), Void.class);

		assertEquals(1, orphanFound.hits().total().value());

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

		orphanFound = client.search(s -> s //
				.index(index).query(orphans) //
				.source(so -> so.fetch(true)) //
				.from(0).size(40), Void.class);

		assertEquals(0, orphanFound.hits().total().value());

	}

	@Test
	public void testIndexAttachment() throws Exception {
		ElasticsearchClient client = ESearchActivator.getClient();
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
		SearchResponse<ObjectNode> found = null;
		do {
			Thread.sleep(50);
			found = client.search(s -> s //
					.index(index) //
					.query(q -> q.bool(b -> b //
							.must(m -> m.term(t -> t.field("in").value(inboxFolder.uid))) //
							.must(m -> m.term(t -> t.field("uid").value(addedUid))))),
					ObjectNode.class);
		} while (found.hits().total().value() == 0 && ++attempt < 200);
		assertTrue("We tried " + attempt + " times & didn't find the doc with uid " + addedUid,
				found.hits().total().value() > 0);

		Hit<ObjectNode> hit = found.hits().hits().get(0);
		List<String> isValue = Streams.stream(hit.source().get("is").elements()).map(n -> n.asText()).toList();
		assertTrue(new HashSet<>(isValue).contains("seen"));

		String parentId = hit.source().get("parentId").asText();
		SearchResponse<ObjectNode> parentResponse = client.search(s -> s //
				.index(index) //
				.query(q -> q.bool(b -> b.must(m -> m.ids(i -> i.values(parentId))))), ObjectNode.class);
		String source = new ObjectMapper().writeValueAsString(parentResponse.hits().hits().get(0).source());
		assertTrue(source.contains("This is analyzed text"));
	}

	private String getUserAliasIndex(String alias, ElasticsearchClient client)
			throws ElasticsearchException, IOException {
		GetAliasResponse t = client.indices().getAlias(a -> a.name(alias));
		return t.result().keySet().iterator().next();
	}
}
