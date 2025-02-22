package net.bluemind.core.backup.store.kafka;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.util.concurrent.RateLimiter;
import com.typesafe.config.Config;

import net.bluemind.core.backup.continuous.store.TopicPublisher;
import net.bluemind.core.backup.store.kafka.config.KafkaStoreConfig;
import net.bluemind.core.backup.store.kafka.metrics.KafkaMetric;
import net.bluemind.core.backup.store.kafka.metrics.KafkaTopicMetrics;
import net.bluemind.core.backup.store.kafka.metrics.KafkaTopicMetrics.ClientEnum;
import net.bluemind.lib.vertx.VertxPlatform;

public class KafkaTopicPublisher implements TopicPublisher {

	private static final Logger logger = LoggerFactory.getLogger(KafkaTopicPublisher.class);

	private final String bootstrapServer;
	private final String physicalTopic;
	private final KafkaProducer<byte[], byte[]> producer;
	private final RateLimiter metricLimiter;
	private final Metric recordSendRate;

	static final Map<String, KafkaProducer<byte[], byte[]>> perPhyTopicProd = new ConcurrentHashMap<>();
	private static final Map<String, RateLimiter> metricsLimiter = new ConcurrentHashMap<>();

	public KafkaTopicPublisher(String bootstrapServer, String physicalTopic) {
		this.bootstrapServer = bootstrapServer;
		this.physicalTopic = physicalTopic;
		this.producer = perPhyTopicProd.computeIfAbsent(physicalTopic, s -> createKafkaProducer());
		this.metricLimiter = metricsLimiter.computeIfAbsent(physicalTopic, t -> RateLimiter.create(1.0));
		this.recordSendRate = producer.metrics().entrySet().stream()
				.filter(m -> KafkaTopicMetrics.SEND_RATE.equals(m.getKey().name())
						&& "producer-metrics".equals(m.getKey().group()))
				.map(e -> (Metric) e.getValue()).findFirst().orElseThrow();
	}

	@Override
	public CompletableFuture<Void> store(String partitionToken, byte[] key, byte[] data) {
		CompletableFuture<Void> comp = new CompletableFuture<>();
		int partition = Math.abs(partitionToken.hashCode() % KafkaTopicStore.PARTITION_COUNT);
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

		if (metricLimiter.tryAcquire() && recordSendRate.metricValue() instanceof Double sendRate) {
			KafkaMetric metric = new KafkaMetric(physicalTopic, KafkaTopicMetrics.SEND_RATE, sendRate.longValue(),
					ClientEnum.PRODUCER.name());
			VertxPlatform.eventBus().publish("bm.monitoring.fw.kafka.metrics", metric.toJsonObj());
		}
		return comp;
	}

	private KafkaProducer<byte[], byte[]> createKafkaProducer() {
		Config conf = KafkaStoreConfig.get();
		Properties producerProps = new Properties();
		producerProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
		producerProps.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, KafkaTopicStore.COMPRESSION_TYPE);
		producerProps.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
				"org.apache.kafka.common.serialization.ByteArraySerializer");
		producerProps.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
				"org.apache.kafka.common.serialization.ByteArraySerializer");
		producerProps.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, Integer.toString(1));
		producerProps.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");

		// tunables
		producerProps.setProperty(ProducerConfig.ACKS_CONFIG, conf.getString("kafka.producer.acks"));
		producerProps.setProperty(ProducerConfig.LINGER_MS_CONFIG,
				Long.toString(conf.getDuration("kafka.producer.linger", TimeUnit.MILLISECONDS)));
		producerProps.setProperty(ProducerConfig.BUFFER_MEMORY_CONFIG,
				Long.toString(conf.getMemorySize("kafka.producer.bufferMemory").toBytes()));
		producerProps.setProperty(ProducerConfig.BATCH_SIZE_CONFIG,
				Long.toString(conf.getMemorySize("kafka.producer.batchSize").toBytes()));
		producerProps.setProperty(ProducerConfig.MAX_REQUEST_SIZE_CONFIG,
				Long.toString(conf.getMemorySize("kafka.producer.maxRecordSize").toBytes()));

		return new KafkaProducer<>(producerProps);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper("KafkaTopic").add("name", physicalTopic).add("prod", producer).toString();
	}
}
