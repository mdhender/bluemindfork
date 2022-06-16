package net.bluemind.central.reverse.proxy.model.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.central.reverse.proxy.common.config.CrpConfig;
import net.bluemind.central.reverse.proxy.model.ProxyInfoStorage;
import net.bluemind.central.reverse.proxy.model.ProxyInfoStore;
import net.bluemind.central.reverse.proxy.model.RecordHandler;
import net.bluemind.central.reverse.proxy.model.client.ProxyInfoStoreClient;
import net.bluemind.central.reverse.proxy.model.common.mapper.RecordKeyMapper;
import net.bluemind.central.reverse.proxy.stream.DirEntriesStreamVerticle;
import net.bluemind.kafka.container.ZkKafkaContainer;
import net.bluemind.lib.vertx.VertxPlatform;

public class ProxyInfoVerticleTests {
	private final Logger logger = LoggerFactory.getLogger(ProxyInfoVerticleTests.class);

	private static final String ORPHANS_TOPIC_NAME = "xyz-__orphans__";
	private static final String DOMAIN_TOPIC_NAME = "xyz-193e9e7d.internal";

	private ZkKafkaContainer kafka;
	private String bootstrapServers;
	private ProxyInfoStore store;

	@Before
	public void setup() {
		this.kafka = new ZkKafkaContainer();
		kafka.start();
		String ip = kafka.inspectAddress();
		this.bootstrapServers = ip + ":9093";
		System.setProperty("bm.kafka.bootstrap.servers", bootstrapServers);
		System.setProperty("bm.zk.servers", ip + ":2181");
	}

	@After
	public void teardown() {
		kafka.stop();
		if (store != null) {
			store.tearDown();
		}
	}

	@Test
	public void testConsumeTopics() throws Throwable {
		Vertx vertx = VertxPlatform.getVertx();
		int numberOfRecords = 10;

		NewTopic orphansTopic = new NewTopic(ORPHANS_TOPIC_NAME, 4, (short) 1);
		NewTopic domainTopic = new NewTopic(DOMAIN_TOPIC_NAME, 4, (short) 1);

		AsyncTestContext.asyncTest(context -> {
			createTopics(orphansTopic, domainTopic).onSuccess(v -> {
				ProxyInfoStorage storage = spy(ProxyInfoStorage.create());
				ProxyInfoVerticle modelVerticle = createModelVerticle(vertx, storage);
				DirEntriesStreamVerticle streamVerticle = createStreamVerticle();
				vertx.deployVerticle(streamVerticle, new DeploymentOptions().setWorker(true), ar -> {
					vertx.deployVerticle(modelVerticle, new DeploymentOptions().setWorker(true), ar2 -> {
						Producer<byte[], byte[]> producer = createProducer(vertx, bootstrapServers);
						producer.send(createDomain());
						sendToKafka(producer, numberOfRecords);
						context.sleep(1, TimeUnit.SECONDS);
						sendToKafka(producer, numberOfRecords);

						context.assertions(() -> {
							ArgumentCaptor<String> dataLocation = ArgumentCaptor.forClass(String.class);
							ArgumentCaptor<String> ip = ArgumentCaptor.forClass(String.class);
							verify(storage, timeout(25000).times(numberOfRecords))
									.addDataLocation(dataLocation.capture(), ip.capture());
							assertTrue(dataLocation.getAllValues().containsAll(Arrays.asList("0", "1", "2", "3", "4")));
							assertTrue(ip.getAllValues().containsAll(Arrays.asList("0", "1", "2", "3", "4")));
							assertArrayEquals(dataLocation.getAllValues().toArray(), ip.getAllValues().toArray());

							ArgumentCaptor<String> login = ArgumentCaptor.forClass(String.class);
							ArgumentCaptor<String> dataLocation2 = ArgumentCaptor.forClass(String.class);
							verify(storage, timeout(25000).times(numberOfRecords)).addLogin(login.capture(),
									dataLocation2.capture());
							assertTrue(login.getAllValues().containsAll(Arrays.asList("5", "6", "7", "8", "9")));
							assertTrue(
									dataLocation2.getAllValues().containsAll(Arrays.asList("5", "6", "7", "8", "9")));
							assertArrayEquals(dataLocation2.getAllValues().toArray(), login.getAllValues().toArray());
						});
					});
				});
			});
		});
	}

	private Future<Void> createTopics(NewTopic... topics) {
		Promise<Void> p = Promise.promise();
		Properties props = new Properties();
		props.put("bootstrap.servers", this.bootstrapServers);
		AdminClient.create(props).createTopics(Arrays.asList(topics)).all().whenComplete((v, t) -> p.complete());
		return p.future();
	}

	private Producer<byte[], byte[]> createProducer(Vertx vertx, String bootstrapServers) {
		Properties props = new Properties();
		props.put("bootstrap.servers", bootstrapServers);
		props.put("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
		props.put("acks", "1");

		return new KafkaProducer<>(props);
	}

	private ProxyInfoVerticle createModelVerticle(Vertx vertx, ProxyInfoStorage storage) {
		store = ProxyInfoStore.create(vertx, storage);
		ProxyInfoStoreClient storeClient = ProxyInfoStoreClient.create(vertx);
		RecordHandler<byte[], byte[]> recordHandler = RecordHandler.createByteHandler(storeClient, vertx);

		Config config = CrpConfig.get("model", ProxyInfoVerticle.class.getClassLoader());
		return new ProxyInfoVerticle(config, store, recordHandler);
	}

	private DirEntriesStreamVerticle createStreamVerticle() {
		Config config = CrpConfig.get("stream", DirEntriesStreamVerticle.class.getClassLoader());
		return new DirEntriesStreamVerticle(config, RecordKeyMapper.byteArray());
	}

	private void sendToKafka(Producer<byte[], byte[]> producer, int numberOfRecords) {
		for (int i = 0; i < numberOfRecords; i++) {
			String indice = String.valueOf(i);
			ProducerRecord<byte[], byte[]> producerRecord = (i < numberOfRecords / 2)
					? createInstallation(indice, indice)
					: createDir(indice, indice);
			producer.send(producerRecord);
		}
		producer.flush();
	}

	private ProducerRecord<byte[], byte[]> createDomain() {
		JsonObject key = new JsonObject().put("type", "domains").put("owner", "owner").put("uid", "uid").put("id", 42)
				.put("valueClass", "valueClass");
		JsonArray aliases = new JsonArray().add("alias1").add("alias2");
		JsonObject domain = new JsonObject().put("uid", "123.internal").put("value",
				new JsonObject().put("aliases", aliases));
		return new ProducerRecord<>(DOMAIN_TOPIC_NAME, key.encode().getBytes(), domain.encode().getBytes());
	}

	private ProducerRecord<byte[], byte[]> createInstallation(String uid, String ip) {
		JsonObject key = new JsonObject().put("type", "installation").put("owner", "owner").put("uid", "uid")
				.put("id", new Random().nextInt()).put("valueClass", "valueClass");
		JsonObject installationValue = new JsonObject().put("tags", new JsonArray().add("bm/nginx")).put("ip", ip);
		JsonObject installation = new JsonObject().put("uid", uid).put("value", installationValue);
		return new ProducerRecord<>(ORPHANS_TOPIC_NAME, key.encode().getBytes(), installation.encode().getBytes());
	}

	private ProducerRecord<byte[], byte[]> createDir(String email, String dataLocation) {
		JsonObject key = new JsonObject().put("type", "dir").put("owner", "owner").put("uid", "uid")
				.put("id", new Random().nextInt()).put("valueClass", "valueClass");
		JsonObject dirEntryValue = new JsonObject().put("dataLocation", dataLocation);
		JsonObject dirEmail = new JsonObject().put("address", email).put("allAliases", false);
		JsonObject dirValueValue = new JsonObject().put("emails", new JsonArray().add(dirEmail));
		JsonObject dir = new JsonObject().put("value",
				new JsonObject().put("entry", dirEntryValue).put("value", dirValueValue));
		return new ProducerRecord<>(DOMAIN_TOPIC_NAME, key.encode().getBytes(), dir.encode().getBytes());
	}

}
