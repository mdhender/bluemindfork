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

import java.util.Collection;
import java.util.Properties;

import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serdes.ByteArraySerde;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import net.bluemind.central.reverse.proxy.model.common.kafka.InstallationTopics;
import net.bluemind.central.reverse.proxy.model.common.kafka.KafkaAdminClient;
import net.bluemind.central.reverse.proxy.model.common.mapper.RecordKeyMapper;

public class DirEntriesStreamVerticle extends AbstractVerticle {

	private final Logger logger = LoggerFactory.getLogger(DirEntriesStreamVerticle.class);

	private final Config config;
	private final String bootstrapServers;
	private final RecordKeyMapper<byte[]> keyMapper;

	private KafkaAdminClient adminClient;

	public DirEntriesStreamVerticle(Config config, RecordKeyMapper<byte[]> keyMapper) {
		this.config = config;
		this.bootstrapServers = config.getString(BOOTSTRAP_SERVERS);
		this.keyMapper = keyMapper;
	}

	@Override
	public void start(Promise<Void> p) {
		this.adminClient = KafkaAdminClient.create(bootstrapServers);

		logger.info("[stream] Starting");
		adminClient.listTopics() //
				.map(topicNames -> new InstallationTopics(topicNames, config.getString(NAME_SUFFIX))) //
				.compose(this::ensureStreamOutputTopicExists) //
				.map(this::streamDirEntries) //
				.map(this::publishTopics) //
				.onSuccess(v -> logger.info("[stream] Started")) //
				.onFailure(t -> logger.error("[stream] Failed to setup dir entries stream", t));
		p.complete();
	}

	private Future<InstallationTopics> ensureStreamOutputTopicExists(InstallationTopics topics) {
		if (topics.hasCrpTopic) {
			return Future.succeededFuture(topics);
		}
		NewTopic newTopic = new NewTopic(topics.crpTopicName, config.getInt(PARTITION_COUNT),
				config.getNumber(REPLICATION_FACTOR).shortValue());
		newTopic.configs(ImmutableMap.of(//
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
				.filter((key, value) -> keyMapper.map(key).map(recordKey -> recordKey.type.equals("dir")).orElse(false))
				.to(ouputTopicName, withProducer());
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
				(String topic, byte[] key, byte[] value, int numPart) -> toPositive(murmur2(key)) % numPart);
	}

	private InstallationTopics publishTopics(InstallationTopics installationTopics) {
		logger.info("[stream] Announcing dir entries stream ready: {}", JsonObject.mapFrom(installationTopics));
		vertx.eventBus().publish(ADDRESS, JsonObject.mapFrom(installationTopics), STREAM_READY);
		return installationTopics;
	}
}
