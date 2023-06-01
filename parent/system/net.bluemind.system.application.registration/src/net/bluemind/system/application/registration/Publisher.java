package net.bluemind.system.application.registration;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.curator.shaded.com.google.common.base.MoreObjects;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Publisher {

	private static final Logger logger = LoggerFactory.getLogger(Publisher.class);

	private final String bootstrapServer;
	private final String physicalTopic;
	private final KafkaProducer<byte[], byte[]> producer;

	static final Map<String, KafkaProducer<byte[], byte[]>> perPhyTopicProd = new ConcurrentHashMap<>();

	public Publisher(String bootstrapServer, String physicalTopic) {
		this.bootstrapServer = bootstrapServer;
		this.physicalTopic = physicalTopic;
		this.producer = perPhyTopicProd.computeIfAbsent(physicalTopic, s -> createKafkaProducer());
	}

	public CompletableFuture<Void> store(String partitionToken, byte[] key, byte[] data) {
		CompletableFuture<Void> comp = new CompletableFuture<>();
		int partition = 0;
		ProducerRecord<byte[], byte[]> rec = new ProducerRecord<>(physicalTopic, partition, key, data);

		producer.send(rec, (RecordMetadata metadata, Exception exception) -> {
			if (exception != null) {
				logger.warn("Could not store {}byte(s) of data. Key: {}, ({})", data == null ? 0 : data.length,
						new String(key), exception.getMessage());
				comp.completeExceptionally(exception);
			} else {
				logger.debug("[{}] stored part: {}, meta: {}", physicalTopic, partition, metadata);
				comp.complete(null);
			}
		});
		return comp;
	}

	private KafkaProducer<byte[], byte[]> createKafkaProducer() {
		Properties producerProps = new Properties();
		producerProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
		producerProps.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, Store.COMPRESSION_TYPE);
		producerProps.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
				"org.apache.kafka.common.serialization.ByteArraySerializer");
		producerProps.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
				"org.apache.kafka.common.serialization.ByteArraySerializer");
		producerProps.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, Integer.toString(1));
		producerProps.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
		return new KafkaProducer<>(producerProps);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper("KafkaTopic").add("name", physicalTopic).add("prod", producer).toString();
	}
}
