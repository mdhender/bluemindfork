/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.backup.store.kafka;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteTopicsOptions;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.errors.TopicExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.Registry;
import com.typesafe.config.Config;

import net.bluemind.config.DataLocation;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.backup.continuous.store.ITopicStore;
import net.bluemind.core.backup.continuous.store.TopicManager;
import net.bluemind.core.backup.continuous.store.TopicPublisher;
import net.bluemind.core.backup.continuous.store.TopicSubscriber;
import net.bluemind.core.backup.store.kafka.config.KafkaStoreConfig;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

public class KafkaTopicStore implements ITopicStore, TopicManager {

	private static final Logger logger = LoggerFactory.getLogger(KafkaTopicStore.class);
	private static final AtomicInteger cidAlloc = new AtomicInteger();

	static final String COMPRESSION_TYPE = "zstd";
	public static final int PARTITION_COUNT = KafkaStoreConfig.get().getInt("kafka.topic.partitionCount");
	static final short REPL_FACTOR = (short) KafkaStoreConfig.get().getInt("kafka.topic.replicationFactor");

	private static record Bootstrap(String zookeeper, String brokers) {

		public boolean valid() {
			return zookeeper != null && brokers != null;
		}
	}

	private final Supplier<AdminClient> adminClient;

	private final Bootstrap bootstrap;

	private final Map<TopicDescriptor, KafkaTopicPublisher> knownPublisher = new ConcurrentHashMap<>();
	private final Registry reg;

	public KafkaTopicStore() {
		bootstrap = kafkaBootstrapServers();

		String loc = DataLocation.current();
		logger.warn("kafka.bootstrap {}, zk {}", bootstrap.brokers(), bootstrap.zookeeper());
		if (bootstrap.valid()) {
			Properties properties = new Properties();
			properties.put("bootstrap.servers", bootstrap.brokers());
			String cid = jvm() + "_" + InstallationId.getIdentifier() + "_" + loc + "_" + cidAlloc.incrementAndGet();
			properties.put("client.id", cid);
			this.adminClient = () -> AdminClient.create(properties);
		} else {
			this.adminClient = null;
		}

		this.reg = MetricsRegistry.get();
	}

	private String jvm() {
		return System.getProperty("net.bluemind.property.product", "unknown");
	}

	private Bootstrap kafkaBootstrapServers() {
		String brokersBootstrap = System.getProperty("bm.kafka.bootstrap.servers");
		String zkBootstrap = System.getProperty("bm.zk.servers");
		if (brokersBootstrap == null || zkBootstrap == null) {
			File local = new File("/etc/bm/kafka.properties");
			if (!local.exists()) {
				local = new File(System.getProperty("user.home") + "/kafka.properties");
			}
			if (local.exists()) {
				Properties tmp = new Properties();
				try (InputStream in = Files.newInputStream(local.toPath())) {
					tmp.load(in);
				} catch (Exception e) {
					logger.warn(e.getMessage());
				}
				brokersBootstrap = tmp.getProperty("bootstrap.servers");
				zkBootstrap = tmp.getProperty("zookeeper.servers");
			}
		}
		return new Bootstrap(zkBootstrap, brokersBootstrap);
	}

	@Override
	public boolean isEnabled() {
		return adminClient != null;
	}

	@Override
	public Set<String> topicNames() {
		try (var ac = adminClient.get()) {
			ListTopicsOptions opts = new ListTopicsOptions();
			opts.listInternal(false);
			Map<String, TopicListing> existing = ac.listTopics(opts).namesToListings().get();
			logger.info("topic names:{}", existing.keySet());
			return existing.keySet();
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public Set<String> topicNames(String installationId) {
		String installation = installationId.replace("bluemind-", "").replace("-", "");
		return topicNames().stream().filter(name -> name.startsWith(installation)).collect(Collectors.toSet());
	}

	@Override
	public TopicSubscriber getSubscriber(String topicName) {
		return new KafkaTopicSubscriber(bootstrap.brokers(), topicName, reg,
				new IdFactory("kafka.consumer", reg, KafkaTopicSubscriber.class));
	}

	@Override
	public TopicPublisher getPublisher(TopicDescriptor descriptor) {
		return knownPublisher.computeIfAbsent(descriptor, this::createImpl);
	}

	private KafkaTopicPublisher createImpl(TopicDescriptor td) {
		String physicalTopic = td.physicalTopic();

		logger.info("{} bound to physical topic '{}'", td, physicalTopic);
		ensureKafkaTopic(physicalTopic);
		return new KafkaTopicPublisher(bootstrap.brokers(), physicalTopic);

	}

	private void ensureKafkaTopic(String name) {
		try (var ac = adminClient.get()) {
			ListTopicsOptions opts = new ListTopicsOptions();
			opts.listInternal(false);
			Map<String, TopicListing> existing = ac.listTopics(opts).namesToListings().get();
			if (!existing.containsKey(name)) {
				Config conf = KafkaStoreConfig.get();
				NewTopic nt = new NewTopic(name, PARTITION_COUNT, REPL_FACTOR);
				long compactionLagMs = conf.getDuration("kafka.topic.maxCompactionLag", TimeUnit.MILLISECONDS);
				long segmentMs = conf.getDuration("kafka.topic.maxSegmentDuration", TimeUnit.MILLISECONDS);
				nt.configs(Map.of(//
						TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, Integer.toString(conf.getInt("kafka.topic.minIsr")), //
						TopicConfig.MAX_MESSAGE_BYTES_CONFIG,
						Long.toString((long) (conf.getMemorySize("kafka.producer.maxRecordSize").toBytes() * 1.05)), //
						TopicConfig.COMPRESSION_TYPE_CONFIG, COMPRESSION_TYPE, //
						TopicConfig.CLEANUP_POLICY_CONFIG, "compact", //
						TopicConfig.MAX_COMPACTION_LAG_MS_CONFIG, Long.toString(compactionLagMs), //
						TopicConfig.SEGMENT_MS_CONFIG, Long.toString(segmentMs)//
				));
				CreateTopicsOptions cto = new CreateTopicsOptions();

				CreateTopicsResult res = ac.createTopics(Arrays.asList(nt), cto);
				Uuid created = res.topicId(name).get();
				logger.info("Created topic {}: {}", name, created);
			}
		} catch (ExecutionException ex) {
			if (!(ex.getCause() instanceof TopicExistsException)) {
				throw new ServerFault(ex);
			}
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public void delete(String topic) {
		try (var ac = adminClient.get()) {
			Iterator<Entry<TopicDescriptor, KafkaTopicPublisher>> it = knownPublisher.entrySet().iterator();
			while (it.hasNext()) {
				Entry<TopicDescriptor, KafkaTopicPublisher> entry = it.next();
				if (entry.getKey().physicalTopic().equals(topic)) {
					it.remove();
				}
			}
			Optional.ofNullable(KafkaTopicPublisher.perPhyTopicProd.remove(topic)).ifPresent(prod -> {
				logger.info("Closing {}", prod);
				prod.close(Duration.ofSeconds(20));
			});
			DeleteTopicsOptions opts = new DeleteTopicsOptions();
			DeleteTopicsResult result = ac.deleteTopics(Collections.singleton(topic), opts);
			result.all().toCompletionStage().thenAccept(v -> logger.info("Topic {} deleted.", topic))
					.exceptionally(ex -> {
						logger.error("Deletion of {} failed ({})", topic, ex.getMessage(), ex);
						return null;
					}).toCompletableFuture().orTimeout(30, TimeUnit.SECONDS).join();
		}
	}

	@Override
	public void flush(String topic) {
		Iterator<Entry<TopicDescriptor, KafkaTopicPublisher>> it = knownPublisher.entrySet().iterator();
		while (it.hasNext()) {
			Entry<TopicDescriptor, KafkaTopicPublisher> entry = it.next();
			if (entry.getKey().physicalTopic().equals(topic)) {
				it.remove();
			}
		}
		Optional.ofNullable(KafkaTopicPublisher.perPhyTopicProd.remove(topic)).ifPresent(prod -> {
			logger.info("Closing {}", prod);
			prod.close(Duration.ofSeconds(20));
		});
	}

	public void flushAll() {
		Set<String> topics = knownPublisher.keySet().stream().map(TopicDescriptor::physicalTopic)
				.collect(Collectors.toSet());
		for (String topic : topics) {
			logger.info("Flushing {}", topics);
			Optional.ofNullable(KafkaTopicPublisher.perPhyTopicProd.remove(topic)).ifPresent(prod -> {
				logger.info("Closing {}", prod);
				prod.close(Duration.ofSeconds(20));
			});
		}
	}

	@Override
	public void reconfigure(String topic, Map<String, String> updatedProps) {
		logger.info("reconfigure {} is not implemented.");

//		Map<ConfigResource, Collection<AlterConfigOp>> matchTopic=new HashMap<>();
//		ConfigResource cr=new ConfigResource(Type.TOPIC, topic);
//		AlterConfigOp op=new AlterConfigOp(entry, OpType.SET);
//		adminClient.incrementalAlterConfigs(matchTopic);

	}

	@Override
	public TopicManager getManager() {
		return this;
	}

}
