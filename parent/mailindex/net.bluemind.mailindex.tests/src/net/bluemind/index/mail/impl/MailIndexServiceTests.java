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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.james.mime4j.MimeIOException;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Streams;
import com.google.common.io.Files;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.indices.get_alias.IndexAliases;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxFolderSearchQuery;
import net.bluemind.backend.mail.api.MessageSearchResult;
import net.bluemind.backend.mail.api.SearchQuery;
import net.bluemind.backend.mail.api.SearchQuery.FolderScope;
import net.bluemind.backend.mail.api.SearchQuery.Header;
import net.bluemind.backend.mail.api.SearchQuery.HeaderQuery;
import net.bluemind.backend.mail.api.SearchQuery.LogicalOperator;
import net.bluemind.backend.mail.api.SearchQuery.SearchScope;
import net.bluemind.backend.mail.api.SearchResult;
import net.bluemind.backend.mail.api.SearchSort;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.api.utils.MailIndexQuery;
import net.bluemind.backend.mail.replica.indexing.IDSet;
import net.bluemind.backend.mail.replica.indexing.IMailIndexService.BulkOp;
import net.bluemind.backend.mail.replica.indexing.MailSummary;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.index.MailIndexActivator;
import net.bluemind.index.mail.MailIndexService;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.elasticsearch.IndexAliasMapping;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.ShardStats;
import net.bluemind.utils.ByteSizeUnit;

public abstract class MailIndexServiceTests extends AbstractSearchTests {

	@Test
	public void testSort() throws Exception {
		byte[] eml = Files.toByteArray(new File("data/test.eml"));

		storeBody("body1", eml);
		storeMessage("inbox", userUid, "body1", 1, Collections.emptyList(), 1l);

		String data = new String(eml);
		data = data.replace("12 Feb", "13 Feb");

		storeBody("body2", data.getBytes());
		storeMessage("inbox", userUid, "body2", 2, Collections.emptyList(), 2l);

		refreshAllIndices();

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.recordQuery = null;
		query.query = "drug";
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;

		MailIndexQuery q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		SearchResult sr = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertEquals(2, sr.totalResults);

		// no sort criteria, default is by date DESC
		assertTrue(sr.results.get(0).date.after(sr.results.get(1).date));

		// by date ASC
		q.sort = SearchSort.byField("date", SearchSort.Order.Asc);
		sr = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertEquals(2, sr.totalResults);
		assertTrue(sr.results.get(0).date.before(sr.results.get(1).date));

		// by date DESC
		q.sort = SearchSort.byField("date", SearchSort.Order.Desc);
		sr = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertEquals(2, sr.totalResults);
		assertTrue(sr.results.get(0).date.after(sr.results.get(1).date));
	}

	@Test
	public void testDeDuplicateSearch() throws Exception {
		byte[] eml = Files.toByteArray(new File("data/test.eml"));

		storeBody("body1", eml);
		storeMessage("inbox", userUid, "body1", 1, Collections.emptyList(), 111l);

		String data = new String(eml);
		data = data.replace("12 Feb", "13 Feb");

		storeBody("body2", data.getBytes());
		storeMessage("inbox", userUid, "body2", 2, Collections.emptyList(), 111l);

		refreshAllIndices();

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.recordQuery = null;
		query.query = "drug";
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;

		MailIndexQuery q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		SearchResult sr = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertEquals(1, sr.totalResults);
	}

	@Test
	public void testFetchSummary() throws Exception {
		byte[] eml = Files.toByteArray(new File("data/test.eml"));

		storeBody("body1", eml);
		storeMessage("inbox", userUid, "body1", 1, Collections.emptyList(), 1);
		storeBody("body2", eml);
		storeMessage("sent", userUid, "body2", 2, Collections.emptyList(), 2);
		storeBody("body3", eml);
		storeMessage("sent", userUid, "body3", 3, Collections.emptyList(), 3);
		storeBody("body4", eml);
		storeMessage("sent", userUid, "body4", 4, Collections.emptyList(), 4);

		refreshAllIndices();

		ItemValue<Mailbox> mailbox = ItemValue.create(userUid, new Mailbox());
		ItemValue<MailboxFolder> mailboxFolder = ItemValue.create("sent", new MailboxFolder());
		IDSet set = IDSet.create(Arrays.asList(2, 3));
		List<MailSummary> summaries = MailIndexActivator.getService().fetchSummary(mailbox, mailboxFolder, set);

		assertEquals(2, summaries.size());
		Set<String> parentIds = summaries.stream().map(s -> s.parentId).collect(Collectors.toSet());
		assertTrue(parentIds.contains("body2"));
		assertTrue(parentIds.contains("body3"));
		Set<Integer> uids = summaries.stream().map(s -> s.uid).collect(Collectors.toSet());
		assertTrue(uids.contains(2));
		assertTrue(uids.contains(3));
	}

	@Test
	public void testSortMultiFolder() throws Exception {
		byte[] eml = Files.toByteArray(new File("data/test.eml"));

		storeBody("body1", eml);
		storeMessage("inbox", userUid, "body1", 1, Collections.emptyList(), 1l);

		String data = new String(eml);
		data = data.replace("12 Feb", "13 Feb");

		storeBody("body2", data.getBytes());
		storeMessage("sent", userUid, "body2", 2, Collections.emptyList(), 2l);

		data = data.replace("13 Feb", "14 Feb");

		storeBody("body3", data.getBytes());
		storeMessage("sent", userUid, "body3", 3, Collections.emptyList(), 3l);

		data = data.replace("14 Feb", "1 Feb");

		storeBody("body4", data.getBytes());
		storeMessage("toto", userUid, "body4", 4, Collections.emptyList(), 4l);

		refreshAllIndices();

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.recordQuery = null;
		query.query = "drug";
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;

		MailIndexQuery q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		SearchResult sr = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertEquals(4, sr.totalResults);

		// no sort criteria, default is by date DESC
		assertTrue(sr.results.get(0).date.after(sr.results.get(1).date));
		assertTrue(sr.results.get(1).date.after(sr.results.get(2).date));
		assertTrue(sr.results.get(2).date.after(sr.results.get(3).date));

		// by date ASC
		q.sort = SearchSort.byField("date", SearchSort.Order.Asc);
		sr = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertEquals(4, sr.totalResults);
		assertTrue(sr.results.get(0).date.before(sr.results.get(1).date));
		assertTrue(sr.results.get(1).date.before(sr.results.get(2).date));
		assertTrue(sr.results.get(2).date.before(sr.results.get(3).date));

		// by date DESC
		q.sort = SearchSort.byField("date", SearchSort.Order.Desc);
		sr = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertEquals(4, sr.totalResults);
		assertTrue(sr.results.get(0).date.after(sr.results.get(1).date));
		assertTrue(sr.results.get(1).date.after(sr.results.get(2).date));
		assertTrue(sr.results.get(2).date.after(sr.results.get(3).date));
	}

	@Test
	public void testSimpleSearch() throws MimeIOException, IOException, InterruptedException, ExecutionException {
		ElasticsearchClient c = ESearchActivator.getClient();
		long imapUid = 1;

		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, imapUid, Collections.emptyList());
		refreshAllIndices();

		SearchResponse<ObjectNode> resp = c.search(s -> s //
				.index(IndexAliasMapping.get().getReadAliasByMailboxUid(userUid)) //
				.query(q -> q.queryString(qs -> qs.query("id:\"" + entryId(44) + "\""))), ObjectNode.class);
		assertEquals(1L, resp.hits().total().value());

		ObjectNode source = resp.hits().hits().get(0).source();
		assertEquals(bodyUid, source.get("parentId").asText());
		assertEquals("SubjectTest", source.get("subject").asText());
		assertEquals(entryId(44), source.get("id").asText());

		// search by IN in alias
		resp = c.search(s -> s //
				.index(IndexAliasMapping.get().getReadAliasByMailboxUid(userUid)) //
				.query(q -> q.bool(b -> b.must(m -> m.term(t -> t.field("in").value(folderUid))))), ObjectNode.class);
		assertEquals(1L, resp.hits().total().value());

		source = resp.hits().hits().get(0).source();
		assertEquals(bodyUid, source.get("parentId").asText());
		assertEquals("SubjectTest", source.get("subject").asText());
		assertEquals(entryId(44), source.get("id").asText());
	}

	@Test
	public void testDelete() throws MimeIOException, IOException, InterruptedException, ExecutionException {
		ElasticsearchClient c = ESearchActivator.getClient();
		int imapUid = 1;

		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, imapUid, Collections.emptyList());
		refreshAllIndices();

		SearchResponse<ObjectNode> resp = c.search(s -> s //
				.index(IndexAliasMapping.get().getReadAliasByMailboxUid(userUid)) //
				.query(q -> q.queryString(qs -> qs.query("id:\"" + entryId(44) + "\""))), ObjectNode.class);
		assertEquals(1L, resp.hits().total().value());

		List<MailboxItemFlag> deleteFlag = Arrays.asList(MailboxItemFlag.System.Deleted.value());
		storeMessage(mboxUid, userUid, bodyUid, imapUid, deleteFlag);
		refreshAllIndices();
		IDSet set = IDSet.create(Arrays.asList(imapUid));
		MailIndexActivator.getService().expunge(ItemValue.create(userUid, null), ItemValue.create(folderUid, null),
				set);
		refreshAllIndices();

		resp = c.search(s -> s //
				.index(IndexAliasMapping.get().getReadAliasByMailboxUid(userUid)) //
				.query(q -> q.queryString(qs -> qs.query("id:\"" + entryId(44) + "\""))), ObjectNode.class);
		assertEquals(0L, resp.hits().total().value());
	}

	@Test
	public void testDeleteBox() throws Exception {
		MailIndexService service = (MailIndexService) MailIndexActivator.getService();
		String userAlias = IndexAliasMapping.get().getWriteAliasByMailboxUid(userUid);
		byte[] eml = Files.toByteArray(new File("data/test.eml"));

		SearchResponse<ObjectNode> resp = ESearchActivator.getClient().search(s -> s //
				.index(userAlias).query(q -> q.term(new TermQuery.Builder().field("owner").value(userUid).build())),
				ObjectNode.class);
		long initialValue = resp.hits().total().value();

		storeBody("body1", eml);
		storeMessage("inbox", userUid, "body1", 0, Collections.emptyList(), 0);
		List<BulkOp> bulkOperations = new ArrayList<>();
		for (int i = 0; i < 2000; i++) {
			bulkOperations.addAll(bulkMessage("sent", userUid, "body1", i + 1, Collections.emptyList(), i + 1));
		}
		service.doBulk(bulkOperations);

		refreshAllIndices();
		resp = ESearchActivator.getClient().search(s -> s //
				.index(userAlias).query(q -> q.term(new TermQuery.Builder().field("owner").value(userUid).build())),
				ObjectNode.class);
		assertEquals(initialValue + 2001, resp.hits().total().value());

		ItemValue<Mailbox> mailbox = ItemValue.create(userUid, new Mailbox());
		ItemValue<MailboxFolder> sentFolder = ItemValue.create("sent", new MailboxFolder());
		ItemValue<MailboxFolder> inboxFolder = ItemValue.create("inbox", new MailboxFolder());
		IDSet emptySet = IDSet.create(new ArrayList<>());

		refreshAllIndices();

		MailIndexActivator.getService().deleteBox(mailbox, "sent");

		refreshAllIndices();

		List<MailSummary> sentSummaries = MailIndexActivator.getService().fetchSummary(mailbox, sentFolder, emptySet);
		assertEquals(0, sentSummaries.size());
		List<MailSummary> inboxSummaries = MailIndexActivator.getService().fetchSummary(mailbox, inboxFolder, emptySet);
		assertEquals(1, inboxSummaries.size());
	}

	@Test
	public void testDeleteMailbox() throws Exception {
		MailIndexService service = (MailIndexService) MailIndexActivator.getService();
		String userAlias = IndexAliasMapping.get().getWriteAliasByMailboxUid(userUid);
		byte[] eml = Files.toByteArray(new File("data/test.eml"));

		SearchResponse<ObjectNode> resp = ESearchActivator.getClient().search(s -> s //
				.index(userAlias).query(q -> q.matchAll(a -> a)), ObjectNode.class);
		long initialValue = resp.hits().total().value();

		storeBody("body1", eml);
		int countPerFolder = 1000;
		List<BulkOp> bulkOperations = new ArrayList<>();
		for (int i = 0; i < countPerFolder; i++) {
			bulkOperations.addAll(bulkMessage("inbox", userUid, "body1", i, Collections.emptyList(), i));
		}
		for (int i = 0; i < countPerFolder; i++) {
			bulkOperations
					.addAll(bulkMessage("sent", userUid, "body1", i + countPerFolder, Collections.emptyList(), i));
		}
		service.doBulk(bulkOperations);

		refreshAllIndices();
		resp = ESearchActivator.getClient().search(s -> s //
				.index(userAlias).query(q -> q.term(new TermQuery.Builder().field("owner").value(userUid).build())),
				ObjectNode.class);
		assertEquals(initialValue + 2000, resp.hits().total().value());

		ItemValue<Mailbox> mailbox = ItemValue.create(userUid, new Mailbox());
		ItemValue<MailboxFolder> sentFolder = ItemValue.create("sent", new MailboxFolder());
		ItemValue<MailboxFolder> inboxFolder = ItemValue.create("inbox", new MailboxFolder());
		IDSet emptySet = IDSet.create(new ArrayList<>());

		MailIndexActivator.getService().deleteMailbox(userUid);

		refreshAllIndices();

		assertFalse(userAliasExists(userAlias));
		List<MailSummary> sentSummaries = service.fetchSummary(mailbox, sentFolder, emptySet);
		assertEquals(0, sentSummaries.size());
		List<MailSummary> inboxSummaries = service.fetchSummary(mailbox, inboxFolder, emptySet);
		assertEquals(0, inboxSummaries.size());
	}

	private boolean userAliasExists(String userAlias) {
		ElasticsearchClient client = ESearchActivator.getClient();
		try {
			return !client.indices().getAlias(a -> a.name(userAlias)).result().isEmpty();
		} catch (ElasticsearchException | IOException e) {
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUpdateFlags() throws MimeIOException, IOException, InterruptedException, ExecutionException {
		ElasticsearchClient c = ESearchActivator.getClient();

		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, 1, Collections.emptyList(), 1);
		storeMessage(mboxUid, userUid, bodyUid, 2, Collections.emptyList(), 2);
		refreshAllIndices();

		List<MailSummary> mails = new ArrayList<>();
		MailSummary summary1 = new MailSummary();
		summary1.uid = 1;
		summary1.parentId = bodyUid;
		summary1.flags = new HashSet<>(Arrays.asList("flag1", "flag2"));
		MailSummary summary2 = new MailSummary();
		summary2.uid = 2;
		summary2.parentId = bodyUid;
		summary2.flags = new HashSet<>(Arrays.asList("flag2", "flag3"));

		mails.add(summary1);
		mails.add(summary2);

		MailIndexActivator.getService().syncFlags(ItemValue.create(userUid, null), ItemValue.create(folderUid, null),
				mails);
		refreshAllIndices();

		SearchResponse<ObjectNode> resp = c.search(s -> s //
				.index(IndexAliasMapping.get().getReadAliasByMailboxUid(userUid)) //
				.query(q -> q.queryString(qs -> qs.query("id:\"" + entryId(1) + "\""))), ObjectNode.class);
		assertEquals(1L, resp.hits().total().value());
		List<String> flags = Streams.stream(resp.hits().hits().get(0).source().get("is")).map(n -> n.asText()).toList();
		assertTrue(flags.contains("flag1"));
		assertTrue(flags.contains("flag2"));

		resp = c.search(s -> s //
				.index(IndexAliasMapping.get().getReadAliasByMailboxUid(userUid)) //
				.query(q -> q.queryString(qs -> qs.query("id:\"" + entryId(2) + "\""))), ObjectNode.class);
		assertEquals(1L, resp.hits().total().value());
		flags = Streams.stream(resp.hits().hits().get(0).source().get("is")).map(n -> n.asText()).toList();
		assertTrue(flags.contains("flag2"));
		assertTrue(flags.contains("flag3"));
	}

	@Test
	public void testFolders() throws MimeIOException, IOException {

		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		String f1 = UUID.randomUUID().toString();
		String f2 = UUID.randomUUID().toString();
		storeMessage(f1, userUid, bodyUid, 1, Collections.emptyList());
		storeMessage(f2, userUid, bodyUid, 2, Collections.emptyList());
		refreshAllIndices();

		Set<String> folders = MailIndexActivator.getService().getFolders(userUid);
		assertTrue(folders.contains(f1));
		assertTrue(folders.contains(f2));
	}

	@Test
	public void testMoveIndexToNewIndex() throws Exception {
		ElasticsearchClient c = ESearchActivator.getClient();
		MailIndexService service = (MailIndexService) MailIndexActivator.getService();

		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		List<BulkOp> bulkOperations = new ArrayList<>();
		for (int i = 0; i < 10000; i++) {
			bulkOperations.addAll(bulkMessage(mboxUid, userUid, bodyUid, i + 1, Collections.emptyList(), i + 1));
		}
		service.doBulk(bulkOperations);
		refreshAllIndices();

		service.moveMailbox(userUid, "mailspool_test2");
		refreshAllIndices();

		SearchResponse<ObjectNode> resp = c.search(s -> s //
				.index("mailspool_test2") //
				.query(q -> q.matchAll(a -> a)), ObjectNode.class);
		assertEquals(10000L, resp.hits().total().value());

		resp = c.search(s -> s //
				.index("mailspool_test2") //
				.query(q -> q.queryString(qs -> qs.query("id:\"" + entryId(44) + "\""))), ObjectNode.class);
		assertEquals(1L, resp.hits().total().value());

		resp = c.search(s -> s //
				.index(ALIAS) //
				.query(q -> q.queryString(qs -> qs.query("id:\"" + entryId(44) + "\""))), ObjectNode.class);
		assertEquals(1L, resp.hits().total().value());

		resp = c.search(s -> s //
				.index(INDEX_NAME) //
				.query(q -> q.queryString(qs -> qs.query("id:\"" + entryId(44) + "\""))), ObjectNode.class);
		assertEquals(0L, resp.hits().total().value());
	}

	@Test
	public void testGetStats() throws MimeIOException, IOException {

		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, 1, Collections.emptyList());
		storeMessage(mboxUid, userUid, bodyUid, 2, Collections.emptyList());
		refreshAllIndices();

		List<ShardStats> stats = MailIndexActivator.getService().getStats();
		assertNotNull(stats);
		MailIndexActivator.getService().moveMailbox(userUid, "mailspool_test2");
		refreshAllIndices();

		stats = MailIndexActivator.getService().getStats();
		assertNotNull(stats);
	}

	@Test
	public void testGetQuotaInKiB() throws IOException {
		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeBody(bodyUid1, eml);
		storeMessage(mboxUid, userUid, bodyUid, 1, Collections.emptyList(), 1l);
		storeMessage(mboxUid, userUid, bodyUid1, 2, Collections.emptyList(), 2l);
		storeMessage(mboxUid, userUid, bodyUid, 3, Collections.emptyList(), 3l);
		storeMessage(mboxUid, userUid, bodyUid, 4, Collections.emptyList(), 4l);
		storeMessage(mboxUid, userUid, bodyUid, 5, Collections.emptyList(), 5l);
		storeMessage(mboxUid2, userUid2, bodyUid, 6, Collections.emptyList(), 1l);
		storeMessage(mboxUid2, userUid2, bodyUid, 7, Collections.emptyList(), 2l);
		refreshAllIndices();

		long quotaUser1 = MailIndexActivator.getService().getMailboxConsumedStorage(userUid, ByteSizeUnit.KB);
		long quotaUser2 = MailIndexActivator.getService().getMailboxConsumedStorage(userUid2, ByteSizeUnit.KB);
		assertEquals(5L, quotaUser1);
		assertEquals(2L, quotaUser2);
	}

	@Test
	public void testGetQuotaInMiB() throws IOException {
		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeBody(bodyUid1, eml);
		storeMessage(mboxUid, userUid, bodyUid, 1, Collections.emptyList(), 1l);
		storeMessage(mboxUid, userUid, bodyUid1, 2, Collections.emptyList(), 2l);
		storeMessage(mboxUid, userUid, bodyUid, 3, Collections.emptyList(), 3l);
		storeMessage(mboxUid, userUid, bodyUid, 4, Collections.emptyList(), 4l);
		storeMessage(mboxUid, userUid, bodyUid, 5, Collections.emptyList(), 5l);
		storeMessage(mboxUid2, userUid2, bodyUid, 6, Collections.emptyList(), 1l);
		storeMessage(mboxUid2, userUid2, bodyUid, 7, Collections.emptyList(), 2l);
		refreshAllIndices();

		long quotaUser1 = MailIndexActivator.getService().getMailboxConsumedStorage(userUid, ByteSizeUnit.MB);
		long quotaUser2 = MailIndexActivator.getService().getMailboxConsumedStorage(userUid2, ByteSizeUnit.MB);
		assertEquals(0L, quotaUser1);
		assertEquals(0L, quotaUser2);
	}

	@Test
	public void testGetQuotaInBytes() throws IOException {
		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeBody(bodyUid1, eml);
		storeMessage(mboxUid, userUid, bodyUid, 1, Collections.emptyList(), 1l);
		storeMessage(mboxUid, userUid, bodyUid1, 2, Collections.emptyList(), 2l);
		storeMessage(mboxUid, userUid, bodyUid, 3, Collections.emptyList(), 3l);
		storeMessage(mboxUid, userUid, bodyUid, 4, Collections.emptyList(), 4l);
		storeMessage(mboxUid, userUid, bodyUid, 5, Collections.emptyList(), 5l);
		storeMessage(mboxUid2, userUid2, bodyUid, 6, Collections.emptyList(), 1l);
		storeMessage(mboxUid2, userUid2, bodyUid, 7, Collections.emptyList(), 2l);
		refreshAllIndices();

		long quotaUser1 = MailIndexActivator.getService().getMailboxConsumedStorage(userUid, ByteSizeUnit.BYTES);
		long quotaUser2 = MailIndexActivator.getService().getMailboxConsumedStorage(userUid2, ByteSizeUnit.BYTES);
		assertEquals(5835L, quotaUser1);
		assertEquals(2334L, quotaUser2);
	}

	@Test
	public void testSearch() throws MimeIOException, IOException, InterruptedException, ExecutionException {
		long imapUid = 1;
		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, imapUid, Collections.emptyList());
		refreshAllIndices();

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.query = "SubjectTest";
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;
		MailIndexQuery q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		SearchResult results = MailIndexActivator.getService().searchItems(null, userUid, q);

		assertOnlyResultIsTestEml(results);
	}

	@Test
	public void testReset() throws Exception {
		long imapUid = 1;
		byte[] eml = Files.toByteArray(new File("data/test.eml"));

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.query = "SubjectTest";
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;
		MailIndexQuery q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;

		SearchResult results1 = MailIndexActivator.getService().searchItems(null, userUid, q);
		SearchResult results2 = MailIndexActivator.getService().searchItems(null, userUid2, q);

		assertEquals(0, results1.totalResults);
		assertEquals(0, results2.totalResults);

		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, imapUid, Collections.emptyList());
		storeMessage(mboxUid2, userUid2, bodyUid, imapUid, Collections.emptyList());
		refreshAllIndices();

		results1 = MailIndexActivator.getService().searchItems(null, userUid, q);
		results2 = MailIndexActivator.getService().searchItems(null, userUid2, q);

		assertEquals(1, results1.totalResults);
		assertEquals(1, results2.totalResults);

		MailIndexActivator.getService().resetMailboxIndex(userUid2);
		refreshAllIndices();

		results1 = MailIndexActivator.getService().searchItems(null, userUid, q);
		results2 = MailIndexActivator.getService().searchItems(null, userUid2, q);

		assertEquals(1, results1.totalResults);
		assertEquals(0, results2.totalResults);

	}

	private void assertOnlyResultIsTestEml(SearchResult results) {
		assertEquals(1, results.totalResults);
		MessageSearchResult messageSearchResult = results.results.get(0);
		assertEquals("mbox_records_" + mboxUid, messageSearchResult.containerUid);
		assertEquals(44l, messageSearchResult.itemId);
		assertEquals("IPM.Note", messageSearchResult.messageClass);
	}

	@Test
	public void testSearchDefaultField() throws MimeIOException, IOException, InterruptedException, ExecutionException {
		long imapUid = 1;
		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, imapUid, Collections.emptyList());
		refreshAllIndices();

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.query = "SubjectTest"; // Subject
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;
		MailIndexQuery q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		SearchResult results = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertOnlyResultIsTestEml(results);

		query.query = "Water"; // From
		results = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertOnlyResultIsTestEml(results);

		query.query = "Barrett"; // To
		results = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertOnlyResultIsTestEml(results);

		query.query = "Lost"; // Content
		results = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertOnlyResultIsTestEml(results);
	}

	@Test
	public void testSearchDefaultOperator()
			throws MimeIOException, IOException, InterruptedException, ExecutionException {
		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage("inbox", userUid, bodyUid, 1, Collections.emptyList(), 1l);

		String data = new String(eml);
		data = data.replace("roger.water@pinkfloyd.net", "david.gilmour@pinkfloyd.net");
		data = data.replace("Roger Water", "David Gilmour");
		storeBody(bodyUid1, data.getBytes());
		storeMessage("inbox", userUid, bodyUid1, 2, Collections.emptyList(), 2l);

		List<MailSummary> mails = new ArrayList<>();
		MailSummary summary1 = new MailSummary();
		summary1.uid = 1;
		summary1.parentId = bodyUid;
		summary1.flags = new HashSet<>(Arrays.asList("yeah"));
		mails.add(summary1);

		MailSummary summary2 = new MailSummary();
		summary2.uid = 2;
		summary2.parentId = bodyUid1;
		summary2.flags = new HashSet<>(Arrays.asList("yeah", "yo"));

		mails.add(summary2);

		MailIndexActivator.getService().syncFlags(ItemValue.create(userUid, null), ItemValue.create("inbox", null),
				mails);
		refreshAllIndices();

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;
		MailIndexQuery q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;

		// Default operator default value
		query.query = "from:roger.water@pinkfloyd.net drug";
		SearchResult results = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertEquals(1, results.totalResults);
		query.query = "";
		query.recordQuery = "with:roger.water@pinkfloyd.net is:yeah";

		results = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertEquals(1, results.totalResults);

		// Default operator AND value
		query.logicalOperator = LogicalOperator.AND;
		query.recordQuery = "";
		query.query = "from:roger.water@pinkfloyd.net drug";
		results = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertEquals(1, results.totalResults);
		query.query = "";
		query.recordQuery = "with:roger.water@pinkfloyd.net is:yeah";
		results = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertEquals(1, results.totalResults);

		// Default operator AND value
		query.logicalOperator = LogicalOperator.OR;
		query.recordQuery = "";
		query.query = "from:roger from:david";
		results = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertEquals(2, results.totalResults);
		query.query = "";
		query.recordQuery = "with:roger is:yo";
		results = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertEquals(2, results.totalResults);
		// Tokenized pattern use default operator
		query.recordQuery = "";
		query.query = "from:roger.water@pinkfloyd.net";
		results = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertEquals(2, results.totalResults);

	}

	@Test
	public void testPaginatedSearch() throws MimeIOException, IOException, InterruptedException, ExecutionException {
		MailIndexService service = (MailIndexService) MailIndexActivator.getService();
		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		int batchSize = 1000;
		int totalSize = batchSize + 10;
		List<BulkOp> bulkOperations = new ArrayList<>();
		for (int i = 1; i <= totalSize; i++) {
			bulkOperations.addAll(bulkMessage(mboxUid, userUid, bodyUid, 1, Collections.emptyList(), i));
		}
		service.doBulk(bulkOperations);

		refreshAllIndices();

		SearchQuery query = new SearchQuery();
		query.maxResults = Integer.MAX_VALUE + 1l;
		query.offset = 0;
		query.query = "SubjectTest";
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;
		MailIndexQuery q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		SearchResult results = MailIndexActivator.getService().searchItems(null, userUid, q);

		assertEquals(totalSize, results.totalResults);
		assertEquals(totalSize, results.results.size());
	}

	@Test
	public void testSearchBody() throws MimeIOException, IOException, InterruptedException, ExecutionException {
		long imapUid = 1;
		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, imapUid, Collections.emptyList());
		refreshAllIndices();

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.query = "drug";
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;
		MailIndexQuery q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		SearchResult results = MailIndexActivator.getService().searchItems(null, userUid, q);

		assertOnlyResultIsTestEml(results);
	}

	@Test
	public void testSearch_InvalidQuery()
			throws MimeIOException, IOException, InterruptedException, ExecutionException {
		long imapUid = 1;
		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, imapUid, Collections.emptyList());
		refreshAllIndices();

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.query = "invalid\"";
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;
		MailIndexQuery q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		SearchResult results = MailIndexActivator.getService().searchItems(null, userUid, q);

		assertEquals(0, results.totalResults);
	}

	@Test
	public void testSearchByFlags() throws MimeIOException, IOException, InterruptedException, ExecutionException {
		long imapUid = 1;
		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, imapUid, Collections.emptyList(), 1);
		refreshAllIndices();

		List<MailSummary> mails = new ArrayList<>();
		MailSummary summary1 = new MailSummary();
		summary1.uid = 1;
		summary1.parentId = bodyUid;
		summary1.flags = new HashSet<>(Arrays.asList("unread", "yeah"));

		mails.add(summary1);

		MailIndexActivator.getService().syncFlags(ItemValue.create(userUid, null), ItemValue.create(folderUid, null),
				mails);
		refreshAllIndices();

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.recordQuery = "is:yeah";
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;

		MailIndexQuery q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		SearchResult results = MailIndexActivator.getService().searchItems(null, userUid, q);

		assertEquals(1, results.totalResults);
		MessageSearchResult messageSearchResult = results.results.get(0);
		assertEquals("mbox_records_" + mboxUid, messageSearchResult.containerUid);
		assertEquals(1l, messageSearchResult.itemId);
		assertEquals("IPM.Note", messageSearchResult.messageClass);
	}

	@Test
	public void testSearch_EsQuery() throws MimeIOException, IOException, InterruptedException, ExecutionException {
		long imapUid = 1;
		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, imapUid, Collections.emptyList(), 1);
		refreshAllIndices();

		List<MailSummary> mails = new ArrayList<>();
		MailSummary summary1 = new MailSummary();
		summary1.uid = 1;
		summary1.parentId = bodyUid;
		summary1.flags = new HashSet<>(Arrays.asList("unread", "yeah"));

		mails.add(summary1);

		MailIndexActivator.getService().syncFlags(ItemValue.create(userUid, null), ItemValue.create(folderUid, null),
				mails);
		refreshAllIndices();

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.recordQuery = null;
		query.query = "drug";
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;

		MailIndexQuery q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		SearchResult results = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertEquals(1, results.totalResults);

		query.recordQuery = "is:unread";
		query.query = null;
		q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		results = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertEquals(1, results.totalResults);

		query.recordQuery = "is:unread";
		query.query = "SubjectTest";
		q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		results = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertEquals(1, results.totalResults);

		query.recordQuery = "is:unread subject:SubjectTest";
		query.query = null;
		q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		results = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertEquals(1, results.totalResults);

		query.recordQuery = "is:unread";
		query.query = "drug";
		q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		results = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertEquals(1, results.totalResults);

		query.recordQuery = "is:yeah";
		query.query = "my drug";
		q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		results = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertEquals(1, results.totalResults);

		query.recordQuery = "is:yeah";
		query.query = "\"my drug\"";
		q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		results = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertEquals(1, results.totalResults);

		query.recordQuery = "is:wat";
		query.query = "drug";
		q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		results = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertEquals(0, results.totalResults);

		query.recordQuery = "is:unread";
		query.query = "pouet";
		q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		results = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertEquals(0, results.totalResults);

	}

	@Test
	public void testSearchByHeader() throws MimeIOException, IOException, InterruptedException, ExecutionException {
		long imapUid = 1;
		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, imapUid, Collections.emptyList());
		refreshAllIndices();

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.headerQuery = new HeaderQuery();
		query.headerQuery.logicalOperator = LogicalOperator.AND;
		Header headerQueryElement = new Header();
		headerQueryElement.name = "From";
		headerQueryElement.value = "Roger Water <roger.water@pinkfloyd.net>";
		query.headerQuery.query = Arrays.asList(headerQueryElement);
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;
		MailIndexQuery q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		SearchResult results = MailIndexActivator.getService().searchItems(null, userUid, q);

		assertEquals(1, results.totalResults);
		MessageSearchResult messageSearchResult = results.results.get(0);
		assertEquals(44l, messageSearchResult.itemId);

		query.headerQuery = new HeaderQuery();
		query.headerQuery.logicalOperator = LogicalOperator.OR;
		headerQueryElement = new Header();
		headerQueryElement.name = "From";
		headerQueryElement.value = "Roger Water <roger.water@pinkfloyd.net>";
		Header headerQueryElement2 = new Header();
		headerQueryElement2.name = "From";
		headerQueryElement2.value = "John Water <john.water@pinkfloyd.net>";
		query.headerQuery.query = Arrays.asList(headerQueryElement, headerQueryElement2);

		q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		results = MailIndexActivator.getService().searchItems(null, userUid, q);

		assertEquals(1, results.totalResults);

		query.headerQuery = new HeaderQuery();
		query.headerQuery.logicalOperator = LogicalOperator.AND;
		headerQueryElement = new Header();
		headerQueryElement.name = "From";
		headerQueryElement.value = "Roger Water <roger.water@pinkfloyd.net>";
		headerQueryElement2 = new Header();
		headerQueryElement2.name = "From";
		headerQueryElement2.value = "John Water <john.water@pinkfloyd.net>";
		query.headerQuery.query = Arrays.asList(headerQueryElement, headerQueryElement2);

		q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		results = MailIndexActivator.getService().searchItems(null, userUid, q);

		assertEquals(0, results.totalResults);
	}

	@Test
	public void testSearchByMessageId() throws MimeIOException, IOException, InterruptedException, ExecutionException {
		long imapUid = 1;
		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, imapUid, Collections.emptyList());
		refreshAllIndices();

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.messageId = "<7FA4B249-C434-4F98-A447-A0F0C8D42A4E@pinkfloy.net>";
		MailIndexQuery q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;

		SearchResult results = MailIndexActivator.getService().searchItems(null, userUid, q);

		assertEquals(1, results.totalResults);
		MessageSearchResult messageSearchResult = results.results.get(0);
		assertEquals(44l, messageSearchResult.itemId);

		query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.messageId = "idontexist";
		q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;
		results = MailIndexActivator.getService().searchItems(null, userUid, q);

		assertEquals(0, results.totalResults);
	}

	@Test
	public void testRecursiveSearch() throws MimeIOException, IOException, InterruptedException, ExecutionException {
		long imapUid = 1;
		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, imapUid, Collections.emptyList(), 44l);
		storeMessage(mboxUid2, userUid, bodyUid, imapUid, Collections.emptyList(), 45l);
		refreshAllIndices();

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.scope = new SearchScope();
		query.scope.folderScope = new FolderScope();
		query.scope.folderScope.folderUid = mboxUid;
		query.scope.isDeepTraversal = false;
		query.query = "";
		MailIndexQuery q = MailIndexQuery.folderQuery(new MailboxFolderSearchQuery(), Arrays.asList());
		q.query = query;

		SearchResult results = MailIndexActivator.getService().searchItems(null, userUid, q);

		assertEquals(1, results.totalResults);
		MessageSearchResult messageSearchResult = results.results.get(0);
		assertEquals(44l, messageSearchResult.itemId);

		query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.scope = new SearchScope();
		query.scope.folderScope = new FolderScope();
		query.scope.folderScope.folderUid = mboxUid;
		query.scope.isDeepTraversal = true;
		query.query = "";
		q = MailIndexQuery.folderQuery(new MailboxFolderSearchQuery(), Arrays.asList(mboxUid2));
		q.query = query;

		results = MailIndexActivator.getService().searchItems(null, userUid, q);

		assertEquals(2, results.totalResults);
		messageSearchResult = results.results.get(0);
		assertEquals(44l, messageSearchResult.itemId);
		messageSearchResult = results.results.get(1);
		assertEquals(45l, messageSearchResult.itemId);

	}

	@Test
	public void testSearchByReferences() throws MimeIOException, IOException, InterruptedException, ExecutionException {
		long imapUid = 1;
		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, imapUid, Collections.emptyList());
		refreshAllIndices();

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.references = "<19980507220459.5655.qmail@warren.demon.co.uk>";
		MailIndexQuery q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;

		SearchResult results = MailIndexActivator.getService().searchItems(null, userUid, q);

		assertEquals(1, results.totalResults);
		MessageSearchResult messageSearchResult = results.results.get(0);
		assertEquals(44l, messageSearchResult.itemId);

		query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.references = "idontexist";
		q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;
		results = MailIndexActivator.getService().searchItems(null, userUid, q);

		assertEquals(0, results.totalResults);
	}

	@Test
	public void testSearchCheckCyrillicRecipient() throws IOException {
		long imapUid = 1;
		byte[] eml = Files.toByteArray(new File("data/testRecipient.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, imapUid, Collections.emptyList());
		refreshAllIndices();

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.headerQuery = new HeaderQuery();
		query.headerQuery.logicalOperator = LogicalOperator.AND;
		Header headerQueryElement = new Header();
		headerQueryElement.name = "From";
		headerQueryElement.value = "Антон Плескановский <osef@gmail.com>";
		query.headerQuery.query = Arrays.asList(headerQueryElement);
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;
		MailIndexQuery q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		SearchResult results = MailIndexActivator.getService().searchItems(null, userUid, q);

		assertEquals(1, results.totalResults);
		MessageSearchResult messageSearchResult = results.results.get(0);

		assertEquals("Антон Плескановский", messageSearchResult.from.displayName);
		assertEquals("osef@gmail.com", messageSearchResult.from.address);
	}

	@Test
	public void testSearch_EsQuery_FieldAlias_From()
			throws MimeIOException, IOException, InterruptedException, ExecutionException {
		long imapUid = 1;
		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, imapUid, Collections.emptyList(), 1);
		refreshAllIndices();

		List<MailSummary> mails = new ArrayList<>();
		MailSummary summary1 = new MailSummary();
		summary1.uid = 1;
		summary1.parentId = bodyUid;
		summary1.flags = new HashSet<>(Arrays.asList("unread", "yeah"));

		mails.add(summary1);

		MailIndexActivator.getService().syncFlags(ItemValue.create(userUid, null), ItemValue.create(folderUid, null),
				mails);
		refreshAllIndices();

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.recordQuery = null;
		query.query = "from:roger.water@pinkfloyd.net";
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;

		MailIndexQuery q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		SearchResult results = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertEquals(1, results.totalResults);

		query.query = "de:roger.water@pinkfloyd.net";
		q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		results = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertEquals(1, results.totalResults);

		query.query = "von:roger.water@pinkfloyd.net";
		q = MailIndexQuery.simpleQuery(new MailboxFolderSearchQuery());
		q.query = query;
		results = MailIndexActivator.getService().searchItems(null, userUid, q);
		assertEquals(1, results.totalResults);

	}

	private void refreshAllIndices() throws RuntimeException {
		try {
			Map<String, IndexAliases> indexAliases = ESearchActivator.getClient().indices().getAlias().result();
			indexAliases.keySet().forEach(ESearchActivator::refreshIndex);
		} catch (Exception e) {
			throw new RuntimeException();
		}
	}

}
