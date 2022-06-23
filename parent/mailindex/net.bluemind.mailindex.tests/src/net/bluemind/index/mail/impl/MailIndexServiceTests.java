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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.google.common.io.Files;

import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxFolderSearchQuery;
import net.bluemind.backend.mail.api.MessageSearchResult;
import net.bluemind.backend.mail.api.SearchQuery;
import net.bluemind.backend.mail.api.SearchQuery.Header;
import net.bluemind.backend.mail.api.SearchQuery.HeaderQuery;
import net.bluemind.backend.mail.api.SearchQuery.LogicalOperator;
import net.bluemind.backend.mail.api.SearchQuery.SearchScope;
import net.bluemind.backend.mail.api.SearchResult;
import net.bluemind.backend.mail.api.SearchSort;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.indexing.IDSet;
import net.bluemind.backend.mail.replica.indexing.MailSummary;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.index.MailIndexActivator;
import net.bluemind.index.mail.MailIndexService;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.ShardStats;

public class MailIndexServiceTests extends AbstractSearchTests {

	@Test
	public void testSort() throws Exception {
		byte[] eml = Files.toByteArray(new File("data/test.eml"));

		storeBody("body1", eml);
		storeMessage("inbox", userUid, "body1", 1, Collections.emptyList(), 1l);

		String data = new String(eml);
		data = data.replace("12 Feb", "13 Feb");

		storeBody("body2", data.getBytes());
		storeMessage("inbox", userUid, "body2", 2, Collections.emptyList(), 2l);

		ESearchActivator.refreshIndex(INDEX_NAME);

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.recordQuery = null;
		query.query = "drug";
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;

		MailboxFolderSearchQuery q = new MailboxFolderSearchQuery();
		q.query = query;
		SearchResult sr = MailIndexActivator.getService().searchItems(userUid, q);
		assertEquals(2, sr.totalResults);

		// no sort criteria, default is by date DESC
		assertTrue(sr.results.get(0).date.after(sr.results.get(1).date));

		// by date ASC
		q.sort = SearchSort.byField("date", SearchSort.Order.Asc);
		sr = MailIndexActivator.getService().searchItems(userUid, q);
		assertEquals(2, sr.totalResults);
		assertTrue(sr.results.get(0).date.before(sr.results.get(1).date));

		// by date DESC
		q.sort = SearchSort.byField("date", SearchSort.Order.Desc);
		sr = MailIndexActivator.getService().searchItems(userUid, q);
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

		ESearchActivator.refreshIndex(INDEX_NAME);

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.recordQuery = null;
		query.query = "drug";
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;

		MailboxFolderSearchQuery q = new MailboxFolderSearchQuery();
		q.query = query;
		SearchResult sr = MailIndexActivator.getService().searchItems(userUid, q);
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

		ESearchActivator.refreshIndex(INDEX_NAME);

		QueryBuilder query = QueryBuilders.boolQuery().must(QueryBuilders.termQuery("in", "sent"));
		query = QueryBuilders.constantScoreQuery(query);
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

		ESearchActivator.refreshIndex(INDEX_NAME);

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.recordQuery = null;
		query.query = "drug";
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;

		MailboxFolderSearchQuery q = new MailboxFolderSearchQuery();
		q.query = query;
		SearchResult sr = MailIndexActivator.getService().searchItems(userUid, q);
		assertEquals(4, sr.totalResults);

		// no sort criteria, default is by date DESC
		assertTrue(sr.results.get(0).date.after(sr.results.get(1).date));
		assertTrue(sr.results.get(1).date.after(sr.results.get(2).date));
		assertTrue(sr.results.get(2).date.after(sr.results.get(3).date));

		// by date ASC
		q.sort = SearchSort.byField("date", SearchSort.Order.Asc);
		sr = MailIndexActivator.getService().searchItems(userUid, q);
		assertEquals(4, sr.totalResults);
		assertTrue(sr.results.get(0).date.before(sr.results.get(1).date));
		assertTrue(sr.results.get(1).date.before(sr.results.get(2).date));
		assertTrue(sr.results.get(2).date.before(sr.results.get(3).date));

		// by date DESC
		q.sort = SearchSort.byField("date", SearchSort.Order.Desc);
		sr = MailIndexActivator.getService().searchItems(userUid, q);
		assertEquals(4, sr.totalResults);
		assertTrue(sr.results.get(0).date.after(sr.results.get(1).date));
		assertTrue(sr.results.get(1).date.after(sr.results.get(2).date));
		assertTrue(sr.results.get(2).date.after(sr.results.get(3).date));
	}

	@Test
	public void testSimpleSearch() throws MimeIOException, IOException, InterruptedException, ExecutionException {
		Client c = ESearchActivator.getClient();
		long imapUid = 1;

		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, imapUid, Collections.emptyList());
		ESearchActivator.refreshIndex(INDEX_NAME);

		SearchResponse resp = c.prepareSearch(INDEX_NAME)
				.setQuery(QueryBuilders.queryStringQuery("id:\"" + entryId(44) + "\"")).execute().get();
		assertEquals(1L, resp.getHits().getTotalHits().value);

		Map<String, Object> source = resp.getHits().getAt(0).getSourceAsMap();
		assertEquals(bodyUid, source.get("parentId"));
		assertEquals("SubjectTest", source.get("subject"));
		assertEquals(entryId(44), source.get("id"));

		// search by IN in alias
		resp = c.prepareSearch(ALIAS).setQuery(QueryBuilders.termQuery("in", folderUid)).execute().get();
		assertEquals(1L, resp.getHits().getTotalHits().value);

		source = resp.getHits().getAt(0).getSourceAsMap();
		assertEquals(bodyUid, source.get("parentId"));
		assertEquals("SubjectTest", source.get("subject"));
		assertEquals(entryId(44), source.get("id"));
	}

	@Test
	public void testDelete() throws MimeIOException, IOException, InterruptedException, ExecutionException {
		Client c = ESearchActivator.getClient();
		int imapUid = 1;

		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, imapUid, Collections.emptyList());
		ESearchActivator.refreshIndex(INDEX_NAME);

		SearchResponse resp = c.prepareSearch(INDEX_NAME)
				.setQuery(QueryBuilders.queryStringQuery("id:\"" + entryId(44) + "\"")).execute().get();
		assertEquals(1L, resp.getHits().getTotalHits().value);

		List<MailboxItemFlag> deleteFlag = Arrays.asList(MailboxItemFlag.System.Deleted.value());
		storeMessage(mboxUid, userUid, bodyUid, imapUid, deleteFlag);
		ESearchActivator.refreshIndex(INDEX_NAME);
		IDSet set = IDSet.create(Arrays.asList(imapUid));
		MailIndexActivator.getService().expunge(ItemValue.create(userUid, null), ItemValue.create(folderUid, null),
				set);
		ESearchActivator.refreshIndex(INDEX_NAME);

		resp = c.prepareSearch(ALIAS).setQuery(QueryBuilders.queryStringQuery("id:\"" + entryId(44) + "\"")).execute()
				.get();
		assertEquals(0L, resp.getHits().getTotalHits().value);
	}

	@Test
	public void testBigExpunge() throws MimeIOException, IOException, InterruptedException, ExecutionException {
		Client c = ESearchActivator.getClient();

		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, 1l, Collections.emptyList(), 1);
		storeMessage(mboxUid, userUid, bodyUid, 2l, Collections.emptyList(), 2);
		ESearchActivator.refreshIndex(INDEX_NAME);

		SearchResponse resp = c.prepareSearch(INDEX_NAME)
				.setQuery(QueryBuilders.queryStringQuery("id:\"" + entryId(2) + "\"")).execute().get();
		assertEquals(1L, resp.getHits().getTotalHits().value);
		resp = c.prepareSearch(INDEX_NAME).setQuery(QueryBuilders.queryStringQuery("id:\"" + entryId(2) + "\""))
				.execute().get();
		assertEquals(1L, resp.getHits().getTotalHits().value);

		Set<Integer> ordered = Sets.newLinkedHashSet();
		ordered.add(-1); // does not exist
		ordered.add(1);
		ordered.add(2);

		MailIndexActivator.getService().cleanupFolder(ItemValue.create(userUid, null),
				ItemValue.create(folderUid, null), ordered);

		ESearchActivator.refreshIndex(INDEX_NAME);
		resp = c.prepareSearch(INDEX_NAME).setQuery(QueryBuilders.queryStringQuery("id:\"" + entryId(1) + "\""))
				.execute().get();
		assertEquals(0L, resp.getHits().getTotalHits().value);
		resp = c.prepareSearch(INDEX_NAME).setQuery(QueryBuilders.queryStringQuery("id:\"" + entryId(2) + "\""))
				.execute().get();
		assertEquals(0L, resp.getHits().getTotalHits().value);
	}

	@Test
	public void testDeleteBox() throws Exception {
		byte[] eml = Files.toByteArray(new File("data/test.eml"));

		storeBody("body1", eml);
		storeMessage("inbox", userUid, "body1", 0, Collections.emptyList());
		for (int i = 0; i < 2000; i++) {
			storeMessage("sent", userUid, "body1", i + 1, Collections.emptyList());
		}

		ItemValue<Mailbox> mailbox = ItemValue.create(userUid, new Mailbox());
		ItemValue<MailboxFolder> sentFolder = ItemValue.create("sent", new MailboxFolder());
		ItemValue<MailboxFolder> inboxFolder = ItemValue.create("inbox", new MailboxFolder());
		IDSet emptySet = IDSet.create(new ArrayList<>());

		ESearchActivator.refreshIndex(INDEX_NAME);

		MailIndexActivator.getService().deleteBox(mailbox, "sent");

		ESearchActivator.refreshIndex(INDEX_NAME);

		List<MailSummary> sentSummaries = MailIndexActivator.getService().fetchSummary(mailbox, sentFolder, emptySet);
		assertEquals(0, sentSummaries.size());
		List<MailSummary> inboxSummaries = MailIndexActivator.getService().fetchSummary(mailbox, inboxFolder, emptySet);
		assertEquals(1, inboxSummaries.size());
	}

	@Test
	public void testDeleteMailbox() throws Exception {
		byte[] eml = Files.toByteArray(new File("data/test.eml"));

		storeBody("body1", eml);
		int countPerFolder = 1000;
		for (int i = 0; i < countPerFolder; i++) {
			storeMessage("inbox", userUid, "body1", i, Collections.emptyList());
		}
		for (int i = 0; i < countPerFolder; i++) {
			storeMessage("sent", userUid, "body1", i + countPerFolder, Collections.emptyList());
		}

		ItemValue<Mailbox> mailbox = ItemValue.create(userUid, new Mailbox());
		ItemValue<MailboxFolder> sentFolder = ItemValue.create("sent", new MailboxFolder());
		ItemValue<MailboxFolder> inboxFolder = ItemValue.create("inbox", new MailboxFolder());
		IDSet emptySet = IDSet.create(new ArrayList<>());

		ESearchActivator.refreshIndex(INDEX_NAME);

		MailIndexActivator.getService().deleteMailbox(userUid);

		ESearchActivator.refreshIndex(INDEX_NAME);

		Client client = ESearchActivator.getClient();
		MailIndexService service = (MailIndexService) MailIndexActivator.getService();
		String userAlias = service.getIndexAliasName(userUid);
		assertNull(client.admin().indices().prepareGetAliases(userAlias).get().getAliases().get(userAlias));
		List<MailSummary> sentSummaries = service.fetchSummary(mailbox, sentFolder, emptySet);
		assertEquals(0, sentSummaries.size());
		List<MailSummary> inboxSummaries = service.fetchSummary(mailbox, inboxFolder, emptySet);
		assertEquals(0, inboxSummaries.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUpdateFlags() throws MimeIOException, IOException, InterruptedException, ExecutionException {
		Client c = ESearchActivator.getClient();

		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, 1, Collections.emptyList(), 1);
		storeMessage(mboxUid, userUid, bodyUid, 2, Collections.emptyList(), 2);
		ESearchActivator.refreshIndex(INDEX_NAME);

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
		ESearchActivator.refreshIndex(INDEX_NAME);

		SearchResponse resp = c.prepareSearch(INDEX_NAME)
				.setQuery(QueryBuilders.queryStringQuery("id:\"" + entryId(1) + "\"")).execute().get();
		assertEquals(1L, resp.getHits().getTotalHits().value);
		List<String> flags = (List<String>) resp.getHits().getAt(0).getSourceAsMap().get("is");
		assertTrue(flags.contains("flag1"));
		assertTrue(flags.contains("flag2"));

		resp = c.prepareSearch(INDEX_NAME).setQuery(QueryBuilders.queryStringQuery("id:\"" + entryId(2) + "\""))
				.execute().get();
		assertEquals(1L, resp.getHits().getTotalHits().value);
		flags = (List<String>) resp.getHits().getAt(0).getSourceAsMap().get("is");
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
		ESearchActivator.refreshIndex(INDEX_NAME);

		Set<String> folders = MailIndexActivator.getService().getFolders(userUid);
		assertTrue(folders.contains(f1));
		assertTrue(folders.contains(f2));
	}

	@Test
	public void testMoveIndexToNewIndex() throws Exception {
		Client c = ESearchActivator.getClient();

		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, 1, Collections.emptyList());
		storeMessage(mboxUid, userUid, bodyUid, 2, Collections.emptyList());
		ESearchActivator.refreshIndex(INDEX_NAME);

		MailIndexActivator.getService().moveMailbox(userUid, "mailspool_test2");
		ESearchActivator.refreshIndex(INDEX_NAME);

		SearchResponse resp = c.prepareSearch("mailspool_test2")
				.setQuery(QueryBuilders.queryStringQuery("id:\"" + entryId(44) + "\"")).execute().get();
		assertEquals(1L, resp.getHits().getTotalHits().value);

		resp = c.prepareSearch(ALIAS).setQuery(QueryBuilders.queryStringQuery("id:\"" + entryId(44) + "\"")).execute()
				.get();
		assertEquals(1L, resp.getHits().getTotalHits().value);

		resp = c.prepareSearch(INDEX_NAME).setQuery(QueryBuilders.queryStringQuery("id:\"" + entryId(44) + "\""))
				.execute().get();
		assertEquals(0L, resp.getHits().getTotalHits().value);
	}

	@Test
	public void testGetStats() throws MimeIOException, IOException {

		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, 1, Collections.emptyList());
		storeMessage(mboxUid, userUid, bodyUid, 2, Collections.emptyList());
		ESearchActivator.refreshIndex(INDEX_NAME);

		List<ShardStats> stats = MailIndexActivator.getService().getStats();
		assertNotNull(stats);
		MailIndexActivator.getService().moveMailbox(userUid, "mailspool_test2");
		ESearchActivator.refreshIndex(INDEX_NAME);

		stats = MailIndexActivator.getService().getStats();
		assertNotNull(stats);
	}

	@Test
	public void testSearch() throws MimeIOException, IOException, InterruptedException, ExecutionException {
		long imapUid = 1;
		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, imapUid, Collections.emptyList());
		ESearchActivator.refreshIndex(INDEX_NAME);

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.query = "SubjectTest";
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;
		MailboxFolderSearchQuery q = new MailboxFolderSearchQuery();
		q.query = query;
		SearchResult results = MailIndexActivator.getService().searchItems(userUid, q);

		assertOnlyResultIsTestEml(results);
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
		ESearchActivator.refreshIndex(INDEX_NAME);

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.query = "SubjectTest"; // Subject
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;
		MailboxFolderSearchQuery q = new MailboxFolderSearchQuery();
		q.query = query;
		SearchResult results = MailIndexActivator.getService().searchItems(userUid, q);
		assertOnlyResultIsTestEml(results);

		query.query = "Water"; // From
		results = MailIndexActivator.getService().searchItems(userUid, q);
		assertOnlyResultIsTestEml(results);

		query.query = "Barrett"; // To
		results = MailIndexActivator.getService().searchItems(userUid, q);
		assertOnlyResultIsTestEml(results);

		query.query = "Lost"; // Content
		results = MailIndexActivator.getService().searchItems(userUid, q);
		assertOnlyResultIsTestEml(results);
	}

	@Test
	public void testPaginatedSearch() throws MimeIOException, IOException, InterruptedException, ExecutionException {
		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		int batchSize = 1000;
		int totalSize = batchSize + 10;
		for (int i = 1; i <= totalSize; i++) {
			storeMessage(mboxUid, userUid, bodyUid, 1, Collections.emptyList(), i);
		}
		ESearchActivator.refreshIndex(INDEX_NAME);

		SearchQuery query = new SearchQuery();
		query.maxResults = Integer.MAX_VALUE + 1l;
		query.offset = 0;
		query.query = "SubjectTest";
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;
		MailboxFolderSearchQuery q = new MailboxFolderSearchQuery();
		q.query = query;
		SearchResult results = MailIndexActivator.getService().searchItems(userUid, q);

		assertEquals(totalSize, results.totalResults);
		assertEquals(totalSize, results.results.size());
	}

	@Test
	public void testSearchBody() throws MimeIOException, IOException, InterruptedException, ExecutionException {
		long imapUid = 1;
		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, imapUid, Collections.emptyList());
		ESearchActivator.refreshIndex(INDEX_NAME);

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.query = "drug";
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;
		MailboxFolderSearchQuery q = new MailboxFolderSearchQuery();
		q.query = query;
		SearchResult results = MailIndexActivator.getService().searchItems(userUid, q);

		assertOnlyResultIsTestEml(results);
	}

	@Test
	public void testSearch_InvalidQuery()
			throws MimeIOException, IOException, InterruptedException, ExecutionException {
		long imapUid = 1;
		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, imapUid, Collections.emptyList());
		ESearchActivator.refreshIndex(INDEX_NAME);

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.query = "invalid\"";
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;
		MailboxFolderSearchQuery q = new MailboxFolderSearchQuery();
		q.query = query;
		SearchResult results = MailIndexActivator.getService().searchItems(userUid, q);

		assertEquals(0, results.totalResults);
	}

	@Test
	public void testSearchByFlags() throws MimeIOException, IOException, InterruptedException, ExecutionException {
		long imapUid = 1;
		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, imapUid, Collections.emptyList(), 1);
		ESearchActivator.refreshIndex(INDEX_NAME);

		List<MailSummary> mails = new ArrayList<>();
		MailSummary summary1 = new MailSummary();
		summary1.uid = 1;
		summary1.parentId = bodyUid;
		summary1.flags = new HashSet<>(Arrays.asList("unread", "yeah"));

		mails.add(summary1);

		MailIndexActivator.getService().syncFlags(ItemValue.create(userUid, null), ItemValue.create(folderUid, null),
				mails);
		ESearchActivator.refreshIndex(INDEX_NAME);

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.recordQuery = "is:yeah";
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;

		MailboxFolderSearchQuery q = new MailboxFolderSearchQuery();
		q.query = query;
		SearchResult results = MailIndexActivator.getService().searchItems(userUid, q);

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
		ESearchActivator.refreshIndex(INDEX_NAME);

		List<MailSummary> mails = new ArrayList<>();
		MailSummary summary1 = new MailSummary();
		summary1.uid = 1;
		summary1.parentId = bodyUid;
		summary1.flags = new HashSet<>(Arrays.asList("unread", "yeah"));

		mails.add(summary1);

		MailIndexActivator.getService().syncFlags(ItemValue.create(userUid, null), ItemValue.create(folderUid, null),
				mails);
		ESearchActivator.refreshIndex(INDEX_NAME);

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.recordQuery = null;
		query.query = "drug";
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;

		MailboxFolderSearchQuery q = new MailboxFolderSearchQuery();
		q.query = query;
		SearchResult results = MailIndexActivator.getService().searchItems(userUid, q);
		assertEquals(1, results.totalResults);

		query.recordQuery = "is:unread";
		query.query = null;
		q = new MailboxFolderSearchQuery();
		q.query = query;
		results = MailIndexActivator.getService().searchItems(userUid, q);
		assertEquals(1, results.totalResults);

		query.recordQuery = "is:unread";
		query.query = "SubjectTest";
		q = new MailboxFolderSearchQuery();
		q.query = query;
		results = MailIndexActivator.getService().searchItems(userUid, q);
		assertEquals(1, results.totalResults);

		query.recordQuery = "is:unread subject:SubjectTest";
		query.query = null;
		q = new MailboxFolderSearchQuery();
		q.query = query;
		results = MailIndexActivator.getService().searchItems(userUid, q);
		assertEquals(1, results.totalResults);

		query.recordQuery = "is:unread";
		query.query = "drug";
		q = new MailboxFolderSearchQuery();
		q.query = query;
		results = MailIndexActivator.getService().searchItems(userUid, q);
		assertEquals(1, results.totalResults);

		query.recordQuery = "is:yeah";
		query.query = "my drug";
		q = new MailboxFolderSearchQuery();
		q.query = query;
		results = MailIndexActivator.getService().searchItems(userUid, q);
		assertEquals(1, results.totalResults);

		query.recordQuery = "is:yeah";
		query.query = "\"my drug\"";
		q = new MailboxFolderSearchQuery();
		q.query = query;
		results = MailIndexActivator.getService().searchItems(userUid, q);
		assertEquals(1, results.totalResults);

		query.recordQuery = "is:wat";
		query.query = "drug";
		q = new MailboxFolderSearchQuery();
		q.query = query;
		results = MailIndexActivator.getService().searchItems(userUid, q);
		assertEquals(0, results.totalResults);

		query.recordQuery = "is:unread";
		query.query = "pouet";
		q = new MailboxFolderSearchQuery();
		q.query = query;
		results = MailIndexActivator.getService().searchItems(userUid, q);
		assertEquals(0, results.totalResults);

	}

	@Test
	public void testSearchByHeader() throws MimeIOException, IOException, InterruptedException, ExecutionException {
		long imapUid = 1;
		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, imapUid, Collections.emptyList());
		ESearchActivator.refreshIndex(INDEX_NAME);

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
		MailboxFolderSearchQuery q = new MailboxFolderSearchQuery();
		q.query = query;
		SearchResult results = MailIndexActivator.getService().searchItems(userUid, q);

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

		q = new MailboxFolderSearchQuery();
		q.query = query;
		results = MailIndexActivator.getService().searchItems(userUid, q);

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

		q = new MailboxFolderSearchQuery();
		q.query = query;
		results = MailIndexActivator.getService().searchItems(userUid, q);

		assertEquals(0, results.totalResults);
	}

	@Test
	public void testSearchByMessageId() throws MimeIOException, IOException, InterruptedException, ExecutionException {
		long imapUid = 1;
		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, imapUid, Collections.emptyList());
		ESearchActivator.refreshIndex(INDEX_NAME);

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.messageId = "<7FA4B249-C434-4F98-A447-A0F0C8D42A4E@pinkfloy.net>";
		MailboxFolderSearchQuery q = new MailboxFolderSearchQuery();
		q.query = query;
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;

		SearchResult results = MailIndexActivator.getService().searchItems(userUid, q);

		assertEquals(1, results.totalResults);
		MessageSearchResult messageSearchResult = results.results.get(0);
		assertEquals(44l, messageSearchResult.itemId);

		query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.messageId = "idontexist";
		q = new MailboxFolderSearchQuery();
		q.query = query;
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;
		results = MailIndexActivator.getService().searchItems(userUid, q);

		assertEquals(0, results.totalResults);
	}

	@Test
	public void testSearchByReferences() throws MimeIOException, IOException, InterruptedException, ExecutionException {
		long imapUid = 1;
		byte[] eml = Files.toByteArray(new File("data/test.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, imapUid, Collections.emptyList());
		ESearchActivator.refreshIndex(INDEX_NAME);

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.references = "<19980507220459.5655.qmail@warren.demon.co.uk>";
		MailboxFolderSearchQuery q = new MailboxFolderSearchQuery();
		q.query = query;
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;

		SearchResult results = MailIndexActivator.getService().searchItems(userUid, q);

		assertEquals(1, results.totalResults);
		MessageSearchResult messageSearchResult = results.results.get(0);
		assertEquals(44l, messageSearchResult.itemId);

		query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.references = "idontexist";
		q = new MailboxFolderSearchQuery();
		q.query = query;
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;
		results = MailIndexActivator.getService().searchItems(userUid, q);

		assertEquals(0, results.totalResults);
	}

	@Test
	public void testSearchCheckCyrillicRecipient() throws IOException {
		long imapUid = 1;
		byte[] eml = Files.toByteArray(new File("data/testRecipient.eml"));
		storeBody(bodyUid, eml);
		storeMessage(mboxUid, userUid, bodyUid, imapUid, Collections.emptyList());
		ESearchActivator.refreshIndex(INDEX_NAME);

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
		MailboxFolderSearchQuery q = new MailboxFolderSearchQuery();
		q.query = query;
		SearchResult results = MailIndexActivator.getService().searchItems(userUid, q);

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
		ESearchActivator.refreshIndex(INDEX_NAME);

		List<MailSummary> mails = new ArrayList<>();
		MailSummary summary1 = new MailSummary();
		summary1.uid = 1;
		summary1.parentId = bodyUid;
		summary1.flags = new HashSet<>(Arrays.asList("unread", "yeah"));

		mails.add(summary1);

		MailIndexActivator.getService().syncFlags(ItemValue.create(userUid, null), ItemValue.create(folderUid, null),
				mails);
		ESearchActivator.refreshIndex(INDEX_NAME);

		SearchQuery query = new SearchQuery();
		query.maxResults = 10;
		query.offset = 0;
		query.recordQuery = null;
		query.query = "from:roger.water@pinkfloyd.net";
		query.scope = new SearchScope();
		query.scope.isDeepTraversal = true;

		MailboxFolderSearchQuery q = new MailboxFolderSearchQuery();
		q.query = query;
		SearchResult results = MailIndexActivator.getService().searchItems(userUid, q);
		assertEquals(1, results.totalResults);

		query.query = "de:roger.water@pinkfloyd.net";
		q = new MailboxFolderSearchQuery();
		q.query = query;
		results = MailIndexActivator.getService().searchItems(userUid, q);
		assertEquals(1, results.totalResults);

		query.query = "von:roger.water@pinkfloyd.net";
		q = new MailboxFolderSearchQuery();
		q.query = query;
		results = MailIndexActivator.getService().searchItems(userUid, q);
		assertEquals(1, results.totalResults);

	}

}
