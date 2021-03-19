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
package net.bluemind.calendar.persistence;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.ExistsQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventQuery;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.elasticsearch.Queries;
import net.bluemind.network.topology.Topology;

public class VEventIndexStore {

	private static final Logger logger = LoggerFactory.getLogger(VEventIndexStore.class);

	public static final String VEVENT_INDEX = "event";
	public static final String VEVENT_TYPE = "vevent";

	private Client esearchClient;
	private Container container;
	private long shardId;

	public static class ItemHolder {
		public String uid;
		public String containerUid;
		public List<VEvent> value;
	}

	public VEventIndexStore(Client esearchClient, Container container, String loc) {
		this.esearchClient = esearchClient;
		this.container = container;
		this.shardId = loc == null ? 0 : Topology.get().datalocation(loc).internalId;
	}

	public void create(Item item, VEventSeries event) {
		store(item, event);
	}

	public void update(Item item, VEventSeries event) {
		store(item, event);
	}

	public void delete(long id) {
		esearchClient.prepareDelete().setIndex(VEVENT_INDEX).setType(VEVENT_TYPE).setId(getId(id)).execute()
				.actionGet();
	}

	public void deleteAll() {
		ESearchActivator.deleteByQuery(VEVENT_INDEX, QueryBuilders.termQuery("containerUid", container.uid));
	}

	public ListResult<String> search(VEventQuery query) {
		return search(query, true);
	}

	public ListResult<String> search(VEventQuery query, boolean searchInPrivate) {

		List<QueryBuilder> filters = new LinkedList<>();

		List<QueryBuilder> antiFilters = new LinkedList<>();
		filters.add(QueryBuilders.termQuery("containerUid", container.uid));

		if (!searchInPrivate && !Strings.nullToEmpty(query.query).trim().isEmpty()) {
			filters.add(QueryBuilders.termQuery("value.classification", ICalendarElement.Classification.Public.name()));
		}

		if (null != query.attendee) {
			if (query.attendee.singleValueSearch()) {
				if (null != query.attendee.dir) {

					filters.add(QueryBuilders.nestedQuery("value.attendees",
							QueryBuilders.termQuery("value.attendees.dir", query.attendee.dir), ScoreMode.None));
				}
				if (null != query.attendee.partStatus) {
					filters.add(QueryBuilders.nestedQuery("value.attendees",
							QueryBuilders.termQuery("value.attendees.partStatus", query.attendee.partStatus.name()),
							ScoreMode.None));
				}
			} else {
				filters.add(QueryBuilders.nestedQuery("value.attendees", QueryBuilders.boolQuery()
						.must(QueryBuilders.termQuery("value.attendees.dir", query.attendee.dir)) //
						.must(QueryBuilders.termQuery("value.attendees.partStatus", query.attendee.partStatus.name())),
						ScoreMode.None));
			}
		}

		if (!Strings.nullToEmpty(query.query).trim().isEmpty()) {
			filters.add(QueryBuilders.queryStringQuery(query.escapeQuery ? escape(query.query) : query.query)
					.defaultOperator(Operator.AND));
		}

		if (query.dateMin != null || query.dateMax != null) {
			List<QueryBuilder> musts = new ArrayList<>(2);
			if (query.dateMin != null) {
				musts.add(fieldGreaterOrEqualAt("value.dtend.iso8601", "value.dtend.timezone", query.dateMin));
			}

			if (query.dateMax != null) {
				musts.add(fieldLessThan("value.dtstart.iso8601", "value.dtstart.timezone", query.dateMax));
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

		BoolQueryBuilder boolQ = QueryBuilders.boolQuery();
		for (QueryBuilder f : filters) {
			boolQ.must(f);
		}

		for (QueryBuilder f : antiFilters) {
			boolQ.mustNot(f);
		}

		SearchRequestBuilder searchRequestBuilder = esearchClient.prepareSearch(VEVENT_INDEX) // index
				.setQuery(boolQ) // query
				.setFrom(query.from) // from
				.setSize(query.size) // size
				.setFetchSource(false).storedFields("uid"); // fetch uid

		logger.debug("vevent query {}", searchRequestBuilder);

		SearchResponse resp = searchRequestBuilder.execute().actionGet();

		logger.debug("vevent resp {}", resp);

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

	private QueryBuilder fieldGreaterOrEqualAt(String field, String fieldTz, BmDateTime dt) {
		QueryBuilder inRangeNoTz = Queries.and(//
				Queries.missing(fieldTz), //
				QueryBuilders.rangeQuery(field).gte(new BmDateTimeWrapper(dt).format("yyyy-MM-dd'T'HH:mm:ss.S")));
		QueryBuilder inRangeWithTz = Queries.and(//
				QueryBuilders.existsQuery(fieldTz), //
				QueryBuilders.rangeQuery(field).gte(dt.iso8601));

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

	private void store(Item item, VEventSeries event) {
		Optional<byte[]> jsonValue = eventToJson(item.uid, event);
		jsonValue.ifPresent(json -> {
			esearchClient.prepareIndex(VEVENT_INDEX, VEVENT_TYPE).setSource(json, XContentType.JSON)
					.setId(getId(item.id)).execute().actionGet();
		});

	}

	public void updates(List<ItemValue<VEventSeries>> events) {
		if (events.isEmpty()) {
			return;
		}
		BulkRequestBuilder bulk = esearchClient.prepareBulk();

		events.forEach(ev -> {
			Optional<byte[]> jsonValue = eventToJson(ev.uid, ev.value);
			jsonValue.ifPresent(json -> {
				IndexRequestBuilder op = esearchClient.prepareIndex(VEVENT_INDEX, VEVENT_TYPE)
						.setSource(json, XContentType.JSON).setId(getId(ev.internalId));
				bulk.add(op);
			});
		});
		bulk.execute().actionGet();
	}

	private Optional<byte[]> eventToJson(String uid, VEventSeries ev) {
		byte[] json = null;
		try {
			json = asJson(uid, ev);
		} catch (JsonProcessingException e) {
			logger.error("error during vevent serialization", e);
			return Optional.empty();
		}
		return Optional.of(json);
	}

	public void refresh() {
		esearchClient.admin().indices().prepareRefresh(VEventIndexStore.VEVENT_INDEX).execute().actionGet();
	}

	private byte[] asJson(String uid, VEventSeries event) throws JsonProcessingException {
		ItemHolder holder = new ItemHolder();
		holder.uid = uid;
		holder.containerUid = container.uid;

		if (event.main != null && event.main.dtend == null) {
			event.main.dtend = event.main.dtstart;
		}

		Builder<VEvent> builder = ImmutableList.<VEvent>builder();
		if (event.main != null) {
			builder = builder.add(event.main);
		}
		holder.value = builder.addAll(event.occurrences).build();
		byte[] ret = JsonUtils.asBytes(holder);
		// System.err.println(new JsonObject(new String(ret)).encodePrettily());
		return ret;
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
