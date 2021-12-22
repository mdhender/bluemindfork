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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.index.mail.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.join.query.JoinQueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.junit.Test;

import io.vertx.core.json.JsonObject;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.lib.elasticsearch.ESearchActivator;

public class WebmailStyleSearchTests extends AbstractSearchTests {

	@Test
	public void addTwoMailsThenSearch() throws IOException {
		System.err.println("Test starts....");
		addEml(1L, "data/test.eml", 1, MailboxItemFlag.System.Seen, MailboxItemFlag.System.Answered);
		System.err.println("Second eml");
		addEml(2L, "data/testAttach.eml", 2, MailboxItemFlag.System.Flagged);
		System.err.println("EMLs added.");
		ESearchActivator.refreshIndex(INDEX_NAME);
		QueryBuilder q = QueryBuilders.boolQuery()//
				.must(JoinQueryBuilders.hasParentQuery("body", QueryBuilders.queryStringQuery("content:\"drug\""),
						false))//
				.must(QueryBuilders.termQuery("in", folderUid))//
		;
		System.err.println("Q: " + q);

		Client client = ESearchActivator.getClient();
		SearchResponse results = client.prepareSearch(INDEX_NAME)//
				.setQuery(q).setFetchSource(true)//
				.storedFields("date", "size", "headers.date", "headers.from", "headers.to", "headers.cc", "subject",
						"content-type", "reply-to", "disposition-notification-to", "list-post", "x-priority",
						"x-bm-event", "x-bm-rsvp", "x-bm-resourcebooking", "x-bm-folderuid", "x-bm-foldertype", "is")//
				.setTypes("recordOrBody").setFrom(0).setSize(40)//
				.execute().actionGet();
		JsonObject js = new JsonObject(results.toString());
		System.err.println("resp: " + js.encodePrettily());
		assertTrue(results.getHits().getTotalHits().value > 0);

	}

	@Test
	public void addTwoMailsRmOneRecordFindOrphan() throws IOException {
		System.err.println("Test starts....");
		addEml(1L, "data/test.eml", 1, MailboxItemFlag.System.Seen, MailboxItemFlag.System.Answered);
		System.err.println("Second eml");
		addEml(2L, "data/testAttach.eml", 2, MailboxItemFlag.System.Flagged);
		System.err.println("EMLs added.");
		ESearchActivator.refreshIndex(INDEX_NAME);
		QueryBuilder q = QueryBuilders.boolQuery()//
				.must(JoinQueryBuilders.hasParentQuery("body", QueryBuilders.queryStringQuery("content:\"drug\""),
						false))//
				.must(QueryBuilders.termQuery("in", folderUid))//
		;
		System.err.println("Q: " + q);

		Client client = ESearchActivator.getClient();
		SearchResponse results = client.prepareSearch(INDEX_NAME)//
				.setQuery(q).setFetchSource(true)//
				.storedFields("date", "size", "headers.date", "headers.from", "headers.to", "headers.cc", "subject",
						"content-type", "reply-to", "disposition-notification-to", "list-post", "x-priority",
						"x-bm-event", "x-bm-rsvp", "x-bm-resourcebooking", "x-bm-folderuid", "x-bm-foldertype", "is")//
				.setTypes("recordOrBody").setFrom(0).setSize(40)//
				.execute().actionGet();
		JsonObject js = new JsonObject(results.toString());
		System.err.println("resp: " + js.encodePrettily());
		assertEquals(1, results.getHits().getTotalHits().value);
		results.getHits().forEach((SearchHit hit) -> {
			DeleteResponse delResponse = client.prepareDelete(INDEX_NAME, "recordOrBody", hit.getId())
					.setRefreshPolicy(RefreshPolicy.WAIT_UNTIL).execute().actionGet();
			System.err.println("DEL: " + delResponse);
		});

		QueryBuilder orphans = QueryBuilders.boolQuery()
				.mustNot(JoinQueryBuilders.hasChildQuery("record", QueryBuilders.matchAllQuery(), ScoreMode.None))//
				.must(QueryBuilders.termQuery("body_msg_link", "body"));

		SearchResponse orphanFound = client.prepareSearch(INDEX_NAME)//
				.setQuery(orphans).setFetchSource(true)//
				.setTypes("recordOrBody").setFrom(0).setSize(40)//
				.execute().actionGet();

		System.err.println("ORPHANS:\n" + new JsonObject(orphanFound.toString()).encodePrettily());
		assertEquals(1, orphanFound.getHits().getTotalHits().value);

	}

}
