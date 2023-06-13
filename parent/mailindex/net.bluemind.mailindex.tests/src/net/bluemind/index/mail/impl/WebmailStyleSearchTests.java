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

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
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
		ElasticsearchClient client = ESearchActivator.getClient();
		SearchResponse<ObjectNode> results = client.search(s -> s //
				.index(INDEX_NAME)//
				.query(q -> q.bool(b -> b //
						.must(m -> m.hasParent(c -> c.parentType("body")
								.query(p -> p.queryString(qs -> qs.query("content:\"drug\""))).score(false)))
						.must(m -> m.term(t -> t.field("in").value(folderUid))))) //
				.source(so -> so.fetch(true)) //
				.storedFields("date", "size", "headers.date", "headers.from", "headers.to", "headers.cc", "subject",
						"content-type", "reply-to", "disposition-notification-to", "list-post", "x-priority",
						"x-bm-event", "x-bm-rsvp", "x-bm-resourcebooking", "x-bm-folderuid", "x-bm-foldertype", "is")//
				.from(0).size(40), ObjectNode.class);
//		JsonObject js = new JsonObject(results.toString());
		System.err.println("resp: " + results.toString());
		assertTrue(results.hits().total().value() > 0);

	}

	@Test
	public void addTwoMailsRmOneRecordFindOrphan() throws IOException {
		System.err.println("Test starts....");
		addEml(1L, "data/test.eml", 1, MailboxItemFlag.System.Seen, MailboxItemFlag.System.Answered);
		System.err.println("Second eml");
		addEml(2L, "data/testAttach.eml", 2, MailboxItemFlag.System.Flagged);
		System.err.println("EMLs added.");
		ESearchActivator.refreshIndex(INDEX_NAME);
		ElasticsearchClient client = ESearchActivator.getClient();
		SearchResponse<ObjectNode> results = client.search(s -> s //
				.index(INDEX_NAME) //
				.query(q -> q.bool(b -> b //
						.must(m -> m.hasParent(c -> c.parentType("body")
								.query(p -> p.queryString(qs -> qs.query("content:\"drug\""))).score(false)))
						.must(m -> m.term(t -> t.field("in").value(folderUid))))) //
				.source(so -> so.fetch(true)) //
				.storedFields("date", "size", "headers.date", "headers.from", "headers.to", "headers.cc", "subject",
						"content-type", "reply-to", "disposition-notification-to", "list-post", "x-priority",
						"x-bm-event", "x-bm-rsvp", "x-bm-resourcebooking", "x-bm-folderuid", "x-bm-foldertype", "is")//
				.from(0).size(40), ObjectNode.class);
//		JsonObject js = new JsonObject(results.toString());
		System.err.println("resp: " + results.toString());
		assertEquals(1, results.hits().total().value());
		for (Hit<ObjectNode> hit : results.hits().hits()) {
			DeleteResponse delResponse = client.delete(d -> d.index(INDEX_NAME).id(hit.id()).refresh(Refresh.WaitFor));
			System.err.println("DEL: " + delResponse);
		}

		SearchResponse<Void> orphanFound = client.search(s -> s //
				.index(INDEX_NAME) //
				.query(q -> q.bool(b -> b
						.must(m -> m.hasChild(
								c -> c.type("record").query(qa -> qa.matchAll(a -> a)).scoreMode(ChildScoreMode.None))) //
						.must(m -> m.term(t -> t.field("body_msg_link").value("body"))))) //
				.source(so -> so.fetch(true))//
				.from(0).size(40), Void.class);

		assertEquals(1, orphanFound.hits().total().value());

	}

}
