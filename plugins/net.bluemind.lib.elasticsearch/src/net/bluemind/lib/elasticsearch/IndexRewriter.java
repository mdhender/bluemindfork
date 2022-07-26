package net.bluemind.lib.elasticsearch;

import static java.util.concurrent.TimeUnit.MINUTES;

import org.elasticsearch.action.DocWriteRequest.OpType;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexAction;
import org.elasticsearch.action.admin.indices.get.GetIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.ReindexAction;
import org.elasticsearch.index.reindex.ReindexRequestBuilder;
import org.elasticsearch.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexRewriter {
	private static final Logger logger = LoggerFactory.getLogger(IndexRewriter.class);

	private final Client client;

	public IndexRewriter() {
		this.client = ESearchActivator.getClient();
	}

	public void rewrite(RewritableIndex index) {
		GetAliasesResponse response = ESearchActivator.getClient().admin().indices()
				.prepareGetAliases(index.readAlias()).get();
		String fromIndex = response.getAliases().keysIt().next();
		String toIndex = index.newName();
		byte[] schema = ESearchActivator.getIndexSchema(index.prefix());

		createIndex(toIndex, schema);
		moveAlias(fromIndex, toIndex, index.writeAlias(), true);
		reindex(fromIndex, toIndex);
		moveAlias(fromIndex, toIndex, index.readAlias(), false);
		deleteIndex(fromIndex);
	}

	private void createIndex(String toIndex, byte[] schema) {
		IndicesExistsResponse existsResp = client.admin().indices().prepareExists(toIndex).get();
		if (!existsResp.isExists()) {
			client.admin().indices().prepareCreate(toIndex).setSource(schema, XContentType.JSON).get();
			ClusterHealthResponse resp = client.admin().cluster().prepareHealth(toIndex).setWaitForGreenStatus().get();
			logger.info("New index created: {} ({})", toIndex, resp);
		}
	}

	private void moveAlias(String fromIndex, String toIndex, String alias, boolean writeIndex) {
		client.admin().indices().prepareAliases() //
				.removeAlias(fromIndex, alias) //
				.addAlias(toIndex, alias, writeIndex) //
				.get();
		logger.info("Alias {} moved from {} to {} (write={})", alias, fromIndex, toIndex, writeIndex);
	}

	private void reindex(String fromIndex, String toIndex) {
		GetIndexResponse indexResponse = new GetIndexRequestBuilder(client, GetIndexAction.INSTANCE, fromIndex).get();
		int numberOfShards = indexResponse.settings().get(fromIndex).getAsInt("index.number_of_shards", 1);
		long docCount = client.admin().indices().prepareStats(fromIndex).get().getTotal().docs.getCount();
		logger.info("Starting reindexation of {} with {} slice ({} docs)", fromIndex, numberOfShards, docCount);
		ReindexRequestBuilder reindexBuilder = new ReindexRequestBuilder(client, ReindexAction.INSTANCE)
				.source(fromIndex).destination(toIndex).setSlices(numberOfShards).abortOnVersionConflict(false);
		reindexBuilder.destination().setOpType(OpType.INDEX);
		BulkByScrollResponse reindexResponse = reindexBuilder.get();
		if (!reindexResponse.getBulkFailures().isEmpty()) {
			logger.error("{} reindexation failures", reindexResponse.getBulkFailures().size());
			reindexResponse.getBulkFailures().forEach(failure -> logger.error("- {}", failure.getMessage()));
		}
		logger.info("Reindexation done for {} into {}: {}", fromIndex, toIndex, reindexResponse);
	}

	private void deleteIndex(String fromIndex) {
		try {
			ESearchActivator.getClient().admin().indices().delete(new DeleteIndexRequest(fromIndex)).get(10, MINUTES);
			logger.info("Deletion of {}", fromIndex);
		} catch (Exception e) { // NOSONAR
			logger.error("Failed to delete {}", fromIndex, e);
		}
	}

}
