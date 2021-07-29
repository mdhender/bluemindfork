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
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.errors.TopicExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import net.bluemind.config.DataLocation;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.backup.continuous.store.ITopicStore;
import net.bluemind.core.backup.continuous.store.TopicPublisher;
import net.bluemind.core.backup.continuous.store.TopicSubscriber;

public class KafkaTopicStore implements ITopicStore {

	private static final Logger logger = LoggerFactory.getLogger(KafkaTopicStore.class);

	static final String COMPRESSION_TYPE = "zstd";
	static final int PARTITION_COUNT = 64;

	private AdminClient adminClient;

	private String zkBootstrap;
	private String bootstrap;
	private String cid;

	private final Map<TopicDescriptor, KafkaTopicPublisher> knownPublisher = new ConcurrentHashMap<>();

	private AtomicLong subId = new AtomicLong();

	public KafkaTopicStore() {
		kafkaBootstrapServers();

		String loc = DataLocation.current();

		if (bootstrap != null && zkBootstrap != null) {
			Properties properties = new Properties();
			properties.put("bootstrap.servers", bootstrap);
			this.cid = "backup.store-" + InstallationId.getIdentifier() + "__" + loc + "_" + System.nanoTime();
			properties.put("client.id", cid);

			this.adminClient = AdminClient.create(properties);

//				Properties topicsProdProps = new Properties();
//				topicsProdProps.setProperty("bootstrap.servers", bootstrap);
//				topicsProdProps.setProperty("acks", "all");
//				topicsProdProps.setProperty("compression.type", COMPRESSION_TYPE);
//				topicsProdProps.setProperty("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
//				topicsProdProps.setProperty("value.serializer",
//						"org.apache.kafka.common.serialization.StringSe	rializer");
//				this.topicsListProd = new KafkaProducer<>(topicsProdProps);
//				this.knownTopicsTopic = InstallationId.getIdentifier().replace("bluemind-", "").replace("-", "")
//						+ "__known_topics";
//				ensureKafkaTopic(knownTopicsTopic);
		}
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
		return new KafkaTopicSubscriber(bootstrap, cid, topicName);
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

}
