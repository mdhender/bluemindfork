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
package net.bluemind.todolist.persistence;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.ExistsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;

import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.elasticsearch.Queries;
import net.bluemind.todolist.api.VTodo;
import net.bluemind.todolist.api.VTodoQuery;

public class VTodoIndexStore {

	private static final Logger logger = LoggerFactory.getLogger(VTodoIndexStore.class);

	public static final String VTODO_INDEX = "todo";
	public static final String VTODO_TYPE = "vtodo";

	private Client esearchClient;
	private Container container;

	public static class ItemHolder {
		public String uid;
		public String containerUid;
		public VTodo value;
	}

	public VTodoIndexStore(Client esearchClient, Container container) {
		this.esearchClient = esearchClient;
		this.container = container;
	}

	public void create(String uid, VTodo todo) {
		store(uid, todo);
	}

	public void update(String uid, VTodo todo) {
		store(uid, todo);
	}

	public void delete(String uid) {
		esearchClient.prepareDelete().setIndex(VTODO_INDEX).setType(VTODO_TYPE).setId(container.uid + ":" + uid)
				.execute().actionGet();
	}

	public void deleteAll() {
		ESearchActivator.deleteByQuery(VTODO_INDEX, QueryBuilders.termQuery("containerUid", container.uid));
	}

	public ListResult<String> search(VTodoQuery query) {

		List<QueryBuilder> filters = new LinkedList<>();

		if (query.todoUid != null) {
			filters.add(QueryBuilders.termQuery("value.uid", query.todoUid));
		}
		if (!Strings.nullToEmpty(query.query).trim().isEmpty()) {
			filters.add(QueryBuilders.queryStringQuery(query.escapeQuery ? escape(query.query) : query.query));
		}

		if (query.dateMin != null || query.dateMax != null) {
			List<QueryBuilder> musts = new ArrayList<>(2);
			if (query.dateMin != null) {
				musts.add(fieldGreaterThan("value.due.iso8601", "value.due.timezone", query.dateMin));
			}

			if (query.dateMax != null) {
				musts.add(fieldLessThan("value.due.iso8601", "value.due.timezone", query.dateMax));
			}

			// has rrule without end or rrule.until in range
			BmDateTime until = (query.dateMin != null) ? query.dateMin : query.dateMax;
			ExistsQueryBuilder isRecurring = QueryBuilders.existsQuery("value.rrule");
			QueryBuilder noEndDate = Queries.missing("value.rrule.until.iso8601");
			QueryBuilder recurEndMatch = fieldGreaterThan("value.rrule.until.iso8601", "value.rrule.until.timezone",
					until);
			QueryBuilder inRangeWhenReccuring = Queries.and(isRecurring, Queries.or(noEndDate, recurEndMatch));

			// build the global date range filter
			QueryBuilder dateRangeFilter = Queries.or(Queries.and(musts), inRangeWhenReccuring);
			filters.add(dateRangeFilter);
		}

		BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
		filters.add(QueryBuilders.termQuery("containerUid", container.uid));

		for (QueryBuilder f : filters) {
			boolQuery.must(f);
		}

		SearchRequestBuilder searchRequestBuilder = esearchClient.prepareSearch(VTODO_INDEX) // index
				.setQuery(boolQuery);
		if (query.size > 0) {
			searchRequestBuilder.setFrom(query.from).setSize(query.size);
		}
		searchRequestBuilder.setFetchSource(false).addStoredField("uid"); // fetch uid

		logger.debug("vtodo query {}", searchRequestBuilder);

		SearchResponse resp = searchRequestBuilder.execute().actionGet();

		logger.debug("vtodo query response size {}", resp.getHits().getHits().length);

		List<String> uids = new ArrayList<>(resp.getHits().getHits().length);
		for (SearchHit hit : resp.getHits().getHits()) {
			uids.add(hit.field("uid").getValue());
		}

		ListResult<String> ret = new ListResult<>();
		ret.values = uids;
		ret.total = (int) resp.getHits().getTotalHits();

		return ret;
	}

	private QueryBuilder fieldGreaterThan(String field, String fieldTz, BmDateTime dt) {
		QueryBuilder inRangeNoTz = Queries.and(//
				Queries.missing(fieldTz), //
				QueryBuilders.rangeQuery(field).gt(new BmDateTimeWrapper(dt).format("yyyy-MM-dd'T'HH:mm:ss.S")));
		QueryBuilder inRangeWithTz = Queries.and(//
				QueryBuilders.existsQuery(fieldTz), //
				QueryBuilders.rangeQuery(field).gt(dt.iso8601));

		return Queries.and(QueryBuilders.existsQuery(field), Queries.or(inRangeNoTz, inRangeWithTz));
	}

	private QueryBuilder fieldLessThan(String field, String fieldTz, BmDateTime dt) {
		QueryBuilder inRangeNoTz = Queries.and(//
				Queries.missing(fieldTz), //
				QueryBuilders.rangeQuery(field).lt(new BmDateTimeWrapper(dt).format("yyyy-MM-dd'T'HH:mm:ss.S")));
		QueryBuilder inRangeWithTz = Queries.and(//
				QueryBuilders.existsQuery(fieldTz), //
				QueryBuilders.rangeQuery(field).lt(dt.iso8601));

		return Queries.and(QueryBuilders.existsQuery(field), Queries.or(inRangeNoTz, inRangeWithTz));
	}

	private void store(String uid, VTodo todo) {
		byte[] json = null;
		try {
			json = asJson(uid, todo);
		} catch (JsonProcessingException e) {
			logger.error("error during vtodo serialization", e);
			return;
		}

		String id = container.uid + ":" + uid;
		esearchClient.prepareIndex(VTODO_INDEX, VTODO_TYPE).setSource(json, XContentType.JSON).setId(id).execute()
				.actionGet();

	}

	public void updates(List<ItemValue<VTodo>> tasks) {
		BulkRequestBuilder bulk = esearchClient.prepareBulk();

		tasks.forEach(task -> {
			byte[] json = null;
			try {
				json = asJson(task.uid, task.value);
			} catch (JsonProcessingException e) {
				logger.error("error during vtodo serialization", e);
				return;
			}
			String id = container.uid + ":" + task.uid;
			IndexRequestBuilder op = esearchClient.prepareIndex(VTODO_INDEX, VTODO_TYPE)
					.setSource(json, XContentType.JSON).setId(id);
			bulk.add(op);
		});
		bulk.execute().actionGet();
	}

	public void refresh() {
		esearchClient.admin().indices().prepareRefresh(VTodoIndexStore.VTODO_INDEX).execute().actionGet();
	}

	private byte[] asJson(String uid, VTodo todo) throws JsonProcessingException {
		ItemHolder holder = new ItemHolder();
		holder.uid = uid;
		holder.containerUid = container.uid;
		holder.value = todo;
		return JsonUtils.asBytes(holder);
	}

	/**
	 * escape the elastic-search query string. we escape all but the ":" character,
	 * since it also serves as the field:value separator
	 * 
	 * @param query
	 * @return
	 */
	String escape(String query) {
		String alreadyEscaped = ".*?\\\\[\\[\\]+!-&|!(){}^\"~*?].*";
		if (Pattern.matches(alreadyEscaped, query)) {
			logger.warn("Escaping already escaped query {}", query);
		}
		query = query.replaceAll("\\\\:", "##");
		String regex = "([+\\-!\\(\\){}\\[\\]^\"~*?\\\\]|[&\\|]{2})";
		query = query.replaceAll(regex, "\\\\$1");
		return query.replaceAll("##", "\\\\:");
	}
}
