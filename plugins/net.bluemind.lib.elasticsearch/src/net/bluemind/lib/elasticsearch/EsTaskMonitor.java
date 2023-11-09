package net.bluemind.lib.elasticsearch;

import java.util.concurrent.CompletableFuture;

import co.elastic.clients.json.JsonData;
import net.bluemind.lib.elasticsearch.exception.ElasticTaskException;

public interface EsTaskMonitor {

	JsonData waitForCompletion(String taskId) throws ElasticTaskException;

	CompletableFuture<JsonData> monitor(String taskId);

}
