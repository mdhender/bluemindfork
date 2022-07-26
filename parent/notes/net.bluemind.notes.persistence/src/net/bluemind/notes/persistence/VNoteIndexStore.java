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
package net.bluemind.notes.persistence;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;

import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.network.topology.Topology;
import net.bluemind.notes.api.VNote;
import net.bluemind.notes.api.VNoteQuery;

public class VNoteIndexStore {

	private static final Logger logger = LoggerFactory.getLogger(VNoteIndexStore.class);

	public static final String VNOTE_WRITE_ALIAS = "note_write_alias";
	public static final String VNOTE_READ_ALIAS = "note_read_alias";
	public static final String VNOTE_TYPE = "vnote";

	private Client esearchClient;
	private Container container;
	private long shardId;

	public static class ItemHolder {
		public String uid;
		public String containerUid;
		public VNote value;
	}

	public VNoteIndexStore(Client esearchClient, Container container, String loc) {
		this.esearchClient = esearchClient;
		this.container = container;
		this.shardId = loc == null ? 0 : Topology.get().datalocation(loc).internalId;
	}

	public void create(Item item, VNote note) {
		store(item, note);
	}

	public void update(Item item, VNote note) {
		store(item, note);
	}

	public void delete(long id) {
		esearchClient.prepareDelete().setIndex(VNOTE_WRITE_ALIAS).setType(VNOTE_TYPE).setId(getId(id)).execute()
				.actionGet();
	}

	public void deleteAll() {
		ESearchActivator.deleteByQuery(VNOTE_WRITE_ALIAS, QueryBuilders.termQuery("containerUid", container.uid));
	}

	public ListResult<String> search(VNoteQuery query) {

		List<QueryBuilder> filters = new LinkedList<>();

		if (!Strings.nullToEmpty(query.query).trim().isEmpty()) {
			filters.add(QueryBuilders.queryStringQuery(query.escapeQuery ? escape(query.query) : query.query));
		}

		BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
		filters.add(QueryBuilders.termQuery("containerUid", container.uid));

		for (QueryBuilder f : filters) {
			boolQuery.must(f);
		}

		SearchRequestBuilder searchRequestBuilder = esearchClient.prepareSearch(VNOTE_READ_ALIAS) // index
				.setQuery(boolQuery);
		if (query.size > 0) {
			searchRequestBuilder.setFrom(query.from).setSize(query.size);
		}
		searchRequestBuilder.setFetchSource(false).addStoredField("uid"); // fetch uid

		logger.debug("vnote query {}", searchRequestBuilder);

		SearchResponse resp = searchRequestBuilder.execute().actionGet();

		logger.debug("vnote query response size {}", resp.getHits().getHits().length);

		List<String> uids = new ArrayList<>(resp.getHits().getHits().length);
		for (SearchHit hit : resp.getHits().getHits()) {
			uids.add(hit.field("uid").getValue());
		}

		ListResult<String> ret = new ListResult<>();
		ret.values = uids;
		ret.total = (int) resp.getHits().getTotalHits().value;

		return ret;
	}

	private void store(Item item, VNote note) {
		byte[] json = null;
		try {
			json = asJson(item.uid, note);
		} catch (JsonProcessingException e) {
			logger.error("error during vnote serialization", e);
			return;
		}

		esearchClient.prepareIndex(VNOTE_WRITE_ALIAS, VNOTE_TYPE).setSource(json, XContentType.JSON)
				.setId(getId(item.id)).execute().actionGet();
	}

	public void updates(List<ItemValue<VNote>> notes) {
		if (notes.isEmpty()) {
			return;
		}
		BulkRequestBuilder bulk = esearchClient.prepareBulk();

		notes.forEach(noteItem -> {
			byte[] json = null;
			try {
				json = asJson(noteItem.uid, noteItem.value);
			} catch (JsonProcessingException e) {
				logger.error("error during vnote serialization", e);
				return;
			}
			IndexRequestBuilder op = esearchClient.prepareIndex(VNOTE_WRITE_ALIAS, VNOTE_TYPE)
					.setSource(json, XContentType.JSON).setId(getId(noteItem.internalId));
			bulk.add(op);
		});
		bulk.execute().actionGet();
	}

	public void refresh() {
		esearchClient.admin().indices().prepareRefresh(VNOTE_WRITE_ALIAS).execute().actionGet();
	}

	private byte[] asJson(String uid, VNote note) throws JsonProcessingException {
		ItemHolder holder = new ItemHolder();
		holder.uid = uid;
		holder.containerUid = container.uid;
		holder.value = note;
		return JsonUtils.asBytes(holder);
	}

	/**
	 * escape the elastic-search query string. we escape all but the ":" character,
	 * since it also serves as the field:value separator
	 * 
	 * @param query
	 * @return
	 */
	private String escape(String query) {
		String alreadyEscaped = ".*?\\\\[\\[\\]+!-&|!(){}^\"~*?].*";
		if (Pattern.matches(alreadyEscaped, query)) {
			logger.warn("Escaping already escaped query {}", query);
		}
		query = query.replaceAll("\\\\:", "##");
		String regex = "([+\\-!\\(\\){}\\[\\]^\"~*?\\\\]|[&\\|]{2})";
		query = query.replaceAll(regex, "\\\\$1");
		return query.replaceAll("##", "\\\\:");
	}

	private String getId(long itemId) {
		return shardId + ":" + container.id + ":" + itemId;
	}
}
