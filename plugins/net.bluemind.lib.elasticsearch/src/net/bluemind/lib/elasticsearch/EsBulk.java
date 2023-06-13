package net.bluemind.lib.elasticsearch;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.util.ObjectBuilder;
import net.bluemind.lib.elasticsearch.exception.ElasticBulkException;

public class EsBulk {
	private static final Logger logger = LoggerFactory.getLogger(EsBulk.class);

	private ElasticsearchClient esClient;
	private BulkRequest.Builder builder;

	public EsBulk(ElasticsearchClient esClient) {
		this.esClient = esClient;
		this.builder = new BulkRequest.Builder();
	}

	public <T> Optional<BulkResponse> commitAll(List<T> toBulk,
			BiFunction<T, BulkOperation.Builder, ObjectBuilder<BulkOperation>> map) {
		if (toBulk.isEmpty()) {
			logger.warn("Empty bulk, not running.");
			return Optional.empty();
		}
		BulkRequest request = builder
				.operations(toBulk.stream().map(e -> map.apply(e, new BulkOperation.Builder()).build()).toList())
				.build();
		try {
			BulkResponse response = esClient.bulk(request);
			reportErrors(response);
			return Optional.of(response);
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticBulkException(e);
		}
	}

	private void reportErrors(BulkResponse response) {
		if (!response.errors()) {
			return;
		}
		List<BulkResponseItem> failedItems = response.items().stream().filter(i -> i.error() != null).toList();
		failedItems.forEach(i -> logger.error("Bulk request failed on index:{} id:{} error:{} stack:{}", //
				i.index(), i.id(), i.error().type(), i.error().stackTrace()));
	}

}
