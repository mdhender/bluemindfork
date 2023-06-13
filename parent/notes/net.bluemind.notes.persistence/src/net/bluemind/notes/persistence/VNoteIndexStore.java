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

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.lib.elasticsearch.EsBulk;
import net.bluemind.lib.elasticsearch.Queries;
import net.bluemind.lib.elasticsearch.exception.ElasticDocumentException;
import net.bluemind.network.topology.Topology;
import net.bluemind.notes.api.VNote;
import net.bluemind.notes.api.VNoteQuery;

public class VNoteIndexStore {

	private static final Logger logger = LoggerFactory.getLogger(VNoteIndexStore.class);

	public static final String VNOTE_WRITE_ALIAS = "note_write_alias";
	public static final String VNOTE_READ_ALIAS = "note_read_alias";

	private final ElasticsearchClient esClient;
	private final Container container;
	private final long shardId;
	private final Query containerQuery;

	public VNoteIndexStore(ElasticsearchClient esearchClient, Container container, String loc) {
		this.esClient = esearchClient;
		this.container = container;
		this.shardId = loc == null ? 0 : Topology.get().datalocation(loc).internalId;
		this.containerQuery = TermQuery.of(t -> t.field("containerUid").value(container.uid))._toQuery();
	}

	public static class IndexableVNote {
		public String uid;
		public String containerUid;
		public VNote value;
	}

	private IndexableVNote asIndexable(String uid, VNote note) {
		IndexableVNote indexable = new IndexableVNote();
		indexable.uid = uid;
		indexable.containerUid = container.uid;
		indexable.value = note;
		return indexable;
	}

	public void create(Item item, VNote note) {
		store(item, note);
	}

	public void update(Item item, VNote note) {
		store(item, note);
	}

	private void store(Item item, VNote note) {
		IndexableVNote toIndex = asIndexable(item.uid, note);
		try {
			esClient.index(i -> i.index(VNOTE_WRITE_ALIAS).id(getId(item.id)).document(toIndex));
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticDocumentException(VNOTE_WRITE_ALIAS, e);
		}
	}

	public void updates(List<ItemValue<VNote>> notes) {
		if (notes.isEmpty()) {
			return;
		}

		new EsBulk(esClient).commitAll(notes, (note, b) -> b.index(idx -> idx //
				.index(VNOTE_WRITE_ALIAS) //
				.id(getId(note.internalId)) //
				.document(asIndexable(note.uid, note.value))));
	}

	public void delete(long id) {
		try {
			esClient.delete(d -> d.index(VNOTE_WRITE_ALIAS).id(getId(id)));
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticDocumentException(VNOTE_WRITE_ALIAS, e);
		}
	}

	public void deleteAll() {
		Query toDelete = QueryBuilders.bool(b -> b.must(containerQuery));
		try {
			esClient.deleteByQuery(d -> d.index(VNOTE_WRITE_ALIAS).query(toDelete));
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticDocumentException(VNOTE_WRITE_ALIAS, e);
		}
	}

	public ListResult<String> search(VNoteQuery query) {
		List<Query> queries = new LinkedList<>();
		queries.add(containerQuery);
		if (!Strings.nullToEmpty(query.query).trim().isEmpty()) {
			queries.add(QueryStringQuery.of(q -> q.query(escape(query)).defaultOperator(Operator.And))._toQuery());
		}
		Query searchQuery = QueryBuilders.bool(b -> b.must(queries));
		try {
			SearchResponse<Void> response = esClient.search(s -> {
				s.index(VNOTE_READ_ALIAS) //
						.query(searchQuery) //
						.source(src -> src.fetch(false)) //
						.storedFields("uid");
				return (query.size > 0) ? s.from(query.from).size(query.size) : s;
			}, Void.class);

			List<String> uids = response.hits().hits().stream()
					.map(hit -> hit.fields().get("uid").toJson().asJsonArray().getString(0)).toList();
			return ListResult.create(uids, response.hits().total().value());
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticDocumentException(VNOTE_READ_ALIAS, e);
		}
	}

	public void refresh() {
		try {
			esClient.indices().refresh(r -> r.index(VNOTE_WRITE_ALIAS));
		} catch (ElasticsearchException | IOException e) {
			logger.error("[es][vnote][{}] Unable to refresh {}, search results may be stale", container.uid,
					VNOTE_WRITE_ALIAS, e);
		}
	}

	public static String escape(VNoteQuery query) {
		return query.escapeQuery ? Queries.escape(query.query) : query.query;
	}

	private String getId(long itemId) {
		return shardId + ":" + container.id + ":" + itemId;
	}
}
