/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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

package net.bluemind.core.auditlogs.client.es;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlogs.AuditLogEntry;
import net.bluemind.core.auditlogs.AuditLogQuery;
import net.bluemind.core.auditlogs.IItemChangeLogClient;
import net.bluemind.core.auditlogs.client.loader.config.AuditLogConfig;
import net.bluemind.core.container.model.ChangeLogEntry.Type;
import net.bluemind.core.container.model.ItemChangeLogEntry;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.lib.elasticsearch.Pit;
import net.bluemind.lib.elasticsearch.Pit.PaginableSearchQueryBuilder;
import net.bluemind.lib.elasticsearch.Pit.PaginationParams;
import net.bluemind.lib.elasticsearch.exception.ElasticDocumentException;

public class ElasticSearchItemChangeLogClient implements IItemChangeLogClient {
	private static final Logger logger = LoggerFactory.getLogger(ElasticSearchItemChangeLogClient.class);

	public ElasticSearchItemChangeLogClient() {
	}

	@Override
	public ItemChangelog getItemChangeLog(String domainUid, String containerUid, String itemUid, Long from) {
		ElasticsearchClient esClient = AudiLogEsClientActivator.get();

		final Long since = null == from ? 0L : from;
		SortOptions sort = new SortOptions.Builder().field(f -> f.field("@timestamp").order(SortOrder.Asc)).build();
		String indexName = AuditLogConfig.resolveDataStreamName(domainUid);
		try {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index(indexName).sort(sort) //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("container.uid").value(containerUid))._toQuery())
							.must(TermQuery.of(t -> t.field("item.uid").value(itemUid))._toQuery())
							.must(RangeQuery.of(r -> r.field("item.version").gte(JsonData.of(since)))._toQuery()))),
					AuditLogEntry.class);
			List<Hit<AuditLogEntry>> list = response.hits().hits();
			return buildResponse(list);

		} catch (ElasticsearchException | IOException e) {
			throw new ElasticDocumentException(indexName, e);
		}
	}

	@Override
	public List<AuditLogEntry> queryAuditLog(AuditLogQuery query) {
		if (query.domainUid == null) {
			throw new ServerFault("Domain uid not found for query " + query);
		}
		String indexName = AuditLogConfig.resolveDataStreamName(query.domainUid);
		SortOptions sortOptions = new SortOptions.Builder().field(f -> f.field("@timestamp").order(SortOrder.Desc))
				.build();
		BoolQuery boolQuery = buildEsQuery(query);
		logger.debug("query for auditlog: {}", boolQuery);
		PaginableSearchQueryBuilder paginable = s -> s //
				.source(so -> so.fetch(true)) //
				.trackTotalHits(t -> t.enabled(true)) //
				.query(q -> q.bool(boolQuery)) //
				.sort(sortOptions);

		try {
			return (query.size > 10_000) //
					? paginatedSearch(indexName, paginable, query) //
					: simpleSearch(indexName, paginable, query);
		} catch (ElasticsearchException | IOException e) {
			e.printStackTrace();
			logger.error("Problem wih '{}': {}", indexName, e.getMessage());
		}
		return Collections.emptyList();
	}

	private ItemChangelog buildResponse(List<Hit<AuditLogEntry>> searchHits) {
		ItemChangelog changelog = new ItemChangelog();

		changelog.entries = searchHits.stream().map(h -> {
			AuditLogEntry auditLogEntry = h.source();
			ItemChangeLogEntry entry = new ItemChangeLogEntry();

			entry.date = auditLogEntry.timestamp;

			if (auditLogEntry.item != null) {
				entry.version = auditLogEntry.item.version();
				entry.internalId = auditLogEntry.item.id();
				entry.itemUid = auditLogEntry.item.uid();
				entry.itemExtId = null;
			}
			if (auditLogEntry.securityContext != null) {
				entry.author = auditLogEntry.securityContext.displayName();
				entry.origin = auditLogEntry.securityContext.origin();
			}

			String type = auditLogEntry.action;
			Arrays.asList(Type.values()).stream().filter(t -> t.name().equals(type)).findFirst()
					.ifPresent(t -> entry.type = t);
			return entry;
		}).toList();
		return changelog;
	}

	private BoolQuery buildEsQuery(AuditLogQuery query) {
		BoolQuery.Builder builder = new BoolQuery.Builder();
		builder.must(TermQuery.of(t -> t.field("domainUid").value(query.domainUid))._toQuery());
		if (query.container != null) {
			builder.must(TermQuery.of(t -> t.field("container.uid").value(query.container))._toQuery());
		}
		if (query.author != null && !query.author.isBlank()) {
			builder.must(TermQuery.of(t -> t.field("content.author").value(query.author))._toQuery());
		}
		if (query.logtype != null) {
			builder.must(TermQuery.of(t -> t.field("logtype").value(query.logtype))._toQuery());
		}
		if (query.with != null && !query.with.isBlank()) {
			builder.must(TermQuery.of(t -> t.field("content.with").value(query.with))._toQuery());
		}
		if (query.description != null && !query.description.isBlank()) {
			builder.must(MatchQuery.of(t -> t.field("content.description").query(query.description))._toQuery());
		}
		if (query.to != null) {
			builder.must(RangeQuery.of(t -> t.field("@timestamp").lt(JsonData.of(query.to.getTime())))._toQuery());
		}
		if (query.from != null) {
			builder.must(RangeQuery.of(t -> t.field("@timestamp").gt(JsonData.of(query.from.getTime())))._toQuery());
		}
		return builder.build();
	}

	private List<AuditLogEntry> simpleSearch(String indexName, PaginableSearchQueryBuilder paginableSearch,
			AuditLogQuery query) throws ElasticsearchException, IOException {
		ElasticsearchClient esClient = AudiLogEsClientActivator.get();

		SearchResponse<AuditLogEntry> response = esClient.search(paginableSearch.andThen(s -> {
			s.index(indexName);
			return (query.size > 0) ? s.size(query.size) : s;
		}), AuditLogEntry.class);
		return response.hits().hits().stream().map(Hit::source).toList();
	}

	private List<AuditLogEntry> paginatedSearch(String indexName, PaginableSearchQueryBuilder paginableSearch,
			AuditLogQuery query) throws ElasticsearchException, IOException {
		ElasticsearchClient esClient = AudiLogEsClientActivator.get();
		SortOptions sort = new SortOptions.Builder().field(f -> f.field("@timestamp").order(SortOrder.Desc)).build();
		try (Pit<AuditLogEntry> pit = Pit.allocate(esClient, indexName, 60, AuditLogEntry.class)) {
			return pit.allPages(paginableSearch, new PaginationParams(0, query.size, sort), Hit::source);
		}
	}

}
