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
package net.bluemind.addressbook.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.PointInTimeBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCardQuery;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.elasticsearch.Pit;
import net.bluemind.network.topology.Topology;

public class VCardIndexStore {

	private static final Logger logger = LoggerFactory.getLogger(VCardIndexStore.class);

	public static final int SIZE = 200;

	public static final String VCARD_WRITE_ALIAS = "contact_write_alias";
	public static final String VCARD_READ_ALIAS = "contact_read_alias";
	public static final String VCARD_TYPE = "vcard";

	private static final Pattern alreadyEscapedRegex = Pattern.compile(".*?\\\\[\\[\\]+!-&|!(){}^\"~*?].*");
	private static final Pattern escapeRegex = Pattern.compile("([+\\-!\\(\\){}\\[\\]^\"~*?\\\\]|[&\\|]{2})");

	private Client esearchClient;

	private Container container;
	private long shardId;

	public static class ItemHolder {
		public String uid;
		public String containerUid;
		public String displayName;
		public VCard value;
		public String sortName;
	}

	public VCardIndexStore(Client esearchClient, Container container, String loc) {
		this.esearchClient = esearchClient;
		this.container = container;
		this.shardId = loc == null ? 0 : Topology.get().datalocation(loc).internalId;
	}

	public void create(Item item, VCard card) {
		byte[] json = null;
		try {
			json = asJson(item.uid, card);
		} catch (JsonProcessingException e) {
			logger.error("error during vcard serialization to json before indexation", e);
			return;
		}
		esearchClient.prepareIndex(VCARD_WRITE_ALIAS, VCARD_TYPE).setSource(json, XContentType.JSON)
				.setId(getId(item.id)).execute().actionGet();

	}

	private byte[] asJson(String uid, VCard card) throws JsonProcessingException {
		ItemHolder holder = new ItemHolder();
		holder.uid = uid;
		holder.containerUid = container.uid;
		holder.displayName = card.identification.formatedName.value;
		holder.sortName = holder.displayName;
		holder.value = card;
		return JsonUtils.asBytes(holder);
	}

	public void update(Item item, VCard value) {
		byte[] json = null;
		try {
			json = asJson(item.uid, value);
		} catch (JsonProcessingException e) {
			logger.error("error during vcard serialization to json before indexation", e);
			return;
		}
		esearchClient.prepareIndex(VCARD_WRITE_ALIAS, VCARD_TYPE).setSource(json, XContentType.JSON)
				.setId(getId(item.id)).execute().actionGet();
	}

	public void updates(List<ItemValue<VCard>> cards) {
		if (cards.isEmpty()) {
			return;
		}
		BulkRequestBuilder bulk = esearchClient.prepareBulk();

		cards.forEach(card -> {
			byte[] json = null;
			try {
				json = asJson(card.uid, card.value);
			} catch (JsonProcessingException e) {
				logger.error("error during vcard serialization to json before indexation", e);
				return;
			}
			IndexRequestBuilder op = esearchClient.prepareIndex(VCARD_WRITE_ALIAS, VCARD_TYPE)
					.setSource(json, XContentType.JSON).setId(getId(card.internalId));
			bulk.add(op);
		});
		bulk.execute().actionGet();
	}

	public void delete(String uid) {
		ESearchActivator.deleteByQuery(VCARD_WRITE_ALIAS,
				QueryBuilders.boolQuery().must(QueryBuilders.termQuery("containerUid", container.uid))
						.must(QueryBuilders.termQuery("uid", uid)));
	}

	public void deleteAll() {
		ESearchActivator.deleteByQuery(VCARD_WRITE_ALIAS,
				QueryBuilders.boolQuery().must(QueryBuilders.termQuery("containerUid", container.uid)));
	}

	public ListResult<String> search(VCardQuery query) throws Exception {
		QueryBuilder queryString = null;
		if (Strings.isNullOrEmpty(query.query)) {
			queryString = QueryBuilders.matchAllQuery();
		} else {
			String escapedQuery = query.escapeQuery ? escape(query.query) : query.query;
			queryString = QueryBuilders.queryStringQuery(escapedQuery).defaultOperator(Operator.AND);
		}

		QueryBuilder qb = QueryBuilders.boolQuery().must(queryString)
				.must(QueryBuilders.termQuery("containerUid", container.uid));
		logger.debug("vcard query {}", qb);

		String[] sourceIncludeFields = { "uid" };
		SearchRequestBuilder searchBuilder = ESearchActivator.getClient().prepareSearch(VCARD_READ_ALIAS);
		searchBuilder.setQuery(qb);
		searchBuilder.setFetchSource(sourceIncludeFields, null);
		searchBuilder.setTrackTotalHits(false);

		if (query.from + query.size > 10000) {
			return paginatedSearch(searchBuilder, query);
		} else {
			return simpleSearch(searchBuilder, query);
		}
	}

	private ListResult<String> simpleSearch(SearchRequestBuilder searchBuilder, VCardQuery query) {
		SortBuilder<?> sort = null;
		if (query.orderBy == null || query.orderBy == VCardQuery.OrderBy.FormatedName) {
			sort = SortBuilders.fieldSort("sortName");
		} else {
			sort = SortBuilders.scoreSort();
		}
		searchBuilder.addSort(sort);

		if (query.size > 0) {
			searchBuilder.setFrom(query.from).setSize(query.size);
		}

		logger.debug("{}", searchBuilder);
		SearchResponse sr = searchBuilder.execute().actionGet();
		logger.debug("{}", sr);
		SearchHits searchHits = sr.getHits();

		List<String> uids = new ArrayList<>();
		for (SearchHit h : searchHits.getHits()) {
			Map<String, Object> source = h.getSourceAsMap();
			uids.add((String) source.get("uid"));
		}

		ListResult<String> ret = new ListResult<>();
		ret.values = uids;
		ret.total = uids.size();
		return ret;
	}

	private ListResult<String> paginatedSearch(SearchRequestBuilder searchBuilder, VCardQuery query) throws Exception {
		searchBuilder.setSize(1000);
		searchBuilder.setTrackTotalHits(false);
		SortBuilder<?> sort = null;
		if (query.orderBy == null || query.orderBy == VCardQuery.OrderBy.FormatedName) {
			sort = SortBuilders.fieldSort("sortName");
		} else {
			sort = SortBuilders.fieldSort("_shard_doc").order(SortOrder.ASC);
		}
		searchBuilder.addSort(sort);

		Client client = ESearchActivator.getClient();
		List<String> uidsList = new ArrayList<>();
		int position = 0;
		Predicate<List<String>> continueSearch = uids -> query.size == -1
				|| (query.size > 0 && uids.size() < query.size);
		Predicate<Integer> hitInRange = pos -> (query.from > 0 && pos >= query.from)
				&& (query.size == -1 || (query.size > 0 && pos < (query.from + query.size)));

		try (Pit pit = Pit.allocate(client, VCARD_READ_ALIAS, 60)) {
			do {
				searchBuilder.setPointInTime(new PointInTimeBuilder(pit.id));
				pit.adaptSearch(searchBuilder);
				logger.debug("{}", searchBuilder);
				SearchResponse sr = searchBuilder.execute().actionGet();
				logger.debug("{}", sr);
				SearchHits searchHits = sr.getHits();

				if (searchHits != null && searchHits.getHits() != null) {
					for (SearchHit h : searchHits.getHits()) {
						Map<String, Object> source = h.getSourceAsMap();
						if (query.from == 0 || hitInRange.test(position)) {
							uidsList.add((String) source.get("uid"));
						}
						pit.consumeHit(h);
						position++;
					}
				}
			} while (pit.hasNext() && continueSearch.test(uidsList));
		}

		ListResult<String> ret = new ListResult<>();
		ret.values = uidsList;
		ret.total = uidsList.size();
		return ret;
	}

	/**
	 * escape the elastic-search query string. we escape all but the ":" character,
	 * since it also serves as the field:value separator
	 * 
	 * @param query
	 * @return
	 */
	String escape(String query) {
		if (alreadyEscapedRegex.matcher(query).matches()) {
			logger.warn("Escaping already escaped query {}", query);
		}
		query = query.replace("\\:", "##");
		query = escapeRegex.matcher(query).replaceAll("\\\\$1");
		return query.replace("##", "\\:");
	}

	public void refresh() {
		esearchClient.admin().indices().prepareRefresh(VCARD_READ_ALIAS).execute().actionGet();
	}

	private String getId(long itemId) {
		return shardId + ":" + container.id + ":" + itemId;
	}
}
