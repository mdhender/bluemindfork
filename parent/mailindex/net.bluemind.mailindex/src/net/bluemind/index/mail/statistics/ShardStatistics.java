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
package net.bluemind.index.mail.statistics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.netflix.spectator.api.Registry;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import co.elastic.clients.elasticsearch.indices.get_alias.IndexAliases;
import co.elastic.clients.elasticsearch.indices.stats.IndicesStats;
import net.bluemind.index.mail.MailIndexService;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.elasticsearch.IndexAliasMapping;
import net.bluemind.lib.elasticsearch.IndexAliasMode;
import net.bluemind.lib.elasticsearch.IndexAliasMode.Mode;
import net.bluemind.lib.elasticsearch.exception.ElasticDocumentException;
import net.bluemind.lib.elasticsearch.exception.ElasticIndexException;
import net.bluemind.mailbox.api.ShardStats;
import net.bluemind.mailbox.api.ShardStats.MailboxStats;
import net.bluemind.mailbox.api.SimpleShardStats;
import net.bluemind.metrics.registry.IdFactory;

public abstract class ShardStatistics {

	protected final Registry metricRegistry;
	protected final IdFactory idFactory;

	protected ShardStatistics(Registry metricRegistry, IdFactory idFactory) {
		this.metricRegistry = metricRegistry;
		this.idFactory = idFactory;
	}

	public static ShardStatistics get(Registry metricRegistry, IdFactory idFactory) {
		return IndexAliasMode.getMode() == Mode.ONE_TO_ONE ? new OnetoOneShardStatistics(metricRegistry, idFactory)
				: new RingShardStatistics(metricRegistry, idFactory);
	}

	public List<SimpleShardStats> getLiteStats() {
		ElasticsearchClient esClient = ESearchActivator.getClient();
		List<String> indexNames = filteredMailspoolIndexNames(esClient);
		return indexNames.stream() //
				.map(indexName -> indexStats(esClient, indexName, new SimpleShardStats())) //
				.sorted((a, b) -> (int) (b.docCount - a.docCount)).toList();
	}

	public List<ShardStats> getStats() {
		ElasticsearchClient esClient = ESearchActivator.getClient();
		List<String> indexNames = filteredMailspoolIndexNames(esClient);
		List<ShardStats> ret = new ArrayList<>(indexNames.size());

		long worstResponseTime = 0;
		for (String indexName : indexNames) {
			ShardStats is = indexStats(esClient, indexName, new ShardStats());

			is.topMailbox = topMailbox(esClient, indexName, is.mailboxes);

			is.state = ShardStats.State.OK;
			if (!is.topMailbox.isEmpty()) {
				MailboxStats topMailbox = is.topMailbox.get(0);
				long duration = boxSearchDuration(esClient, topMailbox.mailboxUid);
				is.state = ShardStats.State.ofDuration(duration);
				worstResponseTime = Math.max(worstResponseTime, duration);
				metricRegistry.timer(idFactory.name("response-time", "index", is.indexName)).record(duration,
						TimeUnit.MILLISECONDS);
			}

			ret.add(is);
		}

		metricRegistry.gauge(idFactory.name("worst-response-time")).set(worstResponseTime);

		Collections.sort(ret, (a, b) -> (int) (b.docCount - a.docCount));
		return ret;
	}

	protected Set<String> indexAliases(ElasticsearchClient esClient, String indexName) {
		try {
			IndexAliases aliasesRsp = esClient.indices().getAlias(a -> a.index(indexName)).get(indexName);
			return aliasesRsp.aliases().keySet();
		} catch (Exception e) {
			return Collections.emptySet();
		}
	}

	public List<String> filteredMailspoolIndexNames(ElasticsearchClient esClient) {
		try {
			GetMappingResponse response = esClient.indices().getMapping(b -> b.index("mailspool*"));
			return response.result().entrySet().stream() //
					.filter(e -> !e.getKey().startsWith(MailIndexService.INDEX_PENDING))
					.filter(e -> e.getValue().mappings().meta() == null || !e.getValue().mappings().meta()
							.containsKey(ESearchActivator.BM_MAINTENANCE_STATE_META_KEY))
					.map(Entry::getKey).sorted().toList();
		} catch (ElasticsearchException | IOException e) {
			return Collections.emptyList();
		}
	}

	public <T extends SimpleShardStats> T indexStats(ElasticsearchClient esClient, String indexName, T is) {
		is.indexName = indexName;
		is.mailboxes = indexMailboxes(esClient, indexName);
		is.aliases = indexAliases(esClient, indexName);
		IndicesStats stat;
		try {
			stat = esClient.indices().stats(s -> s.index(indexName)).indices().get(indexName);
			is.size = stat.total().store().sizeInBytes();
			is.docCount = stat.total().docs().count();
			is.deletedCount = stat.total().docs().deleted();
			is.externalRefreshCount = stat.total().refresh().externalTotal();
			is.externalRefreshDuration = stat.total().refresh().externalTotalTimeInMillis();
			return is;
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticIndexException(indexName, e);
		}
	}

	protected abstract Set<String> indexMailboxes(ElasticsearchClient esClient, String indexName);

	protected abstract List<MailboxStats> topMailbox(ElasticsearchClient esClient, String indexName,
			Set<String> mailboxes);

	protected abstract long boxSearchDuration(ElasticsearchClient esClient, String mailboxUid);

	static class OnetoOneShardStatistics extends ShardStatistics {

		protected OnetoOneShardStatistics(Registry metricRegistry, IdFactory idFactory) {
			super(metricRegistry, idFactory);
		}

		@Override
		protected Set<String> indexAliases(ElasticsearchClient esClient, String indexName) {
			return Collections.emptySet();
		}

		@Override
		protected Set<String> indexMailboxes(ElasticsearchClient esClient, String indexName) {
			try {
				IndexAliases aliasesRsp = esClient.indices().getAlias(a -> a.index(indexName)).get(indexName);
				return aliasesRsp.aliases().keySet().stream().filter(a -> a.startsWith("mailspool_alias_")) //
						.map(a -> a.substring("mailspool_alias_".length())) //
						.collect(Collectors.toSet());
			} catch (Exception e) {
				return Collections.emptySet();
			}
		}

		@Override
		protected List<MailboxStats> topMailbox(ElasticsearchClient esClient, String indexName, Set<String> mailboxes) {
			try {
				SearchResponse<Void> aggResp = esClient.search(s -> s //
						.index(indexName).size(0)
						.aggregations("countByOwner", a -> a.terms(t -> t.field("owner").size(500))), Void.class);
				return aggResp.aggregations().get("countByOwner").sterms().buckets().array().stream() //
						.map(b -> new ShardStats.MailboxStats(b.key().stringValue(), b.docCount())) //
						.filter(as -> mailboxes.contains(as.mailboxUid)) //
						.toList();
			} catch (ElasticsearchException | IOException e) {
				throw new ElasticIndexException(indexName, e);
			}
		}

		@Override
		protected long boxSearchDuration(ElasticsearchClient esClient, String mailboxUid) {
			String alias = new IndexAliasMapping.OneToOneIndexAliasMapping().getReadAliasByMailboxUid(mailboxUid);
			try {
				String randomToken = Long.toHexString(Double.doubleToLongBits(Math.random()));
				SearchResponse<Void> results = esClient.search(s -> s //
						.index(alias) //
						.source(so -> so.fetch(false)) //
						.query(q -> q.bool(b -> b.must(m -> m.hasParent(p -> p //
								.parentType(MailIndexService.PARENT_TYPE) //
								.score(false) //
								.query(f -> f.queryString(qs -> qs.query("content:\"" + randomToken + "\"")))))
								.must(m -> m.term(t -> t.field("owner").value(mailboxUid))))),
						Void.class);
				return results.took();
			} catch (ElasticsearchException | IOException e) {
				throw new ElasticDocumentException(alias, e);
			}

		}

	}

	static class RingShardStatistics extends ShardStatistics {

		public RingShardStatistics(Registry metricRegistry, IdFactory idFactory) {
			super(metricRegistry, idFactory);
		}

		@Override
		protected Set<String> indexMailboxes(ElasticsearchClient esClient, String indexName) {
			return Collections.emptySet();
		}

		@Override
		protected List<MailboxStats> topMailbox(ElasticsearchClient esClient, String indexName, Set<String> mailboxes) {
			return Collections.emptyList();
		}

		@Override
		protected long boxSearchDuration(ElasticsearchClient esClient, String mailboxUid) {
			throw new UnsupportedOperationException("boxSearchDuration not available in ring mode");
		}

	}

}
