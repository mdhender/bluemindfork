/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.cloud.monitoring.server.api.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.streams.StreamsConfig;

import com.typesafe.config.Config;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import net.bluemind.central.reverse.proxy.model.common.kafka.KafkaConsumerClient;
import net.bluemind.cloud.monitoring.server.MonitoringConfig;
import net.bluemind.cloud.monitoring.server.grafana.Mermaid;
import net.bluemind.cloud.monitoring.server.zk.Forest;
import net.bluemind.cloud.monitoring.server.zk.ZkNode;
import net.bluemind.core.utils.JsonUtils;

public class DataState extends AbstractVerticle {

	private static String metrics;
	private static String topology;
	private static final String NODE_TOPIC = "bluemind_cluster-__nodes__";
	private static final long FIVE_MINUTES = 5 * 60 * 1000;
	private static Set<NodeInfo> clusterNodes = new HashSet<>();
	private static Map<String, Long> activityLog = new HashMap<>();
	private Set<ZkNode> zkInstances;

	private final Config config;

	public DataState(Config config) {
		this.config = config;
	}

	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		vertx.executeBlocking((p -> {
			try (Forest forestInstanceLoader = new Forest(config)) {
				zkInstances = forestInstanceLoader.whiteListedInstances();
			}
			p.complete();
		}));

		vertx.setPeriodic(FIVE_MINUTES, (id) -> {
			long fiveMinutesAgo = System.currentTimeMillis() - FIVE_MINUTES;
			for (NodeInfo node : new HashSet<>(clusterNodes)) {
				if (activityLog.get(node.address) < fiveMinutesAgo) {
					clusterNodes.remove(node);
				}
			}
		});

		consume("earliest");
		startPromise.complete();
	}

	private void consume(String mode) {
		CompletableFuture<Void> done = new CompletableFuture<>();
		Properties props = new Properties();
		props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, config.getString(MonitoringConfig.Kafka.BOOTSTRAP_SERVERS));
		props.put(ConsumerConfig.GROUP_ID_CONFIG, "cloud.monitor.listall" + System.currentTimeMillis());
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, mode);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

		KafkaConsumerClient<String, String> consumer = KafkaConsumerClient.create(vertx, props);

		Handler<ConsumerRecord<String, String>> recordHandler = (rec -> {
			NodeInfo node = JsonUtils.read(rec.value(), NodeInfo.class);
			activityLog.put(node.address, System.currentTimeMillis());
			node.timestamp = rec.timestamp();
			if (!(node.state.equals("CORE_STATE_NOT_INSTALLED") || "bluemind-noid".equals(node.installationId))) {
				update(node);
			}
		});
		consumer.handler(recordHandler) //
				.subscribe(Collections.singletonList(NODE_TOPIC)) //
				.onSuccess(v -> done.complete(null));

		done.thenRun(() -> consume("latest"));
	}

	private void setNodeType(NodeInfo node) {
		if (node.product.equals("bm-crp")) {
			node.type = NodeType.CRP;
			node.forestId = node.installationId;
			node.installationId = null;
		} else {
			Optional<ZkNode> associatedZkNode = isManagedByCrp(node.installationId);
			associatedZkNode.ifPresentOrElse(zkNode -> {
				node.forestId = zkNode.forestId;
				if (node.state.equals("CORE_STATE_CLONING")) {
					node.type = NodeType.TAIL;
				} else {
					node.type = NodeType.MASTER;
				}
			}, () -> {
				node.type = NodeType.FORK;
				node.forestId = node.address;
			});
		}
	}

	private Optional<ZkNode> isManagedByCrp(String installationId) {
		String cleanedId = installationId.replace("bluemind-", "");
		return zkInstances.stream().filter(zk -> zk.installationId.equals(cleanedId)).findFirst();
	}

	public void update(NodeInfo node) {
		clusterNodes.remove(node);
		setNodeType(node);
		clusterNodes.add(node);
		Mermaid mermaid = new Mermaid(clusterNodes);
		mermaid.evaluate();
		setData(mermaid.getTopology(), mermaid.getMetrics());
	}

	public static void setData(String topology, String metrics) {
		DataState.topology = topology;
		DataState.metrics = metrics;
	}

	public static String getMetrics() {
		return metrics;
	}

	public static String getTopology() {
		return topology;
	}

}
