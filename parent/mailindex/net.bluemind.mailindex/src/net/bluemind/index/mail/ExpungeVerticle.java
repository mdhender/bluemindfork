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
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.join.query.JoinQueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import net.bluemind.index.mail.BulkData.UnitDelete;
import net.bluemind.lib.vertx.utils.ThrottleMessages;

public class ExpungeVerticle extends Verticle {

	private Logger logger = LoggerFactory.getLogger(ExpungeVerticle.class);

	@Override
	public void start() {
		super.start();

		ThrottleMessages<JsonObject> tm = new ThrottleMessages<JsonObject>((msg) -> msg.body().getString("index"),
				this::expunge, vertx, 10000);

		super.vertx.eventBus().registerHandler("index.mailspool.cleanup",
				(Message<? extends JsonObject> msg) -> tm.handle(msg));
	}

	private void expunge(Message<? extends JsonObject> message) {
		String index = message.body().getString("index");
		logger.info(" *** cleanup parents begin. indice {}", index);

		long time = System.currentTimeMillis();

		BulkData data = new BulkData(MailIndexService.getIndexClient());
		data.indexName = index;
		data.type = MailIndexService.MAILSPOOL_TYPE;
		data.query = QueryBuilders.boolQuery()
				.mustNot(JoinQueryBuilders.hasChildQuery(MailIndexService.CHILD_TYPE, QueryBuilders.matchAllQuery(),
						ScoreMode.None))//
				.must(QueryBuilders.termQuery(MailIndexService.JOIN_FIELD, MailIndexService.PARENT_TYPE));
		data.unitDelete = new UnitDelete() {

			@Override
			public DeleteRequestBuilder build(Client client, SearchHit hit) {
				DeleteRequestBuilder drb = client.prepareDelete().setIndex(index)
						.setType(MailIndexService.MAILSPOOL_TYPE).setId(hit.getId());
				return drb;
			}
		};

		long deleted = data.execute();

		logger.info(" *** cleanup parents ({}) took {} ms", deleted, (System.currentTimeMillis() - time));
	}

}
