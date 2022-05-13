package net.bluemind.core.backup.store.kafka;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.netflix.spectator.api.Gauge;
import com.netflix.spectator.api.Registry;

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
import net.bluemind.metrics.registry.IdFactory;

public class KafkaTopicSubscriber implements TopicSubscriber {

	private static final Logger logger = LoggerFactory.getLogger(KafkaTopicSubscriber.class);
	private static final AtomicInteger CONS_ID_ALLOCATOR = new AtomicInteger();

	private final String bootstrapServer;
	private final String topicName;

	private final Registry reg;
	private final IdFactory idFactory;

	public KafkaTopicSubscriber(String bootstrapServer, String topicName, Registry reg, IdFactory idFactory) {
		this.bootstrapServer = bootstrapServer;
		this.topicName = topicName;
		this.reg = reg;
		this.idFactory = idFactory;
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

		ExecutorService pool = Executors.newFixedThreadPool(tok.workers, new DefaultThreadFactory("kafka-clone-pool"));
		CompletableFuture<?>[] proms = new CompletableFuture[tok.workers];
		String group = tok.groupId;

		ParallelStarvationHandler parStrat = new ParallelStarvationHandler(strat, tok.workers);
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
		return tok;
	}

	private long consumeLoop(RecordHandler handler, IRecordStarvationStrategy strat, String gid, String cid) {
		AtomicLong processed = new AtomicLong();
		boolean assigned = false;

		try (KafkaConsumer<byte[], byte[]> consumer = createKafkaConsumer(gid, cid)) {
			consumer.subscribe(Collections.singletonList(topicName));

			do {
				ConsumerRecords<byte[], byte[]> someRecords = consumer.poll(Duration.ofMillis(500));

				if (someRecords.isEmpty()) {
					if (consumer.assignment().isEmpty()) {
						continue;
					} else {
						if (!assigned) {
							logger.info("[{} / {}]  got {} partition(s) assignment(s).", gid, cid,
									consumer.assignment().size());
							assigned = true;
							Map<TopicPartition, Long> endOffsets = consumer.endOffsets(consumer.assignment());
							endOffsets.forEach((tp, end) -> {
								// this is needed for lag evaluation to work
								if (logger.isDebugEnabled()) {
									logger.debug("part {} ends at offset {}", tp.partition(), end);
								}
							});
							continue;
						}
					}

					// avoid false positive on starvations ?
					if (lagValue(consumer) > 0) {
						continue;
					}
					ExpectedBehaviour expected = strat.onStarvation(
							new JsonObject().put("topic", topicName).put("cid", cid).put("records", processed.get()));
					if (expected == ExpectedBehaviour.ABORT) {
						break;
					} else {
						continue;
					}
				} else {
					strat.onRecordsReceived(new JsonObject().put("topic", topicName));
				}
				logger.info("Fresh batch of {} record(s)", someRecords.count());
				someRecords.partitions().forEach(part -> {
					someRecords.records(part).forEach(rec -> {
						try {
							handler.accept(rec.key(), rec.value(), rec.partition(), rec.offset());
							processed.incrementAndGet();
						} catch (Throwable e) {
							logger.error("handler {} failed, SHOULD exit(1)...", handler, e);
						}
					});
				});
				reportLag(gid, cid, consumer);
				consumer.commitAsync();
			} while (true);
			consumer.commitSync();
		}
		return processed.longValue();
	}

	private void reportLag(String gid, String cid, KafkaConsumer<byte[], byte[]> consumer) {
		Gauge gauge = reg.gauge(idFactory.name("lag", "groupAndClient", gid + "-" + cid));
		LongAdder sum = new LongAdder();
		consumer.assignment().forEach(tp -> consumer.currentLag(tp).ifPresent(lag -> {
			if (lag > 0) {
				if (logger.isDebugEnabled()) {
					logger.debug("**** LAG part {} => {}", tp.partition(), lag);
				}
			}
			sum.add(lag);
		}));
		logger.info("**** GLOBAL LAG {}", sum.sum());
		gauge.set(sum.doubleValue());

	}

	private long lagValue(KafkaConsumer<byte[], byte[]> consumer) {
		LongAdder sum = new LongAdder();
		consumer.assignment().forEach(tp -> consumer.currentLag(tp).ifPresent(sum::add));
		return sum.sum();
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
		Properties cp = new Properties();
		cp.setProperty("bootstrap.servers", bootstrapServer);
		cp.setProperty("group.id", group);
		cp.setProperty("client.id", clientId);
		cp.setProperty(ConsumerConfig.METRIC_REPORTER_CLASSES_CONFIG, BluemindMetricsReporter.class.getCanonicalName());
		cp.setProperty("enable.auto.commit", "false");
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
