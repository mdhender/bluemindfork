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
import com.hazelcast.config.NearCacheConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.internal.diagnostics.HealthMonitorLevel;
import com.hazelcast.spi.properties.ClusterProperty;
import com.hazelcast.topic.TopicOverloadPolicy;

import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Shared;

public final class ClusterClient extends ClusterNode {

	public ClusterClient(String jvmType) {
		super(jvmType);
	}

	protected HazelcastInstance hazelcastConnectImpl(String jvmType) {
		ClientConfig cfg = new ClientConfig();

		cfg.setInstanceName(jvmType + "-" + UUID.randomUUID().toString());
		cfg.setProperty(ClusterProperty.LOGGING_TYPE.getName(), "slf4j");
		cfg.setProperty(ClusterProperty.BACKPRESSURE_ENABLED.getName(), "true");
		cfg.setProperty(ClusterProperty.OPERATION_BACKUP_TIMEOUT_MILLIS.getName(), "61000");
		cfg.setProperty(ClusterProperty.HEALTH_MONITORING_LEVEL.getName(), HealthMonitorLevel.OFF.name());
		cfg.setClusterName(MQ.CLUSTER_ID);

		configureTopics(cfg);

		ClientNetworkConfig netCfg = cfg.getNetworkConfig();
		netCfg.addAddress(memberAddress());

		cfg.getConnectionStrategyConfig() //
				.setConnectionRetryConfig(cfg.getConnectionStrategyConfig().getConnectionRetryConfig()
						.setClusterConnectTimeoutMillis(3000)) //
				.setReconnectMode(ReconnectMode.ASYNC);

		NearCacheConfig nc = new NearCacheConfig(Shared.MAP_SYSCONF).setInvalidateOnChange(true);
		cfg.getNearCacheConfigMap().put(Shared.MAP_SYSCONF, nc);

		NearCacheConfig domainSettingsNC = new NearCacheConfig(Shared.MAP_DOMAIN_SETTINGS).setInvalidateOnChange(true);
		cfg.getNearCacheConfigMap().put(Shared.MAP_DOMAIN_SETTINGS, domainSettingsNC);

		return HazelcastClient.newHazelcastClient(cfg);
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
