package net.bluemind.central.reverse.proxy.kafka.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import net.bluemind.central.reverse.proxy.kafka.KafkaConsumerClient;

public class KafkaConsumerClientImpl<K, V> implements KafkaConsumerClient<K, V> {

	private final Logger logger = LoggerFactory.getLogger(KafkaConsumerClientImpl.class);

	private final Vertx vertx;

	private KafkaConsumer<K, V> consumer;
	private Handler<ConsumerRecord<K, V>> handler;
	private Handler<ConsumerRecords<K, V>> batchHandler;

	public KafkaConsumerClientImpl(Vertx vertx, Properties props) {
		this.vertx = vertx;

		consumer = new KafkaConsumer<>(props);
	}

	@Override
	public KafkaConsumerClientImpl<K, V> handler(Handler<ConsumerRecord<K, V>> handler) {
		this.handler = handler;
		return this;
	}

	@Override
	public KafkaConsumerClientImpl<K, V> batchHandler(Handler<ConsumerRecords<K, V>> batchHandler) {
		this.batchHandler = batchHandler;
		return this;
	}

	@Override
	public KafkaConsumerClientImpl<K, V> subscribe(List<String> topics) {
		consumer.subscribe(topics);
		consume();
		return this;
	}

	private void consume() {
		vertx.executeBlocking((Promise<Void> promise) -> {
			final ConsumerRecords<K, V> records = consumer.poll(Duration.ofSeconds(1));
			if (records.count() > 0) {
				logger.info("before handling records: {}", records.count());
			}
			handle(records);
			promise.complete(null);
		}, true, (v) -> consume());
	}

	private void handle(final ConsumerRecords<K, V> records) {
		if (records != null && records.count() > 0) {
			logger.info("consume {} records", records.count());
			if (Objects.isNull(this.batchHandler) && !Objects.isNull(this.handler)) {
				records.forEach(handler::handle);
			} else if (!Objects.isNull(this.batchHandler)) {
				this.batchHandler.handle(records);
			}
		}
	}
}
