/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Files;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import net.bluemind.backend.mail.replica.indexing.IMailIndexService.BulkOp;
import net.bluemind.index.MailIndexActivator;
import net.bluemind.index.mail.MailIndexService;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.elasticsearch.ElasticsearchClientConfig;

public class DefaultMailIndexServiceTests extends MailIndexServiceTests {

	@Override
	@Before
	public void before() throws Exception {
		String ACTIVATE_ALIAS_RING_MODE = "elasticsearch.indexation.alias_mode.ring";
		String ALIAS_RING_MODE_ALIAS_COUNT_MULTIPLIER = "elasticsearch.indexation.alias_mode.mode_ring.alias_count_multiplier";
		System.setProperty(ACTIVATE_ALIAS_RING_MODE, "false");
		System.setProperty(ALIAS_RING_MODE_ALIAS_COUNT_MULTIPLIER, "0");
		ElasticsearchClientConfig.reload();
		super.before();
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

}
