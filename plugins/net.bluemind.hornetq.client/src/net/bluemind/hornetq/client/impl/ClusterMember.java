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

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MemberAttributeConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.ReliableTopicConfig;
import com.hazelcast.config.RingbufferConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import com.hazelcast.core.MigrationEvent;
import com.hazelcast.core.MigrationListener;
import com.hazelcast.core.PartitionService;
import com.hazelcast.internal.diagnostics.HealthMonitorLevel;
import com.hazelcast.partition.PartitionLostEvent;
import com.hazelcast.partition.PartitionLostListener;
import com.hazelcast.spi.properties.GroupProperty;
import com.hazelcast.topic.TopicOverloadPolicy;
import com.netflix.spectator.api.Gauge;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Registry;

import net.bluemind.config.InstallationId;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.hornetq.client.IHazelcastConfigPimp;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

public final class ClusterMember extends ClusterNode {

	public ClusterMember(String jvmType) {
		super(jvmType);
	}

	protected HazelcastInstance hazelcastConnectImpl(String jvmType) {
		Config cfg = new Config();

		cfg.setInstanceName(jvmType + "-" + UUID.randomUUID().toString());
		cfg.setProperty("hazelcast.logging.type", "slf4j");
		cfg.setProperty("hazelcast.socket.server.bind.any", "false");
		cfg.setProperty("hazelcast.phone.home.enabled", "false");
		cfg.setProperty(GroupProperty.HEALTH_MONITORING_LEVEL.getName(), HealthMonitorLevel.OFF.name());
		GroupConfig gc = new GroupConfig(MQ.CLUSTER_ID);
		cfg.setGroupConfig(gc);

		MemberAttributeConfig memberConf = cfg.getMemberAttributeConfig();
		memberConf.setStringAttribute("bluemind.kind", jvmType);

		configureDiscovery(cfg);

		configureTopics(cfg);

		IHazelcastConfigPimp pimp = loadPimp();
		if (pimp != null) {
			logger.info("PIMP Hazelcast configuration with {}", pimp);
			cfg = pimp.pimp(cfg);
		}

		HazelcastInstance hz = Hazelcast.newHazelcastInstance(cfg);
		return hz;
	}

	protected void setupMetrics(HazelcastInstance hzInstance) {
		Cluster cluster = hzInstance.getCluster();
		Member me = cluster.getLocalMember();

		Registry reg = MetricsRegistry.get();
		IdFactory id = new IdFactory("cluster", reg, ClusterNode.class);
		Id perJvmVision = id.name("members", "clusterId", InstallationId.getIdentifier());
		Gauge perJvmMemberCount = reg.gauge(perJvmVision);
		Id globalClusterId = reg.createId("bluemind.cluster", "id", InstallationId.getIdentifier(), "jvm",
				memberJvm(me));
		Gauge oldestMember = reg.gauge(globalClusterId); // 1 if oldest
		Set<Member> myMembers = cluster.getMembers();
		perJvmMemberCount.set(myMembers.size());
		Member myOldest = myMembers.iterator().next();
		boolean meMaster = myOldest.getUuid().equals(me.getUuid());
		oldestMember.set(meMaster ? 1 : 0);

		cluster.addMembershipListener(new MembershipListener() {

			@Override
			public void memberRemoved(MembershipEvent membershipEvent) {
				refreshMetrics(membershipEvent);
			}

			@Override
			public void memberAttributeChanged(MemberAttributeEvent memberAttributeEvent) {
			}

			@Override
			public void memberAdded(MembershipEvent membershipEvent) {
				refreshMetrics(membershipEvent);
			}

			private void refreshMetrics(MembershipEvent membershipEvent) {
				Member current = membershipEvent.getCluster().getLocalMember();
				Set<Member> members = membershipEvent.getMembers();
				perJvmMemberCount.set(members.size());
				Member oldest = members.iterator().next();
				boolean isMaster = oldest.getUuid().equals(current.getUuid());
				oldestMember.set(isMaster ? 1 : 0);
			}
		});
		PartitionService partitions = hzInstance.getPartitionService();
		Id clusterPartitionsId = reg.createId("bluemind.cluster.partitions", "id", InstallationId.getIdentifier(),
				"jvm", memberJvm(me));
		Gauge oneIfClusterIsSafe = reg.gauge(clusterPartitionsId);
		partitions.addMigrationListener(new MigrationListener() {

			@Override
			public void migrationStarted(MigrationEvent migrationEvent) {
				oneIfClusterIsSafe.set(partitions.isClusterSafe() ? 1 : 0);
			}

			@Override
			public void migrationFailed(MigrationEvent migrationEvent) {
				oneIfClusterIsSafe.set(partitions.isClusterSafe() ? 1 : 0);
			}

			@Override
			public void migrationCompleted(MigrationEvent migrationEvent) {
				oneIfClusterIsSafe.set(partitions.isClusterSafe() ? 1 : 0);
			}
		});
		partitions.addPartitionLostListener(new PartitionLostListener() {

			@Override
			public void partitionLost(PartitionLostEvent event) {
				oneIfClusterIsSafe.set(partitions.isClusterSafe() ? 1 : 0);
			}
		});
	}

	private void configureTopics(Config cfg) {
		RingbufferConfig rbConfig = cfg.getRingbufferConfig("default");
		rbConfig.setCapacity(5000);

		ReliableTopicConfig topicConfig = cfg.getReliableTopicConfig("default");
		topicConfig.setTopicOverloadPolicy(TopicOverloadPolicy.DISCARD_OLDEST);
		topicConfig.setStatisticsEnabled(true);
	}

	private IHazelcastConfigPimp loadPimp() {
		RunnableExtensionLoader<IHazelcastConfigPimp> rel = new RunnableExtensionLoader<>();
		List<IHazelcastConfigPimp> pimp = rel.loadExtensions("net.bluemind.hornetq.client", "hzpimp", "hz_pimp",
				"impl");
		return pimp.isEmpty() ? null : pimp.get(0);
	}

	private void configureDiscovery(Config cfg) {
		NetworkConfig netConf = cfg.getNetworkConfig();
		netConf.setReuseAddress(true);
		JoinConfig joinConf = netConf.getJoin();
		joinConf.getMulticastConfig().setEnabled(false);
		TcpIpConfig tcpConfig = joinConf.getTcpIpConfig();
		tcpConfig.setEnabled(true).addMember(memberAddress());
	}
}
