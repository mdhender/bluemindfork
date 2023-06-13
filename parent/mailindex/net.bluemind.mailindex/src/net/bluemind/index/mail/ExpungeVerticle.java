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
package net.bluemind.index.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.lib.vertx.utils.ThrottleMessages;

public class ExpungeVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(ExpungeVerticle.class);

	@Override
	public void start() throws Exception {
		super.start();

		ThrottleMessages<JsonObject> tm = new ThrottleMessages<>(msg -> msg.body().getString("index"), this::expunge,
				vertx, 120000);

		super.vertx.eventBus().consumer("index.mailspool.cleanup", tm::handle);
	}

	private void expunge(Message<JsonObject> message) {
		String index = message.body().getString("index");
		long time = System.currentTimeMillis();
		Query filter = QueryBuilders.bool() //
				.mustNot(n -> n.hasChild(c -> c //
						.type(MailIndexService.CHILD_TYPE) //
						.scoreMode(ChildScoreMode.None) //
						.query(q -> q.matchAll(a -> a))))
				.must(m -> m.term(t -> t.field(MailIndexService.JOIN_FIELD).value(MailIndexService.PARENT_TYPE)))
				.build()._toQuery();
		ElasticsearchClient esClient = MailIndexService.getIndexClient();
		try {
			long deleted = esClient
					.deleteByQuery(d -> d.index(index).query(t -> t.constantScore(s -> s.filter(filter)))).deleted();
			logger.info(" *** cleanup parents in {} ({} deletion(s)) took {} ms", index, deleted,
					(System.currentTimeMillis() - time));
		} catch (Exception e) {
			logger.error("[es][expunge] Unable to expunge message in {}", index, e);
		}
	}

}
