package net.bluemind.central.reverse.proxy.model.impl;

import static net.bluemind.central.reverse.proxy.common.ProxyEventBusAddress.ADDRESS;
import static net.bluemind.central.reverse.proxy.common.ProxyEventBusAddress.STREAM_READY_NAME;
import static net.bluemind.central.reverse.proxy.common.config.CrpConfig.Kafka.BOOTSTRAP_SERVERS;
import static net.bluemind.central.reverse.proxy.common.config.CrpConfig.Model.CLIENT_ID_PREFIX;
import static net.bluemind.central.reverse.proxy.common.config.CrpConfig.Model.CONSUMER_GROUP_PREFIX;
import static net.bluemind.central.reverse.proxy.common.config.CrpConfig.Model.NUMBER_OF_CONSUMER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import net.bluemind.central.reverse.proxy.common.ProxyEventBusAddress;
import net.bluemind.central.reverse.proxy.model.PostfixMapsStore;
import net.bluemind.central.reverse.proxy.model.ProxyInfoStore;
import net.bluemind.central.reverse.proxy.model.RecordHandler;
import net.bluemind.central.reverse.proxy.model.client.PostfixMapsStoreClient;
import net.bluemind.central.reverse.proxy.model.client.ProxyInfoStoreClient;
import net.bluemind.central.reverse.proxy.model.common.kafka.InstallationTopics;
import net.bluemind.central.reverse.proxy.model.common.kafka.KafkaConsumerClient;

public class ProxyInfoVerticle extends AbstractVerticle {

	private final Logger logger = LoggerFactory.getLogger(ProxyInfoVerticle.class);

	private final Config config;
	private final String bootstrapServers;
	private RecordHandler<byte[], byte[]> recordHandler;

	private List<KafkaConsumerClient<byte[], byte[]>> kafkaConsumers = new ArrayList<>();

	private ProxyInfoStore proxyInfoStore;
	private PostfixMapsStore postfixMapsStore;

	private final Supplier<PostfixMapsStore> postfixMapsStoreSupplier;
	private final Supplier<ProxyInfoStore> proxyInfoStoreSupplier;

	public ProxyInfoVerticle(Config config, Supplier<ProxyInfoStore> proxyInfoStoreSupplier,
			Supplier<PostfixMapsStore> postfixMapsStoreSupplier) {
		this.config = config;
		this.bootstrapServers = config.getString(BOOTSTRAP_SERVERS);

		this.proxyInfoStoreSupplier = proxyInfoStoreSupplier;
		this.postfixMapsStoreSupplier = postfixMapsStoreSupplier;
	}

	@Override
	public void start(Promise<Void> p) {
		ProxyInfoStoreClient proxyInfoStoreClient = ProxyInfoStoreClient.create(vertx);
		PostfixMapsStoreClient postfixMapsStoreClient = PostfixMapsStoreClient.create(vertx);

		recordHandler = RecordHandler.createByteHandler(proxyInfoStoreClient, postfixMapsStoreClient, vertx);

		logger.info("[model] Starting");
		vertx.eventBus().<JsonObject>consumer(ADDRESS).handler(event -> {
			if (STREAM_READY_NAME.equals(event.headers().get("action"))) {
				logger.info("[model] Dir entries stream ready, starting model");
				proxyInfoStore = proxyInfoStoreSupplier.get().setupService(vertx);
				postfixMapsStore = postfixMapsStoreSupplier.get().setupService(vertx);
				InstallationTopics topics = event.body().mapTo(InstallationTopics.class);
				startKafkaConsumption(topics) //
						.onSuccess(v -> logger.info("[model] Started")) //
						.onFailure(t -> logger.error("[model] Failed to start model", t));
			}
		});
		p.complete();
	}

	private Future<Void> startKafkaConsumption(InstallationTopics topics) {
		return deployKafkaConsumer(topics).map(this::publishTopics).mapEmpty();

	}

	private Future<InstallationTopics> deployKafkaConsumer(InstallationTopics installationTopics) {
		List<String> topicNames = topicNamesToConsume(installationTopics);
		AtomicInteger cidAlloc = new AtomicInteger();
		Map<String, Promise<Void>> clientCompletionPromises = new HashMap<>();

		String consumerGroupName = config.getString(CONSUMER_GROUP_PREFIX) + "-" + UUID.randomUUID();
		Future<String> deploy = vertx.deployVerticle(() -> new AbstractVerticle() {
			@Override
			public void start() throws Exception {
				String clientId = config.getString(CLIENT_ID_PREFIX) + "-" + cidAlloc.incrementAndGet();
				Promise<Void> clientCompletionPromise = Promise.promise();
				clientCompletionPromises.put(clientId, clientCompletionPromise);
				KafkaConsumerClient<byte[], byte[]> consumer = createConsumer(consumerGroupName, clientId);
				kafkaConsumers.add(consumer);
				consumer.handler(recordHandler) //
						.subscribe(topicNames) //
						.onSuccess(v -> clientCompletionPromises.get(clientId).complete());
			}
		}, new DeploymentOptions().setInstances(config.getInt(NUMBER_OF_CONSUMER)));
		return deploy.flatMap(dep -> {
			List<Future<Void>> clientCompletionFutures = clientCompletionPromises.values().stream() //
					.map(Promise::future) //
					.collect(Collectors.toList());
			return Future.all(clientCompletionFutures).map(v -> installationTopics);
		});
	}

	private InstallationTopics publishTopics(InstallationTopics installationTopics) {
		logger.info("[model] Announcing model ready");
		vertx.setTimer(5000, tid -> vertx.eventBus().publish(ProxyEventBusAddress.ADDRESS,
				JsonObject.mapFrom(installationTopics), ProxyEventBusAddress.MODEL_READY));
		return installationTopics;
	}

	private KafkaConsumerClient<byte[], byte[]> createConsumer(String groupInstanceId, String cid) {
		Properties props = new Properties();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
		props.put(ConsumerConfig.GROUP_ID_CONFIG, groupInstanceId);
		props.put(ConsumerConfig.CLIENT_ID_CONFIG, cid);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

		return KafkaConsumerClient.create(vertx, props);
	}

	private List<String> topicNamesToConsume(InstallationTopics topics) {
		List<String> topicNames = new ArrayList<>();
		topicNames.add(topics.crpTopicName);
		topicNames.add(topics.orphans);
		return topicNames;
	}

	public void tearDown() throws InterruptedException, ExecutionException {
		if (proxyInfoStore != null) {
			proxyInfoStore.tearDown();
		}

		if (postfixMapsStore != null) {
			postfixMapsStore.tearDown();
		}
	}
}
