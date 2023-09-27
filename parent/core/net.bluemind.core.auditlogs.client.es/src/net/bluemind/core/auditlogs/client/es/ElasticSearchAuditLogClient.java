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

import java.io.ByteArrayInputStream;
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
import net.bluemind.core.auditlogs.AuditLogEntry;
import net.bluemind.core.auditlogs.AuditLogQuery;
import net.bluemind.core.auditlogs.IAuditLogClient;
import net.bluemind.core.container.model.ChangeLogEntry.Type;
import net.bluemind.core.container.model.ItemChangeLogEntry;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.elasticsearch.exception.ElasticDocumentException;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.network.topology.TopologyException;
import net.bluemind.retry.support.RetryRequester;
import net.bluemind.system.api.SystemState;
import net.bluemind.system.state.StateContext;

public class ElasticSearchAuditLogClient implements IAuditLogClient {
	private static final String INDEX_AUDIT_LOG = "audit_log";
	private static final Logger logger = LoggerFactory.getLogger(ElasticSearchAuditLogClient.class);

	private RetryRequester requester;

	public ElasticSearchAuditLogClient() {
		this.requester = new RetryRequester(VertxPlatform.eventBus(), "audit");
	}

	@Override
	public void storeAuditLog(AuditLogEntry document) {
		if (StateContext.getState() != SystemState.CORE_STATE_RUNNING) {
			return;
		}

		try {
			ElasticsearchClient esClient = ESearchActivator.getClient();
			if (esClient == null) {
				return;
			}
			byte[] bytes = JsonUtils.asBytes(document);
			esClient.index(i -> i.index(INDEX_AUDIT_LOG).withJson(new ByteArrayInputStream(bytes)));
		} catch (ElasticsearchException | IOException e) {
			logger.error("Problem wih '{}': {}", INDEX_AUDIT_LOG, e.getMessage());
		} catch (TopologyException e) {
			logger.warn("ElasticClient is not available: {}", e.getMessage());
		}

	}

	@Override
	public ItemChangelog getItemChangeLog(String containerUid, String itemUid, Long from) {
		ElasticsearchClient esClient = ESearchActivator.getClient();

		final Long since = null == from ? 0L : from;
		SortOptions sort = new SortOptions.Builder().field(f -> f.field("@timestamp").order(SortOrder.Asc)).build();

		try {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index(INDEX_AUDIT_LOG).sort(sort) //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("container.uid").value(containerUid))._toQuery())
							.must(TermQuery.of(t -> t.field("item.uid").value(itemUid))._toQuery())
							.must(RangeQuery.of(r -> r.field("item.version").gte(JsonData.of(since)))._toQuery()))),
					AuditLogEntry.class);
			List<Hit<AuditLogEntry>> list = response.hits().hits();
			return buildResponse(list);

		} catch (ElasticsearchException | IOException e) {
			throw new ElasticDocumentException(INDEX_AUDIT_LOG, e);
		}
	}

	@Override
	public List<AuditLogEntry> queryAuditLog(AuditLogQuery query) {
		ElasticsearchClient esClient = ESearchActivator.getClient();
		SortOptions sort = new SortOptions.Builder().field(f -> f.field("@timestamp").order(SortOrder.Asc)).build();
		BoolQuery boolQuery = buildQuery(query);
		logger.debug("query for auditlog: {}", boolQuery);
		try {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index(INDEX_AUDIT_LOG).sort(sort).size(query.size) //
					.query(q -> q.bool(boolQuery)), AuditLogEntry.class);
			if (!response.hits().hits().isEmpty()) {
				return response.hits().hits().stream().map(Hit::source).toList();
			}
			return Collections.emptyList();

		} catch (ElasticsearchException | IOException e) {
			e.printStackTrace();
			logger.error("Problem wih '{}': {}", INDEX_AUDIT_LOG, e.getMessage());
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

	private BoolQuery buildQuery(AuditLogQuery query) {
		BoolQuery.Builder builder = new BoolQuery.Builder();
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

}
