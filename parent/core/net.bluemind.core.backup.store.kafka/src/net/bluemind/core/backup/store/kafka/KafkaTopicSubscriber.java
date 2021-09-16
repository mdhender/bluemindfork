package net.bluemind.core.backup.store.kafka;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.Stopwatch;

import io.vertx.core.json.JsonObject;
import net.bluemind.config.InstallationId;
import net.bluemind.core.backup.continuous.IRecordStarvationStrategy;
import net.bluemind.core.backup.continuous.IRecordStarvationStrategy.ExpectedBehaviour;
import net.bluemind.core.backup.continuous.RecordStarvationStrategies;
import net.bluemind.core.backup.continuous.store.ITopicStore.IResumeToken;
import net.bluemind.core.backup.continuous.store.TopicSubscriber;

public class KafkaTopicSubscriber implements TopicSubscriber {

	private static final Logger logger = LoggerFactory.getLogger(KafkaTopicSubscriber.class);
	private static final AtomicInteger subAlloc = new AtomicInteger();

	private final String bootstrapServer;
	private final String clientId;
	private final String topicName;

	public KafkaTopicSubscriber(String bootstrapServer, String clientId, String topicName) {
		this.bootstrapServer = bootstrapServer;
		this.clientId = clientId;
		this.topicName = topicName;
	}

	public String topicName() {
		return topicName;
	}

	@Override
	public IResumeToken subscribe(BiConsumer<byte[], byte[]> de) {
		return subscribe(null, de);
	}

	@Override
	public IResumeToken subscribe(IResumeToken index, BiConsumer<byte[], byte[]> handler) {
		return subscribe(index, handler, RecordStarvationStrategies.EARLY_ABORT);
	}

	@Override
	public IResumeToken subscribe(IResumeToken index, BiConsumer<byte[], byte[]> handler,
			IRecordStarvationStrategy strat) {
		Stopwatch timeToFirstRecord = Stopwatch.createStarted();

		try (KafkaConsumer<byte[], byte[]> consumer = createKafkaConsumer()) {
			List<PartitionInfo> parts = consumer.partitionsFor(topicName);
			List<TopicPartition> selfAssign = parts.stream().map(pi -> new TopicPartition(topicName, pi.partition()))
					.collect(Collectors.toList());
			consumer.assign(selfAssign);
			KafkaToken tok = (KafkaToken) index;
			if (tok == null || tok.partitionToOffset.isEmpty()) {
				consumer.seekToBeginning(selfAssign);
			} else {
				tok.partitionToOffset.entrySet().stream().forEach(entry -> {
					TopicPartition tp = new TopicPartition(topicName, entry.getKey());
					long newOffset = entry.getValue() + 1;
					logger.warn("[{}] Seek {} to {}", topicName, entry.getKey(), newOffset);
					consumer.seek(tp, newOffset);
				});
			}

			Map<Integer, Long> offsets = new HashMap<>();
			AtomicLong processed = new AtomicLong();
			do {
				ConsumerRecords<byte[], byte[]> someRecords = consumer.poll(Duration.ofMillis(500));
				if (someRecords.isEmpty()) {
					ExpectedBehaviour expected = strat.onStarvation(new JsonObject().put("topic", topicName));
					if (expected == ExpectedBehaviour.ABORT) {
						break;
					} else {
						continue;
					}
				}

				if (timeToFirstRecord.isRunning()) {
					long latency = timeToFirstRecord.elapsed(TimeUnit.MILLISECONDS);
					if (latency > 300) {
						logger.warn("time to first record latency was {}ms.", latency);
					}
					timeToFirstRecord.stop();
				}
				someRecords.forEach(rec -> {
					try {
						handler.accept(rec.key(), rec.value());
						processed.incrementAndGet();
					} catch (Exception e) {
						logger.error("handler {} failed, SHOULD exit(1)...", handler, e);
					}

					offsets.put(rec.partition(), rec.offset());
				});
			} while (true);

			return processed.get() > 0 ? new KafkaToken(offsets) : index;
		}
	}

	@Override
	public IResumeToken parseToken(JsonObject js) {
		Map<Integer, Long> partOffsets = new HashMap<>();
		for (String k : js.fieldNames()) {
			Integer partition = Integer.parseInt(k);
			Long offset = js.getLong(k);
			partOffsets.put(partition, offset);
		}
		return new KafkaToken(partOffsets);
	}

	private KafkaConsumer<byte[], byte[]> createKafkaConsumer() {
		logger.warn("bootstrap: {}, clientId: {}, inst: {}", bootstrapServer, clientId, InstallationId.getIdentifier());
		Properties cp = new Properties();
		cp.setProperty("bootstrap.servers", bootstrapServer);
		cp.setProperty("client.id", clientId + "_" + subAlloc.incrementAndGet());
		cp.setProperty("group.id", "clone-of-" + InstallationId.getIdentifier());
		cp.setProperty(ConsumerConfig.METRIC_REPORTER_CLASSES_CONFIG, BluemindMetricsReporter.class.getCanonicalName());
		cp.setProperty("enable.auto.commit", "true");
		cp.setProperty("fetch.max.wait.ms", "100");
		cp.setProperty("auto.offset.reset", "earliest");
		cp.setProperty("auto.commit.interval.ms", "1000");
		cp.setProperty("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
		cp.setProperty("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
		return new KafkaConsumer<>(cp);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper("KafkaTopicSubscriber").add("name", topicName).toString();
	}
}
