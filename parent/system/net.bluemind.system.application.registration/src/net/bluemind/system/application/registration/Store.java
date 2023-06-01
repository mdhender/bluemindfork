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
package net.bluemind.system.application.registration;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.errors.TopicExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;

public class Store {

	private static final Logger logger = LoggerFactory.getLogger(Store.class);
	private static final AtomicInteger cidAlloc = new AtomicInteger();

	static final String COMPRESSION_TYPE = "zstd";

	private AdminClient adminClient;

	private String zkBootstrap;
	private String bootstrap;
	private String cid;

	private final Map<DefaultTopicDescriptor, Publisher> knownPublisher = new ConcurrentHashMap<>();

	public Store(String client) {
		kafkaBootstrapServers();

		logger.warn("kafka.bootstrap {}, zk {}", bootstrap, zkBootstrap);
		if (bootstrap != null && zkBootstrap != null) {
			Properties properties = new Properties();
			properties.put("bootstrap.servers", bootstrap);
			this.cid = client + "_" + System.currentTimeMillis();
			properties.put("client.id", cid);

			this.adminClient = AdminClient.create(properties);
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

	public boolean isEnabled() {
		return adminClient != null;
	}

	public Publisher getPublisher(DefaultTopicDescriptor descriptor) {
		return knownPublisher.computeIfAbsent(descriptor, this::createImpl);
	}

	private Publisher createImpl(DefaultTopicDescriptor td) {
		String physicalTopic = td.install.replace("bluemind-", "").replace("-", "") + "-" + td.domainUid;
		ensureKafkaTopic(physicalTopic);
		return new Publisher(bootstrap, physicalTopic);

	}

	private void ensureKafkaTopic(String name) {
		try {
			ListTopicsOptions opts = new ListTopicsOptions();
			opts.listInternal(false);
			Map<String, TopicListing> existing = adminClient.listTopics(opts).namesToListings().get();
			if (!existing.containsKey(name)) {
				NewTopic nt = new NewTopic(name, 1, (short) 1);
				nt.configs(Map.of(//
						TopicConfig.COMPRESSION_TYPE_CONFIG, COMPRESSION_TYPE, //
						TopicConfig.CLEANUP_POLICY_CONFIG, "delete"));
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
