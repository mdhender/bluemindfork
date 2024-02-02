package net.bluemind.core.auditlogs.client.kafka;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;

import io.vertx.core.Handler;
import net.bluemind.config.InstallationId;
import net.bluemind.core.backup.continuous.store.RecordHandler;
import net.bluemind.core.backup.store.kafka.KafkaTopicStore;

public class KafkaAuditLogConsumer {
	private static final AtomicInteger CONS_ID_ALLOCATOR = new AtomicInteger();
	private static final Duration POLL_DURATION = Duration.ofMillis(250);

	private final String bootstrap;
	private final AuditLogDeserializer deserializer;

	public KafkaAuditLogConsumer(String bootstrap) {
		this.bootstrap = bootstrap;
		deserializer = new AuditLogDeserializer();
	}

	public void consume(String domainUid, String containerUid, Handler<AuditLogDataElement> handler) {
		String physicalTopic = InstallationId.getIdentifier() + "-" + domainUid + "_audit";
		int consumerId = CONS_ID_ALLOCATOR.incrementAndGet();
		String client = "cons-audit-" + consumerId + "-client";

		int containerPartition = Math.abs(containerUid.hashCode() % KafkaTopicStore.PARTITION_COUNT);

		try (KafkaConsumer<byte[], byte[]> cons = createConsumer(client)) {

			TopicPartition partitionForContainerUid = new TopicPartition(physicalTopic, containerPartition);
			List<TopicPartition> parts = List.of(partitionForContainerUid);
			cons.assign(parts);
			long endOffset = Optional.ofNullable(cons.endOffsets(parts).get(partitionForContainerUid)).orElse(0L);
			if (endOffset == 0) {
				return;
			}
			long lastSeenOffset = -1;
			while (lastSeenOffset + 1 < endOffset) {
				final ConsumerRecords<byte[], byte[]> records = cons.poll(POLL_DURATION);
				lastSeenOffset = processRecords(deserialize(handler), partitionForContainerUid, records);
			}
		}
	}

	private long processRecords(RecordHandler handler, TopicPartition part,
			ConsumerRecords<byte[], byte[]> someRecords) {
		long lastOffset = 0;
		for (ConsumerRecord<byte[], byte[]> rec : someRecords.records(part)) {
			handler.accept(rec.key(), rec.value(), rec.partition(), rec.offset());
			lastOffset = rec.offset();
		}
		return lastOffset;
	}

	private KafkaConsumer<byte[], byte[]> createConsumer(String cid) {
		Properties props = new Properties();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
		props.put(ConsumerConfig.CLIENT_ID_CONFIG, cid);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, cid);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

		return new KafkaConsumer<>(props);
	}

	private RecordHandler deserialize(Handler<AuditLogDataElement> handler) {
		return (keyBytes, valueBytes, part, offset) -> {
			AuditLogKey key = deserializer.key(keyBytes);
			AuditLogDataElement de = new AuditLogDataElement(key, valueBytes, part, offset);
			handler.handle(de);
		};
	}

}
