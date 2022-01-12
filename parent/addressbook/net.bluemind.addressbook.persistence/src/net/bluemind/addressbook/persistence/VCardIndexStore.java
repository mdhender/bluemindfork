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
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
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
import net.bluemind.network.topology.Topology;

public class VCardIndexStore {

	private static final Logger logger = LoggerFactory.getLogger(VCardIndexStore.class);

	public static final String VCARD_INDEX = "contact";
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
		esearchClient.prepareIndex(VCARD_INDEX, VCARD_TYPE).setSource(json, XContentType.JSON).setId(getId(item.id))
				.execute().actionGet();

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
		esearchClient.prepareIndex(VCARD_INDEX, VCARD_TYPE).setSource(json, XContentType.JSON).setId(getId(item.id))
				.execute().actionGet();
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
			IndexRequestBuilder op = esearchClient.prepareIndex(VCARD_INDEX, VCARD_TYPE)
					.setSource(json, XContentType.JSON).setId(getId(card.internalId));
			bulk.add(op);
		});
		bulk.execute().actionGet();
	}

	public void delete(String uid) {
		ESearchActivator.deleteByQuery(VCARD_INDEX,
				QueryBuilders.boolQuery().must(QueryBuilders.termQuery("containerUid", container.uid))
						.must(QueryBuilders.termQuery("uid", uid)));
	}

	public void deleteAll() {
		ESearchActivator.deleteByQuery(VCARD_INDEX,
				QueryBuilders.boolQuery().must(QueryBuilders.termQuery("containerUid", container.uid)));
	}

	public ListResult<String> search(VCardQuery query) {

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

		SortBuilder<?> sort = null;
		if (query.orderBy == null || query.orderBy == VCardQuery.OrderBy.FormatedName) {
			sort = SortBuilders.fieldSort("sortName");
		} else {
			sort = SortBuilders.scoreSort();
		}
		SearchRequestBuilder preparedSearch = esearchClient.prepareSearch(VCARD_INDEX).setTypes(VCARD_TYPE)
				.setQuery(qb);
		if (query.size > 0) {
			preparedSearch = preparedSearch.setFrom(query.from).setSize(query.size);
		}
		preparedSearch = preparedSearch.addSort(sort).setFetchSource(false).storedFields("uid");
		SearchResponse resp = preparedSearch.execute().actionGet();

		List<String> uids = new ArrayList<>(resp.getHits().getHits().length);
		for (SearchHit hit : resp.getHits().getHits()) {
			uids.add(hit.field("uid").getValue());
		}

		ListResult<String> ret = new ListResult<>();
		ret.values = uids;
		ret.total = (int) resp.getHits().getTotalHits().value;
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
		esearchClient.admin().indices().prepareRefresh(VCARD_INDEX).execute().actionGet();
	}

	private String getId(long itemId) {
		return shardId + ":" + container.id + ":" + itemId;
	}
}
