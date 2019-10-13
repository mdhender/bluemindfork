/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.hornetq.client.impl;

import java.util.UUID;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientConnectionStrategyConfig.ReconnectMode;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.client.config.ClientReliableTopicConfig;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.internal.diagnostics.HealthMonitorLevel;
import com.hazelcast.spi.properties.GroupProperty;
import com.hazelcast.topic.TopicOverloadPolicy;

import net.bluemind.hornetq.client.MQ;

public final class ClusterClient extends ClusterNode {

	public ClusterClient(String jvmType) {
		super(jvmType);
	}

	protected HazelcastInstance hazelcastConnectImpl(String jvmType) {
		ClientConfig cfg = new ClientConfig();

		cfg.setInstanceName(jvmType + "-" + UUID.randomUUID().toString());
		cfg.setProperty(GroupProperty.LOGGING_TYPE.getName(), "slf4j");
		cfg.setProperty(GroupProperty.BACKPRESSURE_ENABLED.getName(), "true");
		cfg.setProperty(GroupProperty.OPERATION_BACKUP_TIMEOUT_MILLIS.getName(), "61000");
		cfg.setProperty(GroupProperty.HEALTH_MONITORING_LEVEL.getName(), HealthMonitorLevel.OFF.name());
		GroupConfig gc = new GroupConfig(MQ.CLUSTER_ID);
		cfg.setGroupConfig(gc);

		configureTopics(cfg);

		ClientNetworkConfig netCfg = cfg.getNetworkConfig();
		netCfg.addAddress(memberAddress());
		// 0 means try forever
		netCfg.setConnectionAttemptLimit(0).setConnectionAttemptPeriod(3000);

		cfg.getConnectionStrategyConfig().setReconnectMode(ReconnectMode.ASYNC);

		HazelcastInstance hz = HazelcastClient.newHazelcastClient(cfg);
		return hz;
	}

	private void configureTopics(ClientConfig cfg) {
		ClientReliableTopicConfig topicConfig = cfg.getReliableTopicConfig("default");
		topicConfig.setTopicOverloadPolicy(TopicOverloadPolicy.DISCARD_OLDEST);
	}

	@Override
	protected void setupMetrics(HazelcastInstance hzInstance) {
		// no cluster metrics on hz client
	}

}
