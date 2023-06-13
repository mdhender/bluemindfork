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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventQuery;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.lib.elasticsearch.EsBulk;
import net.bluemind.lib.elasticsearch.Pit;
import net.bluemind.lib.elasticsearch.Pit.PaginableSearchQueryBuilder;
import net.bluemind.lib.elasticsearch.Pit.PaginationParams;
import net.bluemind.lib.elasticsearch.Queries;
import net.bluemind.lib.elasticsearch.exception.ElasticDocumentException;
import net.bluemind.network.topology.Topology;

public class VEventIndexStore {

	private static final Logger logger = LoggerFactory.getLogger(VEventIndexStore.class);

	public static final int SIZE = 200;

	public static final String VEVENT_WRITE_ALIAS = "event_write_alias";
	public static final String VEVENT_READ_ALIAS = "event_read_alias";

	private final ElasticsearchClient esClient;
	private final Container container;
	private final long shardId;
	private final Query containerQuery;

	public VEventIndexStore(ElasticsearchClient esClient, Container container, String loc) {
		this.esClient = esClient;
		this.container = container;
		this.shardId = loc == null ? 0 : Topology.get().datalocation(loc).internalId;
		this.containerQuery = TermQuery.of(t -> t.field("containerUid").value(container.uid))._toQuery();
	}

	public static class IndexableVEventSeries {
		public String uid;
		public String containerUid;
		public List<VEvent> value;
	}

	private IndexableVEventSeries asIndexable(String uid, VEventSeries event) {
		IndexableVEventSeries indexable = new IndexableVEventSeries();
		indexable.uid = uid;
		indexable.containerUid = container.uid;

		if (event.main != null && event.main.dtend == null) {
			event.main.dtend = event.main.dtstart;
		}

		Builder<VEvent> builder = ImmutableList.<VEvent>builder();
		if (event.main != null) {
			builder = builder.add(event.main);
		}
		indexable.value = builder.addAll(event.occurrences).build();
		return indexable;
	}

	public void create(Item item, VEventSeries event) {
		store(item, event);
	}

	public void update(Item item, VEventSeries event) {
		store(item, event);
	}

	private void store(Item item, VEventSeries event) {
		IndexableVEventSeries toIndex = asIndexable(item.uid, event);
		try {
			esClient.index(i -> i.index(VEVENT_WRITE_ALIAS).id(getId(item.id)).document(toIndex));
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticDocumentException(VEVENT_WRITE_ALIAS, e);
		}
	}

	public void updates(List<ItemValue<VEventSeries>> events) {
		if (events.isEmpty()) {
			return;
		}

		new EsBulk(esClient).commitAll(events, (event, b) -> b.index(idx -> idx //
				.index(VEVENT_WRITE_ALIAS) //
				.id(getId(event.internalId)) //
				.document(asIndexable(event.uid, event.value))));
	}

	public void delete(long id) {
		try {
			esClient.delete(d -> d.index(VEVENT_WRITE_ALIAS).id(getId(id)));
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticDocumentException(VEVENT_WRITE_ALIAS, e);
		}
	}

	public void deleteAll() {
		Query toDelete = QueryBuilders.bool(b -> b.must(containerQuery));
		try {
			esClient.deleteByQuery(d -> d.index(VEVENT_WRITE_ALIAS).query(toDelete));
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticDocumentException(VEVENT_WRITE_ALIAS, e);
		}
	}

	public ListResult<String> search(VEventQuery query) {
		return search(query, true);
	}

	public ListResult<String> search(VEventQuery query, boolean searchInPrivate) {
		List<Query> queries = new LinkedList<>();
		queries.add(containerQuery);

		if (!Strings.nullToEmpty(query.query).trim().isEmpty()) {
			queries.add(QueryStringQuery.of(q -> q.query(escape(query)).defaultOperator(Operator.And))._toQuery());
		}

		if (!searchInPrivate && !Strings.nullToEmpty(query.query).trim().isEmpty()) {
			queries.add(TermQuery
					.of(t -> t.field("value.classification").value(ICalendarElement.Classification.Public.name()))
					._toQuery());
		}

		if (null != query.attendee) {
			queries.add(NestedQuery.of(n -> n.path("value.attendees").scoreMode(ChildScoreMode.None) //
					.query(q -> q.bool(bq -> {
						if (null != query.attendee.dir) {
							bq.must(m -> m.term(t -> t //
									.field("value.attendees.dir") //
									.value(query.attendee.dir)));
						}
						if (null != query.attendee.partStatus) {
							bq.must(m -> m.term(t -> t //
									.field("value.attendees.partStatus") //
									.value(query.attendee.partStatus.name())));
						}
						return bq;
					})))._toQuery());
		}

		if (query.dateMin != null || query.dateMax != null) {
			List<Query> between = new ArrayList<>(2);
			if (query.dateMin != null) {
				between.add(Queries.BmDateRange.gte("value.dtend", query.dateMin));
			}

			if (query.dateMax != null) {
				between.add(Queries.BmDateRange.lt("value.dtstart", query.dateMax));
			}

			BmDateTime until = (query.dateMin != null) ? query.dateMin : query.dateMax;
			Query dateQuery = BoolQuery.of(b -> b //
					.should(s -> s.bool(b1 -> b1.must(between))) //
					.should(s -> s.bool(b1 -> b1 //
							.must(m -> m.exists(e -> e.field("value.rrule"))) //
							.must(m -> m.bool(b2 -> b2 //
									.should(sn -> sn.bool(b3 -> b3
											.mustNot(m2 -> m2.exists(e -> e.field("value.rrule.until.iso8601")))))
									.should(Queries.BmDateRange.gte("value.rrule.until", until)))))))
					._toQuery();

			queries.add(dateQuery);
		}

		Query searchQuery = QueryBuilders.bool(b -> b.must(queries));
		PaginableSearchQueryBuilder paginableSearch = s -> s //
				.query(searchQuery) //
				.source(src -> src.fetch(false)) //
				.storedFields("uid");

		try {
			return (query.from + query.size > 10000) //
					? paginatedSearch(paginableSearch, query) //
					: simpleSearch(paginableSearch, query);
		} catch (ElasticsearchException | IOException e) {
			logger.error("search failed", e);
			throw new ElasticDocumentException(VEVENT_READ_ALIAS, e);
		}
	}

	private ListResult<String> simpleSearch(PaginableSearchQueryBuilder paginableSearch, VEventQuery query)
			throws ElasticsearchException, IOException {
		SearchResponse<Void> response = esClient.search(paginableSearch //
				.andThen(s -> s.index(VEVENT_READ_ALIAS)) //
				.andThen(s -> (query.size > 0) ? s.from(query.from).size(query.size) : s), Void.class);

		List<String> uids = response.hits().hits().stream()
				.map(hit -> hit.fields().get("uid").toJson().asJsonArray().getString(0)).toList();
		return ListResult.create(uids, uids.size());
	}

	private ListResult<String> paginatedSearch(PaginableSearchQueryBuilder paginableSearch, VEventQuery query)
			throws ElasticsearchException, IOException {
		SortOptions sort = SortOptions.of(s -> s.field(f -> f.field("_shard_doc").order(SortOrder.Asc)));
		try (Pit<Void> pit = Pit.allocate(esClient, VEVENT_READ_ALIAS, 60, Void.class)) {
			List<String> uids = pit.allPages(paginableSearch, new PaginationParams(query.from, query.size, sort),
					hit -> hit.fields().get("uid").toJson().asJsonArray().getString(0));
			return ListResult.create(uids, uids.size());
		}
	}

	public void refresh() {
		try {
			esClient.indices().refresh(r -> r.index(VEVENT_WRITE_ALIAS));
		} catch (ElasticsearchException | IOException e) {
			logger.error("[es][vevent][{}] Unable to refresh {}, search results may be stale", container.uid,
					VEVENT_WRITE_ALIAS, e);
		}
	}

	/**
	 * escape the elastic-search query string. we escape all but the ":" character,
	 * since it also serves as the field:value separator
	 * 
	 * @param query
	 * @return
	 */
	String escape(VEventQuery query) {
		return query.escapeQuery ? Queries.escape(query.query) : query.query;
	}

	private String getId(long itemId) {
		return shardId + ":" + container.id + ":" + itemId;
	}
}
