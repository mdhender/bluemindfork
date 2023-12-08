package net.bluemind.central.reverse.proxy.stream;

import static net.bluemind.central.reverse.proxy.common.ProxyEventBusAddress.ADDRESS;
import static net.bluemind.central.reverse.proxy.common.ProxyEventBusAddress.STREAM_READY;
import static net.bluemind.central.reverse.proxy.common.config.CrpConfig.Kafka.BOOTSTRAP_SERVERS;
import static net.bluemind.central.reverse.proxy.common.config.CrpConfig.Stream.APPLICATION_ID;
import static net.bluemind.central.reverse.proxy.common.config.CrpConfig.Stream.NUMBER_OF_THREADS;
import static net.bluemind.central.reverse.proxy.common.config.CrpConfig.Topic.CLEANUP_POLICY;
import static net.bluemind.central.reverse.proxy.common.config.CrpConfig.Topic.COMPRESSION_TYPE;
import static net.bluemind.central.reverse.proxy.common.config.CrpConfig.Topic.MAX_COMPACTION_LAG_MS;
import static net.bluemind.central.reverse.proxy.common.config.CrpConfig.Topic.NAME_SUFFIX;
import static net.bluemind.central.reverse.proxy.common.config.CrpConfig.Topic.PARTITION_COUNT;
import static net.bluemind.central.reverse.proxy.common.config.CrpConfig.Topic.REPLICATION_FACTOR;
import static org.apache.kafka.common.utils.Utils.murmur2;
import static org.apache.kafka.common.utils.Utils.toPositive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serdes.ByteArraySerde;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import net.bluemind.central.reverse.proxy.common.config.CrpConfig;
import net.bluemind.central.reverse.proxy.model.common.kafka.InstallationTopics;
import net.bluemind.central.reverse.proxy.model.common.kafka.KafkaAdminClient;
import net.bluemind.central.reverse.proxy.model.common.mapper.RecordKeyMapper;
import net.bluemind.central.reverse.proxy.model.common.mapper.RecordValueMapper;

public class DirEntriesStreamVerticle extends AbstractVerticle {

	private final Logger logger = LoggerFactory.getLogger(DirEntriesStreamVerticle.class);

	private final Config config;
	private final String bootstrapServers;
	private final RecordKeyMapper<byte[]> keyMapper;
	private final RecordValueMapper<byte[]> valueMapper;

	private KafkaAdminClient adminClient;

	public DirEntriesStreamVerticle(Config config, RecordKeyMapper<byte[]> keyMapper,
			RecordValueMapper<byte[]> valueMapper) {
		this.config = config;
		this.bootstrapServers = config.getString(BOOTSTRAP_SERVERS);
		this.keyMapper = keyMapper;
		this.valueMapper = valueMapper;
	}

	@Override
	public void start(Promise<Void> p) {
		this.adminClient = KafkaAdminClient.create(bootstrapServers);

		logger.info("[stream] Starting");
		try (ForestInstancesLoader loader = new ForestInstancesLoader(config)) {
			Set<String> whiteList = loader.whiteListedInstances().stream().map(s -> s.replace("-", ""))
					.collect(Collectors.toSet());
			adminClient.listTopics() //
					.map(fullSet -> onlyWhiteListed(whiteList, fullSet, config))//
					.map(topicNames -> new InstallationTopics(topicNames, config.getString(NAME_SUFFIX))) //
					.compose(this::ensureStreamOutputTopicExists) //
					.map(this::streamDirEntries) //
					.map(this::publishTopics) //
					.onSuccess(v -> logger.info("[stream] Started")) //
					.onFailure(t -> logger.error("[stream] Failed to setup dir entries stream", t));
		}
		p.complete();
	}

	private Set<String> onlyWhiteListed(Set<String> white, Set<String> topics, Config config) {
		String crpSuffix = config.getString(NAME_SUFFIX);
		if (config.getBoolean(CrpConfig.Stream.ENFORCE_FOREST)) {
			return topics.stream().filter(tp -> tp.endsWith(crpSuffix) || white.stream().anyMatch(tp::startsWith))
					.collect(Collectors.toSet());
		} else {
			return topics;
		}

	}

	private Future<InstallationTopics> ensureStreamOutputTopicExists(InstallationTopics topics) {
		if (topics.hasCrpTopic) {
			return Future.succeededFuture(topics);
		}
		NewTopic newTopic = new NewTopic(topics.crpTopicName, config.getInt(PARTITION_COUNT),
				config.getNumber(REPLICATION_FACTOR).shortValue());
		newTopic.configs(Map.of(//
				"compression.type", config.getString(COMPRESSION_TYPE), //
				"cleanup.policy", config.getString(CLEANUP_POLICY), //
				"max.compaction.lag.ms", config.getString(MAX_COMPACTION_LAG_MS)//
		));
		return adminClient.createTopic(newTopic, new CreateTopicsOptions()).map(uuid -> topics);
	}

	private InstallationTopics streamDirEntries(InstallationTopics topics) {
		Properties props = new Properties();
		props.put(StreamsConfig.APPLICATION_ID_CONFIG, config.getString(APPLICATION_ID));
		props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, config.getInt(NUMBER_OF_THREADS));
		props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.ByteArraySerde.class.getName());
		props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.ByteArraySerde.class.getName());

		Collection<String> inputTopicNames = topics.domainTopics.values();
		String ouputTopicName = topics.crpTopicName;

		StreamsBuilder topology = new StreamsBuilder();
		topology //
				.<byte[], byte[]>stream(inputTopicNames) //
				.filter((key, value) -> keyMapper.map(key)
						.map(recordKey -> value != null
								&& (recordKey.type.equals("dir") || recordKey.type.equals("memberships")))
						.orElse(false))
				.flatMap((key, value) -> {
					Collection<KeyValue<byte[], byte[]>> keyValueList = new ArrayList<>(3);

					keyMapper.map(key).filter(recordKey -> recordKey.operation.equals("DELETE"))
							.ifPresent(recordKey -> {
								recordKey.operation = "UPDATE";
								keyMapper.map(recordKey)
										.map(keyAsByteArray -> new KeyValue<byte[], byte[]>(keyAsByteArray, null))
										.ifPresent(keyValueList::add);

								recordKey.operation = "CREATE";
								keyMapper.map(recordKey)
										.map(keyAsByteArray -> new KeyValue<byte[], byte[]>(keyAsByteArray, null))
										.ifPresent(keyValueList::add);

								recordKey.operation = "DELETE";
							});

					keyValueList.add(new KeyValue<>(key, value));
					return keyValueList;
				}).to(ouputTopicName, withProducer());

		KafkaStreams stream = new KafkaStreams(topology.build(), props);
		stream.setUncaughtExceptionHandler((Throwable throwable) -> {
			logger.error("[stream] Exception occurred during stream processing", throwable);
			stream.close();
			return StreamThreadExceptionResponse.REPLACE_THREAD;
		});
		stream.start();
		return topics;
	}

	private Produced<byte[], byte[]> withProducer() {
		return Produced.with(new ByteArraySerde(), new ByteArraySerde(), //
				(String topic, byte[] key, byte[] value, int numPart) -> toPositive(murmur2(partitionId(key, value)))
						% numPart);
	}

	private byte[] partitionId(byte[] key, byte[] value) {
		return keyMapper.map(key).filter(k -> k.owner.equals("system")).map(k -> k.owner.getBytes())
				.orElseGet(() -> partitionIdFromValue(value));
	}

	private byte[] partitionIdFromValue(byte[] value) {
		return valueMapper.getValueUid(value).map(String::getBytes).orElse("default".getBytes());
	}

	private InstallationTopics publishTopics(InstallationTopics installationTopics) {
		logger.info("[stream] Announcing dir entries stream ready: {}", JsonObject.mapFrom(installationTopics));
		// the consumers of this message might not be ready when crp starts
		vertx.setTimer(5000,
				tid -> vertx.eventBus().publish(ADDRESS, JsonObject.mapFrom(installationTopics), STREAM_READY));
		return installationTopics;
	}
}
