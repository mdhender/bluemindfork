package net.bluemind.lib.elasticsearch;

import java.util.concurrent.CompletableFuture;

import co.elastic.clients.elasticsearch.tasks.Status;
import net.bluemind.lib.elasticsearch.exception.ElasticTaskException;

public interface EsTaskMonitor {

	Status waitForCompletion(String taskId) throws ElasticTaskException;

	CompletableFuture<Status> monitor(String taskId);

}
