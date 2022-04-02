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
import java.util.concurrent.atomic.AtomicInteger;
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
import org.apache.kafka.common.errors.TopicExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.netflix.spectator.api.Registry;

import net.bluemind.config.DataLocation;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.backup.continuous.store.ITopicStore;
import net.bluemind.core.backup.continuous.store.TopicManager;
import net.bluemind.core.backup.continuous.store.TopicPublisher;
import net.bluemind.core.backup.continuous.store.TopicSubscriber;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

public class KafkaTopicStore implements ITopicStore, TopicManager {

	private static final Logger logger = LoggerFactory.getLogger(KafkaTopicStore.class);
	private static final AtomicInteger cidAlloc = new AtomicInteger();

	static final String COMPRESSION_TYPE = "zstd";
	static final int PARTITION_COUNT = 64;

	private AdminClient adminClient;

	private String zkBootstrap;
	private String bootstrap;
	private String cid;

	private final Map<TopicDescriptor, KafkaTopicPublisher> knownPublisher = new ConcurrentHashMap<>();
	private final Registry reg;

	public KafkaTopicStore() {
		kafkaBootstrapServers();

		String loc = DataLocation.current();
		logger.warn("kafka.bootstrap {}, zk {}", bootstrap, zkBootstrap);
		if (bootstrap != null && zkBootstrap != null) {
			Properties properties = new Properties();
			properties.put("bootstrap.servers", bootstrap);
			this.cid = jvm() + "_" + InstallationId.getIdentifier() + "_" + loc + "_" + cidAlloc.incrementAndGet();
			properties.put("client.id", cid);

			this.adminClient = AdminClient.create(properties);
		}
		this.reg = MetricsRegistry.get();
	}

	private String jvm() {
		return System.getProperty("net.bluemind.property.product", "unknown");
	}

	private void kafkaBootstrapServers() {
		this.bootstrap = System.getProperty("bm.kafka.bootstrap.servers");
		this.zkBootstrap = System.getProperty("bm.zk.servers");
		if (bootstrap == null || zkBootstrap == null) {
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
				bootstrap = tmp.getProperty("bootstrap.servers");
				zkBootstrap = tmp.getProperty("zookeeper.servers");
			}
		}
	}

	@Override
	public boolean isEnabled() {
		return adminClient != null;
	}

	@Override
	public Set<String> topicNames() {
		try {
			ListTopicsOptions opts = new ListTopicsOptions();
			opts.listInternal(false);
			Map<String, TopicListing> existing = adminClient.listTopics(opts).namesToListings().get();
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
		return new KafkaTopicSubscriber(bootstrap, topicName, reg,
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
		return new KafkaTopicPublisher(bootstrap, physicalTopic);

	}

	private void ensureKafkaTopic(String name) {
		try {
			ListTopicsOptions opts = new ListTopicsOptions();
			opts.listInternal(false);
			Map<String, TopicListing> existing = adminClient.listTopics(opts).namesToListings().get();
			if (!existing.containsKey(name)) {
				NewTopic nt = new NewTopic(name, PARTITION_COUNT, (short) 1);
				nt.configs(ImmutableMap.of(//
						"compression.type", COMPRESSION_TYPE, //
						"cleanup.policy", "compact", //
						"max.compaction.lag.ms", "120000"//
				));
				CreateTopicsOptions cto = new CreateTopicsOptions();

				CreateTopicsResult res = adminClient.createTopics(Arrays.asList(nt), cto);
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
		DeleteTopicsResult result = adminClient.deleteTopics(Collections.singleton(topic), opts);
		result.all().toCompletionStage().thenAccept(v -> logger.info("Topic {} deleted.", topic)).exceptionally(ex -> {
			logger.error("Deletion of {} failed ({})", topic, ex.getMessage(), ex);
			return null;
		}).toCompletableFuture().join();
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
