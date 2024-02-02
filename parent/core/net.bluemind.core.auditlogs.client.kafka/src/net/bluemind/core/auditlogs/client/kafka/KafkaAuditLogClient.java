package net.bluemind.core.auditlogs.client.kafka;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;

import net.bluemind.core.auditlogs.AuditLogEntry;
import net.bluemind.core.auditlogs.IAuditLogClient;
import net.bluemind.core.auditlogs.client.kafka.config.AuditLogKafkaConfig;
import net.bluemind.core.backup.store.kafka.KafkaTopicStore;
import net.bluemind.core.backup.store.kafka.config.KafkaStoreConfig;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueWriter;

public class KafkaAuditLogClient implements IAuditLogClient {

	private static final Logger logger = LoggerFactory.getLogger(KafkaAuditLogClient.class);
	private static final Map<String, KafkaProducer<byte[], byte[]>> perPhyTopicProd = new ConcurrentHashMap<>();

	private KafkaAuditLogMngt manager;
	private String bootstrap;

	public KafkaAuditLogClient(String bootstrap, KafkaAuditLogMngt manager) {
		this.bootstrap = bootstrap;
		this.manager = manager;
	}

	@Override
	public void storeAuditLog(AuditLogEntry document) {

		String physicalTopic = AuditLogKafkaConfig.getTopic(document.domainUid);

		AuditLogSerializer serializer = new AuditLogSerializer();
		CompletableFuture<Void> comp = new CompletableFuture<>();

		KafkaProducer<byte[], byte[]> producer = perPhyTopicProd.computeIfAbsent(physicalTopic, s -> {
			if (!manager.hasKafkaTopicForDomainUid(document.domainUid)) {
				manager.createKafkaTopic(physicalTopic);
			}
			return createKafkaProducer();
		});
		AuditLogKey key = generateKey(document);
		int partition = Math.abs(key.containerUid().hashCode() % KafkaTopicStore.PARTITION_COUNT);
		byte[] data = serializer.value(document);
		ProducerRecord<byte[], byte[]> rec = new ProducerRecord<>(physicalTopic, partition, serializer.key(key), data);

		producer.send(rec, (RecordMetadata metadata, Exception exception) -> {
			if (exception != null) {
				logger.warn("Could not store {}byte(s) of data. Key: {}, ({})", data == null ? 0 : data.length, key,
						exception.getMessage());
				comp.completeExceptionally(exception);
			} else {
				logger.debug("[{}] stored part: {}, meta: {}", physicalTopic, partition, metadata);
				comp.complete(null);
			}
		});

	}

	private KafkaProducer<byte[], byte[]> createKafkaProducer() {
		Config conf = KafkaStoreConfig.get();
		Properties producerProps = new Properties();
		producerProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
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

	private AuditLogKey generateKey(AuditLogEntry doc) {
		return new AuditLogKey((doc.container == null) ? "__orphan__" : doc.container.uid(),
				(doc.item == null) ? doc.domainUid : doc.item.uid(), System.nanoTime());
	}

	private static class AuditLogSerializer {

		private final ValueWriter keyWriter;
		private final ValueWriter valueWriter;

		public AuditLogSerializer() {
			keyWriter = JsonUtils.writer(AuditLogKey.class);
			valueWriter = JsonUtils.writer(AuditLogEntry.class);
		}

		public byte[] key(AuditLogKey item) {
			return keyWriter.write(item);
		}

		public byte[] value(AuditLogEntry item) {
			return valueWriter.write(item);
		}

	}

}
