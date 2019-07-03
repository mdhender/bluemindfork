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
package net.bluemind.im.persistance;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import net.bluemind.im.api.IMMessage;

public class IMIndexStore {

	private static final Logger logger = LoggerFactory.getLogger(IMIndexStore.class);

	private Client client;

	public IMIndexStore(Client client) {
		this.client = client;
	}

	public List<IMMessage> getLastMessagesBetween(String user1, String user2, int messagesCount) {

		StringBuilder query = new StringBuilder();
		query.append("((from:\"").append(user2).append("\"");
		query.append(" AND to:\"").append(user1).append("\")");
		query.append(" OR (to:\"").append(user2).append("\"");
		query.append(" AND from:\"").append(user1).append("\"))");

		if (logger.isDebugEnabled()) {
			logger.debug("getLastMessagesBetween {} and {}: {}", user1, user2, query.toString());
		}

		return doElasticSearchQuery(query, messagesCount);
	}

	public List<IMMessage> getGroupChatHistory(String groupChatId) {

		StringBuilder query = new StringBuilder(1024);
		query.append("to:\"");
		query.append(groupChatId);
		query.append("\"");

		if (logger.isDebugEnabled()) {
			logger.debug("getGroupChatHistory {}: {}", groupChatId, query.toString());
		}

		return doElasticSearchQuery(query, 10000);
	}

	private List<IMMessage> doElasticSearchQuery(StringBuilder query, int size) {

		SearchRequestBuilder searchBuilder = client.prepareSearch("im");
		searchBuilder.setQuery(QueryBuilders.queryStringQuery(query.toString()));
		searchBuilder.storedFields("message", "from", "to", "timecreate");
		searchBuilder.addSort("timecreate", SortOrder.DESC);
		searchBuilder.setSize(size);
		SearchResponse sr = searchBuilder.execute().actionGet();

		List<IMMessage> messages = new ArrayList<IMMessage>();

		for (SearchHit sh : sr.getHits()) {
			IMMessage m = new IMMessage();
			m.body = (String) sh.field("message").getValue();
			m.from = (String) sh.field("from").getValue();
			m.to = (String) sh.field("to").getValue();
			m.timestamp = new Date((Long) sh.field("timecreate").getValue());
			messages.add(m);
		}

		return Lists.reverse(messages);
	}
}
