package net.bluemind.core.backup.store.kafka;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.util.concurrent.RateLimiter;
import com.netflix.spectator.api.Gauge;
import com.netflix.spectator.api.Registry;
import com.typesafe.config.Config;

import io.netty.util.concurrent.DefaultThreadFactory;
import io.vertx.core.json.JsonObject;
import net.bluemind.config.InstallationId;
import net.bluemind.core.backup.continuous.IRecordStarvationStrategy;
import net.bluemind.core.backup.continuous.IRecordStarvationStrategy.ExpectedBehaviour;
import net.bluemind.core.backup.continuous.RecordStarvationStrategies;
import net.bluemind.core.backup.continuous.api.CloneDefaults;
import net.bluemind.core.backup.continuous.store.ITopicStore.IResumeToken;
import net.bluemind.core.backup.continuous.store.RecordHandler;
import net.bluemind.core.backup.continuous.store.TopicSubscriber;
import net.bluemind.core.backup.store.kafka.config.KafkaStoreConfig;
import net.bluemind.core.backup.store.kafka.config.KafkaStoreConfig.PoisonPillStrategy;
import net.bluemind.core.backup.store.kafka.metrics.KafkaTopicMetrics;
import net.bluemind.metrics.registry.IdFactory;

public class KafkaTopicSubscriber implements TopicSubscriber {

	private static final Logger logger = LoggerFactory.getLogger(KafkaTopicSubscriber.class);
	private static final AtomicInteger CONS_ID_ALLOCATOR = new AtomicInteger();

	private final String bootstrapServer;
	private final String topicName;

	private final Registry reg;
	private final IdFactory idFactory;
	private RateLimiter lagReportRateLimiter;

	public KafkaTopicSubscriber(String bootstrapServer, String topicName, Registry reg, IdFactory idFactory) {
		this.bootstrapServer = bootstrapServer;
		this.topicName = topicName;
		this.reg = reg;
		this.idFactory = idFactory;
		this.lagReportRateLimiter = RateLimiter.create(0.5);
	}

	public String topicName() {
		return topicName;
	}

	@Override
	public IResumeToken subscribe(RecordHandler de) {
		return subscribe(null, de);
	}

	@Override
	public IResumeToken subscribe(IResumeToken index, RecordHandler handler) {
		return subscribe(index, handler, RecordStarvationStrategies.EARLY_ABORT);
	}

	@Override
	public IResumeToken subscribe(IResumeToken index, RecordHandler handler, IRecordStarvationStrategy strat) {
		KafkaToken tok = (KafkaToken) index;
		if (tok == null) {
			String splitProp = System.getProperty(CloneDefaults.WORKERS_SYSPROP,
					"" + Math.max(4, Runtime.getRuntime().availableProcessors() - 2));
			int split = Integer.parseInt(splitProp);
			tok = new KafkaToken("clone-" + UUID.randomUUID().toString().replace("-", "") + "-of-"
					+ InstallationId.getIdentifier().replace("bluemind-", ""), split);
		}
		// ensure tail-mode knows our consumer group id if interrupted
		strat.checkpoint(topicName, tok);

		ExecutorService pool = Executors.newFixedThreadPool(tok.workers,
				new DefaultThreadFactory("clone-p-" + topicName));
		CompletableFuture<?>[] proms = new CompletableFuture[tok.workers];
		String group = tok.groupId;

		ParallelStarvationHandler parStrat = new ParallelStarvationHandler(strat, tok.workers, getEndOffsets(group));
		int consumerId = CONS_ID_ALLOCATOR.incrementAndGet();
		for (int i = 0; i < tok.workers; i++) {
			final int idx = i;
			String client = "cons-" + consumerId + "-client-" + idx;
			proms[i] = CompletableFuture.<Long>supplyAsync(() -> {
				logger.info("Starting {} for topic {}", client, topicName);
				return consumeLoop(handler, parStrat, group, client);
			}, pool);
		}
		CompletableFuture.allOf(proms).join();
		pool.shutdown();
		logger.info("[{}] ending subscribe loop", topicName);
		return tok;
	}

	private Map<Integer, Long> getEndOffsets(String group) {
		try (KafkaConsumer<byte[], byte[]> consumer = createKafkaConsumer(group,
				"cons-" + CONS_ID_ALLOCATOR.incrementAndGet() + "-getendoffset")) {
			List<TopicPartition> partitions = consumer.partitionsFor(topicName).stream()
					.map(p -> new TopicPartition(topicName, p.partition())).toList();
			consumer.assign(partitions);
			do {
				consumer.poll(Duration.ofMillis(100));
			} while (consumer.assignment().isEmpty());

			return consumer.endOffsets(partitions).entrySet().stream()
					.collect(Collectors.toMap(es -> es.getKey().partition(), Entry::getValue));
		}
	}

	public static class SetCurrentOffsetsOnPartitionAssigned implements ConsumerRebalanceListener {
		private Consumer<?, ?> consumer;
		private IRecordStarvationStrategy strat;

		public SetCurrentOffsetsOnPartitionAssigned(Consumer<?, ?> consumer, IRecordStarvationStrategy strat) {
			this.consumer = consumer;
			this.strat = strat;
		}

		@Override
		public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
			// Nothing to do
		}

		@Override
		public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
			strat.updateOffsets(consumer.committed(new HashSet<>(partitions)).entrySet().stream().collect(Collectors
					.toMap(es -> es.getKey().partition(), es -> es.getValue() != null ? es.getValue().offset() : 0L)));
		}
	}

	private long consumeLoop(RecordHandler handler, IRecordStarvationStrategy strat, String gid, String cid) {
		AtomicLong processed = new AtomicLong();

		try (KafkaConsumer<byte[], byte[]> consumer = createKafkaConsumer(gid, cid)) {
			SetCurrentOffsetsOnPartitionAssigned onPartitionReassigned = new SetCurrentOffsetsOnPartitionAssigned(
					consumer, strat);

			consumer.subscribe(Collections.singletonList(topicName), onPartitionReassigned);
			JsonObject recJs = new JsonObject().put("topic", topicName);
			do {
				ConsumerRecords<byte[], byte[]> someRecords = consumer.poll(Duration.ofMillis(500));
				strat.onRecordsReceived(recJs.put("records", someRecords.count()));
				strat.updateOffsets(someRecords.partitions().stream()
						.collect(Collectors.toMap(tp -> tp.partition(), consumer::position)));

				if (lagReportRateLimiter.tryAcquire()) {
					reportLag(gid, cid, consumer);
				}

				if (!someRecords.isEmpty()) {
					if (logger.isDebugEnabled()) {
						logger.debug("{}: {} record(s)", topicName, someRecords.count());
					}
					processRecords(handler, consumer, processed, someRecords);
				}
				if (someRecords.isEmpty() || strat.isTopicFinished()) {
					if (consumer.assignment().isEmpty()) {
						continue;
					}

					ExpectedBehaviour expected = ExpectedBehaviour.RETRY;
					JsonObject starvationInfo = new JsonObject().put("topic", topicName).put("cid", cid).put("records",
							processed.get());
					expected = strat.onStarvation(starvationInfo);
					if (expected == ExpectedBehaviour.ABORT) {
						break;
					} else {
						continue;
					}
				}
				consumer.commitAsync();
			} while (true);
			consumer.commitSync();
			reportLag(gid, cid, consumer);
		}
		return processed.longValue();
	}

	private void processRecords(RecordHandler handler, KafkaConsumer<byte[], byte[]> consumer, AtomicLong processed,
			ConsumerRecords<byte[], byte[]> someRecords) {
		long lastProcessedOffset = 0L;
		for (TopicPartition part : someRecords.partitions()) {
			for (ConsumerRecord<byte[], byte[]> rec : someRecords.records(part)) {
				try {
					handler.accept(rec.key(), rec.value(), rec.partition(), rec.offset());
					lastProcessedOffset = rec.offset();
					processed.incrementAndGet();
				} catch (Exception e) {
					PoisonPillStrategy strat = KafkaStoreConfig.get().getEnum(PoisonPillStrategy.class,
							"kafka.consumer.poisonPillStrategy");
					logger.error("[part {} - offset {}] handler {} failed, strategy is {}", rec.partition(),
							rec.offset(), handler, strat, e);
					strat.apply(rec.value(), e);
					// We failed, commit the last processed offset to avoid processing again already
					// processed events in the batch
					consumer.commitSync(Map.of(part, new OffsetAndMetadata(lastProcessedOffset,
							"handler has failed to process: " + e.getMessage())));
					throw e;
				}
			}
		}
	}

	private void reportLag(String gid, String cid, KafkaConsumer<byte[], byte[]> consumer) {
		String id = gid + "-" + cid;
		Gauge gauge = reg.gauge(idFactory.name("lag", "groupAndClient", id));
		LongAdder sum = new LongAdder();
		consumer.assignment().forEach(tp -> consumer.currentLag(tp).ifPresent(lag -> {
			if (lag > 0 && logger.isDebugEnabled()) {
				logger.debug("**** LAG part {} => {}", tp.partition(), lag);
			}
			sum.add(lag);
		}));
		gauge.set(sum.doubleValue());
		KafkaTopicMetrics.get().addConsumerMetric(id, KafkaTopicMetrics.LAG, sum.sum());
	}

	@Override
	public IResumeToken parseToken(JsonObject js) {
		return new KafkaToken(js.getString("group"), js.getInteger("workers", 4).intValue());
	}

	private KafkaConsumer<byte[], byte[]> createKafkaConsumer(String group, String clientId) {
		if (logger.isDebugEnabled()) {
			logger.debug("bootstrap: {}, clientId: {}, inst: {}", bootstrapServer, clientId,
					InstallationId.getIdentifier());
		}
		// https://stackoverflow.com/questions/37363119/kafka-producer-org-apache-kafka-common-serialization-stringserializer-could-no#:~:text=instance%20like%20this-,Thread.currentThread().setContextClassLoader(null)%3B%0AProducer%3CString%2C%20String%3E%20producer%20%3D%20new%20KafkaProducer(props)%3B,-hope%20my%20answer
		ClassLoader savedCl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(null);
		Properties cp = new Properties();
		Config conf = KafkaStoreConfig.get();
		cp.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
		cp.setProperty(ConsumerConfig.GROUP_ID_CONFIG, group);
		cp.setProperty(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
		cp.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getCanonicalName());
		cp.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getCanonicalName());
		cp.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
		cp.setProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
		cp.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		cp.setProperty(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG,
				Long.toString(conf.getDuration("kafka.consumer.fetchMaxWait", TimeUnit.MILLISECONDS)));
		cp.setProperty(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,
				Long.toString(conf.getDuration("kafka.consumer.maxPollInterval", TimeUnit.MILLISECONDS)));
		if (conf.hasPath("kafka.consumer.clientRack")) {
			cp.setProperty(ConsumerConfig.CLIENT_RACK_CONFIG, conf.getString("kafka.consumer.clientRack"));
		}
		KafkaConsumer<byte[], byte[]> ret = new KafkaConsumer<>(cp);
		Thread.currentThread().setContextClassLoader(savedCl);
		return ret;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper("KafkaTopicSubscriber").add("name", topicName).toString();
	}
}
