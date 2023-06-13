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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.lib.elasticsearch.EsBulk;
import net.bluemind.lib.elasticsearch.Queries;
import net.bluemind.lib.elasticsearch.exception.ElasticDocumentException;
import net.bluemind.network.topology.Topology;
import net.bluemind.todolist.api.VTodo;
import net.bluemind.todolist.api.VTodoQuery;

public class VTodoIndexStore {

	private static final Logger logger = LoggerFactory.getLogger(VTodoIndexStore.class);

	public static final String VTODO_WRITE_ALIAS = "todo_write_alias";
	public static final String VTODO_READ_ALIAS = "todo_read_alias";

	private final ElasticsearchClient esClient;
	private final Container container;
	private final long shardId;
	private final Query containerQuery;

	public VTodoIndexStore(ElasticsearchClient esClient, Container container, String loc) {
		this.esClient = esClient;
		this.container = container;
		this.shardId = loc == null ? 0 : Topology.get().datalocation(loc).internalId;
		this.containerQuery = TermQuery.of(t -> t.field("containerUid").value(container.uid))._toQuery();
	}

	public static class IndexableVTodo {
		public String uid;
		public String containerUid;
		public VTodo value;
	}

	private IndexableVTodo asIndexable(String uid, VTodo todo) {
		IndexableVTodo indexable = new IndexableVTodo();
		indexable.uid = uid;
		indexable.containerUid = container.uid;
		indexable.value = todo;
		return indexable;
	}

	public void create(Item item, VTodo todo) {
		store(item, todo);
	}

	public void update(Item item, VTodo todo) {
		store(item, todo);
	}

	private void store(Item item, VTodo todo) {
		IndexableVTodo toIndex = asIndexable(item.uid, todo);
		try {
			esClient.index(i -> i.index(VTODO_WRITE_ALIAS).id(getId(item.id)).document(toIndex));
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticDocumentException(VTODO_WRITE_ALIAS, e);
		}
	}

	public void updates(List<ItemValue<VTodo>> tasks) {
		if (tasks.isEmpty()) {
			return;
		}

		new EsBulk(esClient).commitAll(tasks, (task, b) -> b.index(idx -> idx //
				.index(VTODO_WRITE_ALIAS) //
				.id(getId(task.internalId)) //
				.document(asIndexable(task.uid, task.value))));
	}

	public void delete(long id) {
		try {
			esClient.delete(d -> d.index(VTODO_WRITE_ALIAS).id(getId(id)));
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticDocumentException(VTODO_WRITE_ALIAS, e);
		}
	}

	public void deleteAll() {
		Query toDelete = QueryBuilders.bool(b -> b.must(containerQuery));
		try {
			esClient.deleteByQuery(d -> d.index(VTODO_WRITE_ALIAS).query(toDelete));
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticDocumentException(VTODO_WRITE_ALIAS, e);
		}
	}

	public ListResult<String> search(VTodoQuery query) {
		List<Query> queries = new LinkedList<>();
		queries.add(containerQuery);

		if (query.todoUid != null) {
			queries.add(TermQuery.of(t -> t.field("value.uid").value(query.todoUid))._toQuery());
		}
		if (!Strings.nullToEmpty(query.query).trim().isEmpty()) {
			queries.add(QueryStringQuery.of(q -> q.query(escape(query)).defaultOperator(Operator.And))._toQuery());
		}

		if (query.dateMin != null || query.dateMax != null) {
			List<Query> between = new ArrayList<>(2);
			if (query.dateMin != null) {
				between.add(Queries.BmDateRange.gt("value.due", query.dateMin));
			}

			if (query.dateMax != null) {
				between.add(Queries.BmDateRange.lt("value.due", query.dateMax));
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
		try {
			SearchResponse<Void> response = esClient.search(s -> {
				s.index(VTODO_READ_ALIAS) //
						.query(searchQuery) //
						.source(src -> src.fetch(false)) //
						.storedFields("uid");
				return (query.size > 0) ? s.from(query.from).size(query.size) : s;
			}, Void.class);

			List<String> uids = response.hits().hits().stream()
					.map(hit -> hit.fields().get("uid").toJson().asJsonArray().getString(0)).toList();
			return ListResult.create(uids, response.hits().total().value());
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticDocumentException(VTODO_READ_ALIAS, e);
		}
	}

	public void refresh() {
		try {
			esClient.indices().refresh(r -> r.index(VTODO_WRITE_ALIAS));
		} catch (ElasticsearchException | IOException e) {
			logger.error("[es][vtodo][{}] Unable to refresh {}, search results may be stale", container.uid,
					VTODO_WRITE_ALIAS, e);
		}
	}

	public static String escape(VTodoQuery query) {
		return query.escapeQuery ? Queries.escape(query.query) : query.query;
	}

	private String getId(long itemId) {
		return shardId + ":" + container.id + ":" + itemId;
	}
}
