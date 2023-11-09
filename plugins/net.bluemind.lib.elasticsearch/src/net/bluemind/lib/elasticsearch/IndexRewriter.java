package net.bluemind.lib.elasticsearch;

import static co.elastic.clients.elasticsearch._types.HealthStatus.Green;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Conflicts;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.OpType;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.elasticsearch.core.ReindexResponse;
import co.elastic.clients.elasticsearch.indices.GetAliasResponse;
import co.elastic.clients.elasticsearch.indices.IndicesStatsResponse;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import io.vertx.core.Vertx;
import net.bluemind.lib.elasticsearch.exception.ElasticTaskException;

public class IndexRewriter {
	private static final Logger logger = LoggerFactory.getLogger(IndexRewriter.class);

	private final ElasticsearchClient esClient;

	public IndexRewriter() {
		this.esClient = ESearchActivator.getClient();
	}

	public void rewrite(RewritableIndex index) throws ElasticsearchException, IOException {
		rewrite(index, "*");
	}

	public void rewrite(RewritableIndex index, String fromVersion) throws ElasticsearchException, IOException {
		GetAliasResponse response = esClient.indices().getAlias(a -> a.name(index.readAlias()));
		String fromIndex = response.result().keySet().iterator().next();
		String version = esClient.indices().getSettings(s -> s.index(fromIndex).includeDefaults(true)) //
				.get(fromIndex).settings().index().version().created();
		if (!fromVersion.equals("*") && !version.startsWith(fromVersion)) {
			return;
		}

		String toIndex = index.newName();
		byte[] schema = ESearchActivator.getIndexSchema(index.prefix());

		createIndex(toIndex, schema);
		moveAlias(fromIndex, toIndex, index.writeAlias(), true);
		reindex(fromIndex, toIndex);
		moveAlias(fromIndex, toIndex, index.readAlias(), false);
		deleteIndex(fromIndex);
	}

	private void createIndex(String toIndex, byte[] schema) throws ElasticsearchException, IOException {
		BooleanResponse exists = esClient.indices().exists(e -> e.index(toIndex));
		if (!exists.value()) {
			esClient.indices().create(c -> c.index(toIndex).withJson(new ByteArrayInputStream(schema)));
			HealthResponse response = esClient.cluster().health(h -> h.index(toIndex).waitForStatus(Green));
			logger.info("New index created: {} ({})", toIndex, response);
		}
	}

	private void moveAlias(String fromIndex, String toIndex, String alias, boolean writeIndex)
			throws ElasticsearchException, IOException {
		esClient.indices().updateAliases(u -> u //
				.actions(a -> a.remove(r -> r.index(fromIndex).alias(alias))) //
				.actions(a -> a.add(add -> add.index(toIndex).alias(alias).isWriteIndex(writeIndex))));
		logger.info("Alias {} moved from {} to {} (write={})", alias, fromIndex, toIndex, writeIndex);
	}

	private void reindex(String fromIndex, String toIndex) throws ElasticsearchException, IOException {
		IndicesStatsResponse statsResponse = esClient.indices().stats(s -> s.index(fromIndex));
		int numberOfShards = (int) statsResponse.indices().get(fromIndex).total().shardStats().totalCount();
		long docCount = statsResponse.indices().get(fromIndex).total().docs().count();
		logger.info("Starting reindexation of {} with {} slice ({} docs)", fromIndex, numberOfShards, docCount);
		ReindexResponse response = esClient.reindex(r -> r //
				.waitForCompletion(false) //
				.source(s -> s.index(fromIndex)) //
				.dest(d -> d.index(toIndex).opType(OpType.Index)) //
				.slices(s -> s.value(numberOfShards)) //
				.conflicts(Conflicts.Proceed) //
				.scroll(s -> s.time("1d")));
		JsonData status;
		try {
			status = new VertxEsTaskMonitor(Vertx.vertx(), esClient).waitForCompletion(response.task());
			List<String> failures = status.toJson().asJsonObject().getJsonArray("failures").stream()
					.map(Object::toString).toList();
			if (!failures.isEmpty()) {
				logger.error("Reindexation done with {} failures:", response.failures().size());
				failures.forEach(failure -> logger.error("- {}", failure));
			} else {
				logger.info("Reindexation done for {} into {}: {}", fromIndex, toIndex, response);
			}
		} catch (ElasticTaskException e) {
			logger.error("Failed while tracking task id '{}', continue", response.task(), e);
		}
	}

	private void deleteIndex(String fromIndex) {
		try {
			esClient.indices().delete(d -> d.index(fromIndex).ignoreUnavailable(true));
			logger.info("Deletion of {}", fromIndex);
		} catch (Exception e) { // NOSONAR
			logger.error("Failed to delete {}", fromIndex, e);
		}
	}

}
