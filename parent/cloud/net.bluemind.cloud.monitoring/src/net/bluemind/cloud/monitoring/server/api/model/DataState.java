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

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.streams.StreamsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.typesafe.config.Config;

import io.prometheus.client.Gauge;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.central.reverse.proxy.model.common.kafka.KafkaConsumerClient;
import net.bluemind.cloud.monitoring.server.MonitoringConfig;
import net.bluemind.cloud.monitoring.server.grafana.Mermaid;
import net.bluemind.cloud.monitoring.server.zk.Forest;
import net.bluemind.cloud.monitoring.server.zk.ZkNode;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.system.application.registration.model.ApplicationInfoModel;

public class DataState extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(DataState.class);

	private static String metrics;
	private static String metricKeys;
	private static String topology;
	private static final String TOPOLOGY_CHANGED = "topology.changed";
	private static final String NODE_TOPIC = "bluemind_cluster-__nodes__";
	private static final Duration THREE_MINUTES = Duration.ofMinutes(3);
	private static final long THREE_SECONDS = Duration.ofSeconds(3).toMillis();
	private static Map<String, Gauge> gauges = new HashMap<>();
	private static final Config config = MonitoringConfig.get();

	private Set<ZkNode> zkInstances;
	private Cache<String, NodeInfo> clusterNodes;

	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		getZkNodeInfo();

		clusterNodes = Caffeine.newBuilder() //
				.removalListener((key, value, cause) -> {
					if (RemovalCause.EXPIRED == cause) {
						getZkNodeInfo();
						updateMermaid();
						logger.info("Topology updated to {}", getTopology());
						vertx.setTimer(1, e -> vertx.eventBus().publish(TOPOLOGY_CHANGED, new JsonObject(metricKeys)));
					}
				}) //
				.expireAfterWrite(THREE_MINUTES) //
				.build();

		vertx.setTimer(THREE_SECONDS, id -> consume("earliest"));

		startPromise.complete();
	}

	private void getZkNodeInfo() {
		try (Forest forestInstanceLoader = new Forest(config)) {
			zkInstances = forestInstanceLoader.whiteListedInstancesNode();
		}
	}

	private void consume(String mode) {
		logger.info("Starting Topic streaming using mode {}", mode);
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
			AtomicBoolean topicChangeDuringHarvesting = new AtomicBoolean(false);
			ApplicationInfoModel info = JsonUtils.read(rec.value(), ApplicationInfoModel.class);
			NodeInfo node = new NodeInfo(info);
			if (node.info.product.equals("bm-crp")) {
				node.forestId = node.info.installationId;
				node.info.installationId = null;
			}
			if (!clusterNodes.asMap().containsKey(node.id())) {
				topicChangeDuringHarvesting.set(true);
			}
			node.timestamp = rec.timestamp();
			if (!(node.isNotInstalledStateOrNoId())) {
				update(node);
				if (topicChangeDuringHarvesting.get()) {
					logger.info("Topology has changed to: {}", getTopology());
					vertx.setTimer(1, e -> vertx.eventBus().publish(TOPOLOGY_CHANGED, new JsonObject(metricKeys)));
				}
			} else {
				clusterNodes.put(node.id(), node);
			}
		});
		consumer.handler(recordHandler) //
				.infinite(true) //
				.subscribe(Collections.singletonList(NODE_TOPIC)) //
				.onSuccess(v -> done.complete(null));

		done.thenRun(() -> logger.warn("Should never happen in inifinite mode"));

	}

	private void setNodeType(NodeInfo node) {
		if (node.info.product.equals("bm-crp")) {
			node.type = NodeType.CRP;
		} else {
			Optional<ZkNode> associatedZkNode = isManagedByCrp(node.info.installationId);
			associatedZkNode.ifPresentOrElse(zkNode -> {
				node.forestId = zkNode.forestId();
				if (node.isCloningState()) {
					node.type = NodeType.TAIL;
				} else {
					node.type = NodeType.MASTER;
				}
			}, () -> {
				node.type = NodeType.FORK;
				node.forestId = node.info.address;
			});
		}
	}

	private Optional<ZkNode> isManagedByCrp(String installationId) {
		String cleanedId = installationId.replace("bluemind-", "");
		return zkInstances.stream().filter(zk -> zk.installationId().equals(cleanedId)).findFirst();
	}

	private void updateMermaid() {
		Mermaid mermaid = new Mermaid(clusterNodes.asMap().values());
		mermaid.evaluate();
		setData(mermaid.getTopology(), mermaid.getMetrics(), mermaid.getMetricsAsMap());
	}

	public void update(NodeInfo node) {
		setNodeType(node);
		clusterNodes.put(node.id(), node);
		updateMermaid();
	}

	public static void setData(String topology, String metrics, Map<String, Long> map) {
		DataState.topology = topology;
		DataState.metrics = metrics;
		DataState.metricKeys = getMetricKeys(map);

		map.forEach((metric, value) -> {
			Gauge gauge = gauges.computeIfAbsent(metric, key -> Gauge.build().name(metric).help(metric).register());
			gauge.set(value);
		});
	}

	public static String getMetrics() {
		return metrics;
	}

	public static String getMetricKeys(Map<String, Long> map) {
		final JsonArray jsonArray = new JsonArray(map.keySet().stream().toList());
		JsonObject obj = new JsonObject();
		obj.put("metrics", jsonArray);
		return obj.encode();
	}

	public static String getTopology() {
		return topology == null ? "" : topology;
	}

}
