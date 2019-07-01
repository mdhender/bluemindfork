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

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BulkData {
	public String indexName;
	public String type;
	public QueryBuilder query;
	public SortBuilder<?> order;
	public boolean fetchSource;
	public String[] fields;
	public UnitIndex unitIndex;
	public UnitUpdate unitUpdate;
	public UnitDelete unitDelete;
	private final Client client;
	private static final Logger logger = LoggerFactory.getLogger(BulkData.class);

	public BulkData(Client client) {
		this.client = client;
	}

	public long execute() {
		logger.info("Bulk START on index {}", indexName);

		SearchRequestBuilder searchBuilder = client.prepareSearch(indexName).setTypes(type)
				.setSize(MailIndexService.SIZE).setQuery(query);

		if (fields != null) {
			searchBuilder.storedFields(fields);
		}

		searchBuilder.setFetchSource(fetchSource);

		if (order != null) {
			searchBuilder.addSort(order);
		}

		logger.debug(searchBuilder.toString());

		searchBuilder.setScroll(TimeValue.timeValueSeconds(20));
		SearchResponse r = searchBuilder.execute().actionGet();

		long current = 0;
		long totalHits = r.getHits().getTotalHits();
		logger.debug(" ***** Got {} total hits", totalHits);
		while (current < totalHits) {
			BulkRequestBuilder bulk = client.prepareBulk();
			bulk.setRefreshPolicy(RefreshPolicy.IMMEDIATE);

			SearchHit[] hits = r.getHits().getHits();
			for (SearchHit h : hits) {
				logger.debug("Handling {} {}", h.getType(), h.getId());

				if (unitIndex != null) {
					IndexRequestBuilder irb = unitIndex.build(client, h);
					if (irb != null) {
						bulk.add(irb);
					}
				}
				if (unitUpdate != null) {
					UpdateRequestBuilder urb = unitUpdate.build(client, h);
					if (urb != null) {
						bulk.add(urb);
					}
				}

				if (unitDelete != null) {
					DeleteRequestBuilder drb = unitDelete.build(client, h);
					if (drb != null) {
						bulk.add(drb);
					}
				}
				current++;
			}
			BulkResponse br = bulk.execute().actionGet();
			logger.debug("  bulk chunk: {}", br.getTook());

			if (current < totalHits) {
				r = client.prepareSearchScroll(r.getScrollId()).setScroll(TimeValue.timeValueSeconds(20)).execute()
						.actionGet();

			}

		}
		logger.info("Bulk END ({})", current);

		return current;
	}

	public static interface UnitIndex {
		public IndexRequestBuilder build(Client client, SearchHit hit);
	}

	public static interface UnitUpdate {
		public UpdateRequestBuilder build(Client client, SearchHit hit);
	}

	public static interface UnitDelete {
		public DeleteRequestBuilder build(Client client, SearchHit hit);
	}

	public static class DeleteUnitHelper implements UnitDelete {

		private String indexName;

		public DeleteUnitHelper(String indexName) {
			this.indexName = indexName;
		}

		@Override
		public DeleteRequestBuilder build(Client client, SearchHit hit) {
			DeleteRequestBuilder drb = client.prepareDelete().setIndex(indexName);
			drb.setType(MailIndexService.MAILSPOOL_TYPE);

			drb.setId(hit.getId());
			String pid = hit.getFields().get("parentId").getValue();
			drb.setParent(pid);

			return drb;
		}
	};

}