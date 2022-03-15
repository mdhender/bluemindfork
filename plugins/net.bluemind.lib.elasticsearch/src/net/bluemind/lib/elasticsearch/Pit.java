/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.lib.elasticsearch;

import org.elasticsearch.action.search.ClosePointInTimeAction;
import org.elasticsearch.action.search.ClosePointInTimeRequest;
import org.elasticsearch.action.search.OpenPointInTimeAction;
import org.elasticsearch.action.search.OpenPointInTimeRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pit implements AutoCloseable {

	public final String id;
	private final Client client;
	private Object[] sortFields;
	private boolean hasNext;
	private long start;
	private long budget;
	private boolean invalidated;

	private static final Logger logger = LoggerFactory.getLogger(Pit.class);

	private Pit(String id, Client client, long budget) {
		this.id = id;
		this.client = client;
		this.start = System.nanoTime();
		this.budget = budget;
		this.invalidated = false;
	}

	public static Pit allocate(Client client, String index, int keepAliveInSeconds) {
		return Pit.allocateUsingTimebudget(client, index, keepAliveInSeconds, -1);
	}

	public static Pit allocateUsingTimebudget(Client client, String index, int keepAliveInSeconds, long budget) {
		final OpenPointInTimeRequest openPointInTimeRequest = new OpenPointInTimeRequest(index)
				.keepAlive(TimeValue.timeValueSeconds(keepAliveInSeconds));
		String pitId = client.execute(OpenPointInTimeAction.INSTANCE, openPointInTimeRequest).actionGet()
				.getPointInTimeId();
		return new Pit(pitId, client, budget);
	}

	@Override
	public void close() throws Exception {
		client.execute(ClosePointInTimeAction.INSTANCE, new ClosePointInTimeRequest(id)).actionGet();
	}

	public boolean hasNext() {
		return !invalidated && hasNext;
	}

	public void consumeHit(SearchHit h) {
		hasNext = true;
		sortFields = h.getSortValues();
		if (budget != -1 && System.nanoTime() - start > budget) {
			logger.warn("Stopped processing search results as timebudget ({} ns) is exhausted", budget);
			invalidated = true;
		}
	}

	public void adaptSearch(SearchRequestBuilder searchBuilder) {
		hasNext = false;
		if (sortFields != null) {
			searchBuilder.searchAfter(sortFields);
		}
	}

}
