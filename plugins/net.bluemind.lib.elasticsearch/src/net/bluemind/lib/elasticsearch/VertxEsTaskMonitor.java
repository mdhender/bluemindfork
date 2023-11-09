package net.bluemind.lib.elasticsearch;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.client.ResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch.tasks.GetTasksResponse;
import co.elastic.clients.json.JsonData;
import io.vertx.core.Vertx;
import net.bluemind.lib.elasticsearch.exception.ElasticTaskException;

public class VertxEsTaskMonitor implements EsTaskMonitor {
	private static final Logger logger = LoggerFactory.getLogger(IndexRewriter.class);

	private static final Long delay = Duration.ofSeconds(60).toMillis();
	private static final Time waitTimeout = Time.of(t -> t.time("20s"));

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private final Vertx vertx;
	private final ElasticsearchClient esClient;

	public VertxEsTaskMonitor(Vertx vertx, ElasticsearchClient esClient) {
		this.vertx = vertx;
		this.esClient = esClient;
	}

	@Override
	public JsonData waitForCompletion(String taskId) throws ElasticTaskException {
		CompletableFuture<JsonData> future = new CompletableFuture<>();
		try {
			return monitor(future, taskId).get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ElasticTaskException("Failed to track task id '" + taskId + "', interrupted");
		} catch (ExecutionException e) {
			if (e.getCause() instanceof ElasticTaskException t) {
				throw t;
			} else {
				throw new ElasticTaskException(e);
			}
		}
	}

	@Override
	public CompletableFuture<JsonData> monitor(String taskId) {
		return monitor(new CompletableFuture<>(), taskId);
	}

	private CompletableFuture<JsonData> monitor(CompletableFuture<JsonData> future, String taskId) {
		try {
			GetTasksResponse task = esClient.tasks()
					.get(t -> t.taskId(taskId).waitForCompletion(true).timeout(waitTimeout));
			return (!task.completed()) ? queue(future, taskId) : complete(future, task);
		} catch (ResponseException e) {
			JsonNode details = decodeResponseException(e);
			return ("timeout_exception".equals(details.at("/error/type").asText())) //
					? queue(future, taskId)
					: CompletableFuture.failedFuture(new ElasticTaskException(e)); //
		} catch (ElasticsearchException | IOException e) {
			return CompletableFuture.failedFuture(new ElasticTaskException(e));
		}

	}

	private CompletableFuture<JsonData> queue(CompletableFuture<JsonData> future, String taskId) {
		vertx.setTimer(delay, timer -> monitor(future, taskId));
		return future;
	}

	private CompletableFuture<JsonData> complete(CompletableFuture<JsonData> future, GetTasksResponse task) {
		if (task.error() != null) {
			future.completeExceptionally(new ElasticTaskException(task.error().reason()));
		} else {
			future.complete(task.response());
		}
		return future;
	}

	private JsonNode decodeResponseException(ResponseException e) {
		try {
			return objectMapper.readTree(e.getResponse().getEntity().getContent());
		} catch (UnsupportedOperationException | IOException e1) {
			logger.error("Failed to decode ResponseException entity", e);
			return MissingNode.getInstance();
		}
	}
}
