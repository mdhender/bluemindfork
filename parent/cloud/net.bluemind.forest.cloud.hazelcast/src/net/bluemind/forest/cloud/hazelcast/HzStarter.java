/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.forest.cloud.hazelcast;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.config.Config;
import com.hazelcast.config.DiscoveryStrategyConfig;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.MemberAttributeConfig;
import com.hazelcast.config.RestApiConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.internal.diagnostics.HealthMonitorLevel;
import com.hazelcast.spi.properties.GroupProperty;

public class HzStarter {

	private static final Logger logger = LoggerFactory.getLogger(HzStarter.class);
	private final CompletableFuture<HazelcastInstance> hzStart;

	public HzStarter(String jvmType, String zkIp) {
		this.hzStart = new CompletableFuture<>();

		Thread hzConnect = new Thread(() -> {
			try {
				HazelcastInstance hzInstance = hazelcastConnectImpl(jvmType, zkIp);
				hzStart.complete(hzInstance);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				hzStart.completeExceptionally(e);
			}
		}, "bm-hz-connect");
		hzConnect.setDaemon(true);
		hzConnect.start();

	}

	public HazelcastInstance get(long t, TimeUnit tu)
			throws InterruptedException, ExecutionException, TimeoutException {
		return hzStart.get(t, tu);
	}

	public CompletableFuture<HazelcastInstance> startFuture() {
		return hzStart;
	}

	private HazelcastInstance hazelcastConnectImpl(String jvmType, String zkIp) {
		Config cfg = new Config();

		cfg.setInstanceName(jvmType + "-" + UUID.randomUUID().toString());
		cfg.setProperty(GroupProperty.LOGGING_TYPE.getName(), "slf4j");
		cfg.setProperty(GroupProperty.BACKPRESSURE_ENABLED.getName(), "true");
		cfg.setProperty(GroupProperty.OPERATION_BACKUP_TIMEOUT_MILLIS.getName(), "61000");
		cfg.setProperty(GroupProperty.SOCKET_SERVER_BIND_ANY.getName(), "false");
		cfg.setProperty(GroupProperty.PHONE_HOME_ENABLED.getName(), "false");
		cfg.getNetworkConfig().setRestApiConfig(new RestApiConfig().setEnabled(true));
		cfg.getNetworkConfig().setPort(9701).setPortAutoIncrement(true).setPortCount(100);

		cfg.setProperty(GroupProperty.HEALTH_MONITORING_LEVEL.getName(), HealthMonitorLevel.OFF.name());
		GroupConfig gc = new GroupConfig("forest-hz-nodes");
		cfg.setGroupConfig(gc);

		MemberAttributeConfig memberConf = cfg.getMemberAttributeConfig();
		memberConf.setStringAttribute("bluemind.kind", jvmType);

		cfg.setProperty(GroupProperty.DISCOVERY_SPI_ENABLED.getName(), "true");
		cfg.getNetworkConfig().setReuseAddress(true).getJoin().getMulticastConfig().setEnabled(false);

		DiscoveryStrategyConfig discoveryStrategyConfig = new DiscoveryStrategyConfig(
				new ZookeeperDiscoveryStrategyFactory());
		discoveryStrategyConfig.addProperty(ZookeeperDiscoveryProperties.ZOOKEEPER_URL.key(), zkIp + ":2181");
		discoveryStrategyConfig.addProperty(ZookeeperDiscoveryProperties.ZOOKEEPER_PATH.key(), "/discovery/hazelcast");
		discoveryStrategyConfig.addProperty(ZookeeperDiscoveryProperties.GROUP.key(), "forest-hz-nodes");
		cfg.getNetworkConfig().getJoin().getDiscoveryConfig().addDiscoveryStrategyConfig(discoveryStrategyConfig);

		return Hazelcast.newHazelcastInstance(cfg);
	}

}
