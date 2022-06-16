package net.bluemind.central.reverse.proxy.model.common.kafka.impl;

import static java.time.Duration.ofSeconds;

import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import net.bluemind.central.reverse.proxy.model.common.kafka.KafkaConsumerClient;

public class KafkaConsumerClientImpl<K, V> implements KafkaConsumerClient<K, V> {

	private final Logger logger = LoggerFactory.getLogger(KafkaConsumerClientImpl.class);
	private static final int MIN_UPTIME_BEFORE_EMPTY_CONSUMPTION = 10;
	private static final int POLL_DURATION_IN_SECONDS = 1;

	private final Vertx vertx;

	private long startTimeInMillis;
	private boolean hadRecords = false;
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
	public Future<Void> subscribe(List<String> topics) {
		consumer.subscribe(topics);
		startTimeInMillis = System.currentTimeMillis();
		return consume();
	}

	private Future<Void> consume() {
		Promise<Void> emptyConsumptionPromise = Promise.promise();
		consume(emptyConsumptionPromise);
		return emptyConsumptionPromise.future();
	}

	private void consume(Promise<Void> emptyConsumptionPromise) {
		vertx.executeBlocking(pollingPromise -> {
			final ConsumerRecords<K, V> records = consumer.poll(ofSeconds(POLL_DURATION_IN_SECONDS));
			boolean hasRecord = handle(records);
			if (isEmptyConsumption(hasRecord, emptyConsumptionPromise)) {
				emptyConsumptionPromise.complete();
			} else if (hasRecord) {
				hadRecords = true;
			}
			pollingPromise.complete(hasRecord);
		}, true, (AsyncResult<Boolean> hasRecord) -> consume(emptyConsumptionPromise));
	}

	private boolean isEmptyConsumption(boolean hasRecord, Promise<Void> emptyConsumptionPromise) {
		return (!hasRecord && !emptyConsumptionPromise.future().isComplete()
				&& (hadRecords || upTimeInSeconds() > MIN_UPTIME_BEFORE_EMPTY_CONSUMPTION));
	}

	private boolean handle(final ConsumerRecords<K, V> records) {
		if (records == null || records.count() == 0) {
			return false;
		}
		logger.info("consuming {} records", records.count());
		if (Objects.isNull(this.batchHandler) && !Objects.isNull(this.handler)) {
			records.forEach(handler::handle);
		} else if (!Objects.isNull(this.batchHandler)) {
			this.batchHandler.handle(records);
		}
		return true;
	}

	private long upTimeInSeconds() {
		return (System.currentTimeMillis() - startTimeInMillis) / 1000;
	}
}
