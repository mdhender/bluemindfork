package net.bluemind.core.backup.store.kafka;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

import net.bluemind.core.backup.continuous.store.TopicPublisher;

public class KafkaTopicPublisher implements TopicPublisher {

	private static final Logger logger = LoggerFactory.getLogger(KafkaTopicPublisher.class);

	private final String bootstrapServer;
	private final String physicalTopic;
	private final KafkaProducer<byte[], byte[]> producer;

	private static final Map<String, KafkaProducer<byte[], byte[]>> perPhyTopicProd = new ConcurrentHashMap<>();

	public KafkaTopicPublisher(String bootstrapServer, String physicalTopic) {
		this.bootstrapServer = bootstrapServer;
		this.physicalTopic = physicalTopic;
		this.producer = perPhyTopicProd.computeIfAbsent(physicalTopic, s -> createKafkaProducer());
	}

	@Override
	public CompletableFuture<Void> store(String partitionToken, byte[] key, byte[] data) {
		CompletableFuture<Void> comp = new CompletableFuture<>();
		int partition = Math.abs(partitionToken.hashCode() % KafkaTopicStore.PARTITION_COUNT);
		ProducerRecord<byte[], byte[]> rec = new ProducerRecord<>(physicalTopic, partition, key, data);

		producer.send(rec, (RecordMetadata metadata, Exception exception) -> {
			if (exception != null) {
				logger.warn(exception.getMessage());
				comp.completeExceptionally(exception);
			} else {
				comp.complete(null);
			}
		});
		return comp;
	}

	private KafkaProducer<byte[], byte[]> createKafkaProducer() {
		Properties producerProps = new Properties();
		producerProps.setProperty("bootstrap.servers", bootstrapServer);
		producerProps.setProperty("acks", "all");
		producerProps.setProperty(ProducerConfig.METRIC_REPORTER_CLASSES_CONFIG,
				BluemindMetricsReporter.class.getCanonicalName());
		producerProps.setProperty("compression.type", KafkaTopicStore.COMPRESSION_TYPE);
		producerProps.setProperty("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
		producerProps.setProperty("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
		producerProps.setProperty("batch.size", "250");
		return new KafkaProducer<>(producerProps);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper("KafkaTopic").add("name", physicalTopic).toString();
	}
}
