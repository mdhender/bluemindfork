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

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.join.query.JoinQueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.lib.vertx.utils.ThrottleMessages;

public class ExpungeVerticle extends AbstractVerticle {

	private Logger logger = LoggerFactory.getLogger(ExpungeVerticle.class);

	@Override
	public void start() throws Exception {
		super.start();

		ThrottleMessages<JsonObject> tm = new ThrottleMessages<JsonObject>((msg) -> msg.body().getString("index"),
				this::expunge, vertx, 10000);

		super.vertx.eventBus().consumer("index.mailspool.cleanup", (Message<JsonObject> msg) -> tm.handle(msg));
	}

	private void expunge(Message<JsonObject> message) {
		String index = message.body().getString("index");
		logger.info(" *** cleanup parents begin. indice {}", index);

		long time = System.currentTimeMillis();
		QueryBuilder queryBuilder = QueryBuilders.boolQuery()
				.mustNot(JoinQueryBuilders.hasChildQuery(MailIndexService.CHILD_TYPE, QueryBuilders.matchAllQuery(),
						ScoreMode.None))//
				.must(QueryBuilders.termQuery(MailIndexService.JOIN_FIELD, MailIndexService.PARENT_TYPE));
		long deleted = DeleteByQueryAction.INSTANCE.newRequestBuilder(MailIndexService.getIndexClient())
				.filter(queryBuilder).source(index).get().getDeleted();

		logger.info(" *** cleanup parents ({}) took {} ms", deleted, (System.currentTimeMillis() - time));
	}

}
