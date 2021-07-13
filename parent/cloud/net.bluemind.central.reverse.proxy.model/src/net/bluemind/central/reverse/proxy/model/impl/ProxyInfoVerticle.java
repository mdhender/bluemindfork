package net.bluemind.central.reverse.proxy.model.impl;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import net.bluemind.central.reverse.proxy.kafka.KafkaAdminClient;
import net.bluemind.central.reverse.proxy.kafka.KafkaConsumerClient;
import net.bluemind.central.reverse.proxy.model.InstallationTopics;
import net.bluemind.central.reverse.proxy.model.ProxyInfoStore;
import net.bluemind.central.reverse.proxy.model.RecordHandler;

public class ProxyInfoVerticle extends AbstractVerticle {

	private final Logger logger = LoggerFactory.getLogger(ProxyInfoVerticle.class);

	private static final String CONSUMER_GROUP_NAME = "crp-consumer-group-" + UUID.randomUUID();

	private final ProxyInfoStore store;
	private final RecordHandler<byte[], byte[]> recordHandler;

	private List<KafkaConsumerClient<byte[], byte[]>> consumers = new ArrayList<>();

	public ProxyInfoVerticle(ProxyInfoStore store, RecordHandler<byte[], byte[]> recordHandler) {
		this.store = store;
		this.recordHandler = recordHandler;
	}

	@Override
	public void start(Promise<Void> p) {
		setupStore();
		String bootstrapServers = kafkaBootstrapServers()
				.orElseThrow(() -> new RuntimeException("No configuration available for kafka bootstrap server"));
		KafkaAdminClient adminClient = KafkaAdminClient.create(bootstrapServers);
//		Context context = vertx.getOrCreateContext();
		adminClient.listTopics().map(InstallationTopics::new).onSuccess(installationTopics -> {
//			context.runOnContext((v) -> {
			logger.info("Subscribing to {}", installationTopics.domainTopics);
			setupConsumer(bootstrapServers, installationTopics);
			p.complete();
//			});
		}).onFailure(t -> logger.error("Unable to list installation topic names", t));
	}

	private void setupStore() {
		store.setup();
	}

	private void setupConsumer(String bootstrapServers, InstallationTopics topics) {
		List<String> topicNames = topicNamesToConsume(topics);
		for (int i = 0; i < 8; i++) {
			String groupIdInstance = "consumer-" + i;
			KafkaConsumerClient<byte[], byte[]> consumer = createConsumer(bootstrapServers, groupIdInstance);
			consumers.add(consumer);
			consumer.handler(recordHandler).subscribe(topicNames);
			logger.info("consumer {} subscibed to {}", groupIdInstance, topicNames);
		}
	}

	private KafkaConsumerClient<byte[], byte[]> createConsumer(String bootstrapServers, String groupInstanceId) {
		Properties props = new Properties();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
		props.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP_NAME);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

		return KafkaConsumerClient.create(vertx, props);
	}

	private List<String> topicNamesToConsume(InstallationTopics topics) {
		List<String> topicNames = new ArrayList<>(topics.domainTopics.values());
		topicNames.add(topics.orphans);
		return topicNames;
	}

	private Optional<String> kafkaBootstrapServers() {
		String bootstrapServer = System.getProperty("bm.kafka.bootstrap.servers");
		if (bootstrapServer != null) {
			return Optional.of(bootstrapServer);
		}

		File local = new File("/etc/bm/kafka.properties");
		if (!local.exists()) {
			local = new File(System.getProperty("user.home") + "/kafka.properties");
		}
		if (local.exists()) {
			Properties tmp = new Properties();
			try (InputStream in = Files.newInputStream(local.toPath())) {
				tmp.load(in);
				bootstrapServer = tmp.getProperty("bootstrap.servers");
			} catch (Exception e) {
				logger.warn(e.getMessage());
			}
		}
		return Optional.ofNullable(bootstrapServer);
	}
}
