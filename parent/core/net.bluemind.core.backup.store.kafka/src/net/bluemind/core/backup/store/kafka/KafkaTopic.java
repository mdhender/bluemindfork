package net.bluemind.core.backup.store.kafka;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.Stopwatch;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import net.bluemind.config.InstallationId;
import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.store.ITopic;
import net.bluemind.core.backup.continuous.store.ITopicStore.IResumeToken;
import net.bluemind.core.backup.continuous.store.ITopicStore.TopicDescriptor;
import net.bluemind.core.utils.JsonUtils;

public class KafkaTopic implements ITopic {

	private static final Logger logger = LoggerFactory.getLogger(KafkaTopicStore.class);

	private static final byte[] EMPTY = new byte[] {};

	private final String bootstrapServer;
	private final String clientId;
	private final TopicDescriptor topicDescriptor;
	private final String physicalTopic;
	private final String partitionKey;
	private final ArrayBlockingQueue<CompletableFuture<Void>> promQueue;
	private final KafkaProducer<byte[], byte[]> producer;

	public KafkaTopic(String bootstrapServer, String clientId, TopicDescriptor td) {
		this.bootstrapServer = bootstrapServer;
		this.clientId = clientId;
		this.topicDescriptor = td;
		this.physicalTopic = td.physicalTopic();
		this.partitionKey = td.partitionKey();
		this.promQueue = new ArrayBlockingQueue<>(250);
		this.producer = createKafkaProducer();
	}

	public TopicDescriptor topicDescriptor() {
		return topicDescriptor;
	}

	@Override
	public CompletableFuture<Void> store(byte[] key, byte[] data) {
		CompletableFuture<Void> comp = new CompletableFuture<>();
		int partition = Math.abs(partitionKey.hashCode()) % KafkaTopicStore.PARTITION_COUNT;
//		byte[] serializedKey = key.serialize();
		ProducerRecord<byte[], byte[]> rec = new ProducerRecord<>(physicalTopic, partition, key, data);

		if (!promQueue.offer(comp)) {
			ArrayList<CompletableFuture<Void>> list = new ArrayList<>(250);
			promQueue.drainTo(list);
			CompletableFuture<Void> all = CompletableFuture.allOf(list.toArray(new CompletableFuture[250]));
			try {
				long time = System.currentTimeMillis();
				all.get(1, TimeUnit.MINUTES);
				time = System.currentTimeMillis() - time;
				logger.info("Checkpointed all producers in {}ms.", time);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
			promQueue.add(comp);
		}

		producer.send(rec, (RecordMetadata metadata, Exception exception) -> {
			if (exception != null) {
				logger.warn(exception.getMessage());
				comp.completeExceptionally(exception);
			} else {
//				if (logger.isDebugEnabled()) {
//					logger.debug("[{}] part: {}, off: {} for id {}.", physicalTopic, metadata.partition(),
//							metadata.offset(), key.id);
//				}
				comp.complete(null);
			}
		});
		return comp;
	}

	@Override
	public IResumeToken subscribe(Handler<DataElement> de) {
		return subscribe(null, de);
	}

	@Override
	public IResumeToken subscribe(IResumeToken index, Handler<DataElement> handler) {
		JsonUtils.reader(RecordKey.class).read(bootstrapServer);
		return subscribe(index, handler, key -> key.match(topicDescriptor));
	}

	@Override
	public IResumeToken subscribe(IResumeToken index, Handler<DataElement> handler, Predicate<RecordKey> keyFilter) {

		Stopwatch timeToFirstRecord = Stopwatch.createStarted();

		try (KafkaConsumer<byte[], byte[]> consumer = createKafkaConsumer()) {
			List<PartitionInfo> parts = consumer.partitionsFor(physicalTopic);
			List<TopicPartition> selfAssign = parts.stream()
					.map(pi -> new TopicPartition(physicalTopic, pi.partition())).collect(Collectors.toList());
			consumer.assign(selfAssign);
			KafkaToken tok = (KafkaToken) index;
			if (tok == null || tok.partitionToOffset.isEmpty()) {
				consumer.seekToBeginning(selfAssign);
			} else {
				tok.partitionToOffset.entrySet().stream().forEach(entry -> {
					TopicPartition tp = new TopicPartition(physicalTopic, entry.getKey());
					long newOffset = entry.getValue() + 1;
					logger.warn("[{}] Seek {} to {}", physicalTopic, entry.getKey(), newOffset);
					consumer.seek(tp, newOffset);
				});
			}

			Map<Integer, Long> offsets = new HashMap<>();
			AtomicLong processed = new AtomicLong();
			HashMap<String, String> interner = new HashMap<>();
			do {
				ConsumerRecords<byte[], byte[]> someRecords = consumer.poll(Duration.ofMillis(200));
				if (someRecords.isEmpty()) {
					break;
				}

				if (timeToFirstRecord.isRunning()) {
					long latency = timeToFirstRecord.elapsed(TimeUnit.MILLISECONDS);
					if (latency > 100) {
						logger.warn("time to first record latency was {}ms.", latency);
					}
					timeToFirstRecord.stop();
				}
				someRecords.forEach(rec -> {
					DataElement de = new DataElement();

					RecordKey key = RecordKey.unserialize(rec.key());
					if (key != null && keyFilter.test(key)) {
						de.key = key;
						de.payload = rec.value();
						if (de.key.id == 0) {
							// silent skip
						} else if (de.payload == null) {
							logger.warn("null payload for {} in {}", de.key.id, rec);
						} else {
							try {
								handler.handle(de);
								processed.incrementAndGet();
							} catch (Exception e) {
								logger.error("handler {} failed", handler, e);
							}
						}
					}
					offsets.put(rec.partition(), rec.offset());
				});
			} while (true);

			return processed.get() > 0 ? new KafkaToken(offsets) : index;
		}
	}

	@Override
	public void delete(long id) {
		// TODO
		// store(id, "*", EMPTY);
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
		Properties cp = new Properties();
		cp.setProperty("bootstrap.servers", bootstrapServer);
		cp.setProperty("client.id", clientId + "_consumer");
		cp.setProperty("group.id", "clone-of-" + InstallationId.getIdentifier());
		cp.setProperty("enable.auto.commit", "true");
		cp.setProperty("fetch.max.wait.ms", "100");
		cp.setProperty("auto.offset.reset", "earliest");
		cp.setProperty("auto.commit.interval.ms", "1000");
		cp.setProperty("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
		cp.setProperty("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
		return new KafkaConsumer<>(cp);
	}

	private KafkaProducer<byte[], byte[]> createKafkaProducer() {
		Properties producerProps = new Properties();
		producerProps.setProperty("bootstrap.servers", bootstrapServer);
		producerProps.setProperty("acks", "all");
		producerProps.setProperty("compression.type", KafkaTopicStore.COMPRESSION_TYPE);
		producerProps.setProperty("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
		producerProps.setProperty("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
		return new KafkaProducer<>(producerProps);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper("KafkaTopic").add("name", physicalTopic).toString();
	}
}
