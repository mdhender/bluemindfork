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

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCardQuery;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.lib.elasticsearch.EsBulk;
import net.bluemind.lib.elasticsearch.Pit;
import net.bluemind.lib.elasticsearch.Pit.PaginableSearchQueryBuilder;
import net.bluemind.lib.elasticsearch.Pit.PaginationParams;
import net.bluemind.lib.elasticsearch.Queries;
import net.bluemind.lib.elasticsearch.exception.ElasticDocumentException;
import net.bluemind.network.topology.Topology;

public class VCardIndexStore {

	private static final Logger logger = LoggerFactory.getLogger(VCardIndexStore.class);

	public static final int SIZE = 200;

	public static final String VCARD_WRITE_ALIAS = "contact_write_alias";
	public static final String VCARD_READ_ALIAS = "contact_read_alias";

	private final ElasticsearchClient esClient;
	private final Container container;
	private final long shardId;
	private final Query containerQuery;

	public VCardIndexStore(ElasticsearchClient esClient, Container container, String loc) {
		this.esClient = esClient;
		this.container = container;
		this.shardId = loc == null ? 0 : Topology.get().datalocation(loc).internalId;
		this.containerQuery = TermQuery.of(t -> t.field("containerUid").value(container.uid))._toQuery();
	}

	public static class IndexableVCard {
		public String uid;
		public String containerUid;
		public String displayName;
		public VCard value;
		public String sortName;
	}

	private IndexableVCard asIndexable(String uid, VCard card) {
		IndexableVCard holder = new IndexableVCard();
		holder.uid = uid;
		holder.containerUid = container.uid;
		holder.displayName = card.identification.formatedName.value;
		holder.sortName = holder.displayName;
		holder.value = card;
		return holder;
	}

	public void create(Item item, VCard card) {
		store(item, card);
	}

	public void update(Item item, VCard card) {
		store(item, card);
	}

	private void store(Item item, VCard card) {
		IndexableVCard toIndex = asIndexable(item.uid, card);
		try {
			esClient.index(i -> i.index(VCARD_WRITE_ALIAS).id(getId(item.id)).document(toIndex));
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticDocumentException(VCARD_WRITE_ALIAS, e);
		}
	}

	public void updates(List<ItemValue<VCard>> cards) {
		if (cards.isEmpty()) {
			return;
		}

		new EsBulk(esClient).commitAll(cards, (card, b) -> b.index(idx -> idx //
				.index(VCARD_WRITE_ALIAS) //
				.id(getId(card.internalId)) //
				.document(asIndexable(card.uid, card.value))));
	}

	public void delete(String uid) {
		Query toDelete = QueryBuilders.bool(b -> b //
				.must(containerQuery).must(TermQuery.of(t -> t.field("uid").value(uid))._toQuery()));
		try {
			esClient.deleteByQuery(d -> d.index(VCARD_WRITE_ALIAS).query(toDelete));
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticDocumentException(VCARD_WRITE_ALIAS, e);
		}
	}

	public void deleteAll() {
		Query toDelete = QueryBuilders.bool(b -> b.must(containerQuery));
		try {
			esClient.deleteByQuery(d -> d.index(VCARD_WRITE_ALIAS).query(toDelete));
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticDocumentException(VCARD_WRITE_ALIAS, e);
		}
	}

	public ListResult<String> search(VCardQuery query) {
		Query queryString = (Strings.isNullOrEmpty(query.query)) //
				? MatchAllQuery.of(q -> q)._toQuery() //
				: QueryStringQuery.of(q -> q.query(escape(query)).defaultOperator(Operator.And))._toQuery();

		Query searchQuery = QueryBuilders.bool(b -> b.must(containerQuery).must(queryString));
		PaginableSearchQueryBuilder paginableSearch = s -> s.query(searchQuery).source(src -> src.fetch(false))
				.storedFields("uid");

		try {
			return (query.from + query.size > 10000) //
					? paginatedSearch(paginableSearch, query) //
					: simpleSearch(paginableSearch, query);
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticDocumentException(VCARD_READ_ALIAS, e);
		}
	}

	private ListResult<String> simpleSearch(PaginableSearchQueryBuilder paginableSearch, VCardQuery query)
			throws ElasticsearchException, IOException {
		SortOptions sort = SortOptions
				.of(so -> (query.orderBy == null || query.orderBy == VCardQuery.OrderBy.FormatedName)
						? so.field(f -> f.field("sortName"))
						: so.score(sc -> sc));

		SearchResponse<Void> response = esClient.search(paginableSearch.andThen(s -> {
			s.index(VCARD_READ_ALIAS).sort(sort);
			return (query.size > 0) ? s.from(query.from).size(query.size) : s;
		}), Void.class);

		List<String> uids = response.hits().hits().stream()
				.map(hit -> hit.fields().get("uid").toJson().asJsonArray().getString(0)).toList();
		return ListResult.create(uids, uids.size());
	}

	private ListResult<String> paginatedSearch(PaginableSearchQueryBuilder paginableSearch, VCardQuery query)
			throws ElasticsearchException, IOException {
		SortOptions sort = SortOptions
				.of(so -> (query.orderBy == null || query.orderBy == VCardQuery.OrderBy.FormatedName)
						? so.field(f -> f.field("sortName"))
						: so.field(f -> f.field("_shard_doc").order(SortOrder.Asc)));
		try (Pit<Void> pit = Pit.allocate(esClient, VCARD_READ_ALIAS, 60, Void.class)) {
			List<String> uids = pit.allPages(paginableSearch, new PaginationParams(query.from, query.size, sort),
					hit -> hit.fields().get("uid").toJson().asJsonArray().getString(0));
			return ListResult.create(uids, uids.size());
		}
	}

	/**
	 * escape the elastic-search query string. we escape all but the ":" character,
	 * since it also serves as the field:value separator
	 * 
	 * @param query
	 * @return
	 */
	public static String escape(VCardQuery query) {
		return query.escapeQuery ? Queries.escape(query.query) : query.query;
	}

	public void refresh() {
		try {
			esClient.indices().refresh(r -> r.index(VCARD_WRITE_ALIAS));
		} catch (ElasticsearchException | IOException e) {
			logger.error("[es][vcard][{}] Unable to refresh {}, search results may be stale", container.uid,
					VCARD_WRITE_ALIAS, e);
		}
	}

	private String getId(long itemId) {
		return shardId + ":" + container.id + ":" + itemId;
	}
}
