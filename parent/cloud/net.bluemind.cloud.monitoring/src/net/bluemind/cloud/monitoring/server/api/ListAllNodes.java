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
package net.bluemind.cloud.monitoring.server.api;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import com.typesafe.config.Config;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.central.reverse.proxy.model.common.kafka.KafkaAdminClient;
import net.bluemind.cloud.monitoring.server.api.model.NodeInfo;
import net.bluemind.cloud.monitoring.server.api.model.NodeType;
import net.bluemind.cloud.monitoring.server.zk.Forest;
import net.bluemind.cloud.monitoring.server.zk.ZkNode;
import net.bluemind.core.utils.JsonUtils;

public class ListAllNodes extends NodeConsumer<Set<NodeInfo>> {

	private final KafkaAdminClient adminClient;
	private final Config config;
	private final Vertx vertx;

	public ListAllNodes(KafkaAdminClient adminClient, Config config, Vertx vertx) {
		this.adminClient = adminClient;
		this.config = config;
		this.vertx = vertx;
	}

	@Override
	public void handle(HttpServerRequest request) {
		adminClient.listTopics().andThen((ret) -> {
			if (ret.failed()) {
				super.error(request, ret);
			} else {
				CompletableFuture<Set<NodeInfo>> clusterNodes = ret.map(nodes -> nodes.stream()
						.filter(this::isNodeInfoTopic).findFirst().map(node -> this.retrieveBlueMindNodes(node))
						.orElse(CompletableFuture.completedFuture(Collections.emptySet()))).result();
				response(request, clusterNodes);
			}
		});

	}

	private CompletableFuture<Set<NodeInfo>> retrieveBlueMindNodes(String nodeInfo) {
		return getNodes(nodeInfo).thenApply(this::filterNodes).thenApply(this::setNodeType);
	}

	private CompletableFuture<Set<NodeInfo>> getNodes(String nodeInfo) {
		Set<NodeInfo> nodes = new HashSet<>();
		CompletableFuture<Set<NodeInfo>> completion = new CompletableFuture<>();

		Handler<ConsumerRecord<String, String>> recordHandler = (record) -> {
			NodeInfo info = JsonUtils.read(record.value(), NodeInfo.class);
			info.timestamp = record.timestamp();
			nodes.remove(info);
			nodes.add(info);
		};

		super.consume(config, vertx, nodeInfo, recordHandler, () -> completion.complete(nodes));

		return completion;
	}

	private Set<NodeInfo> filterNodes(Set<NodeInfo> nodes) {
		nodes.removeIf(
				node -> node.state.equals("CORE_STATE_NOT_INSTALLED") || "bluemind-noid".equals(node.installationId));
		return nodes;
	}

	private Set<NodeInfo> setNodeType(Set<NodeInfo> nodes) {
		Set<ZkNode> zkInstances = null;
		try (Forest forestInstanceLoader = new Forest(config)) {
			zkInstances = forestInstanceLoader.whiteListedInstances();
		}

		for (NodeInfo node : nodes) {
			if (node.product.equals("bm-crp")) {
				node.type = NodeType.CRP;
				node.forestId = node.installationId;
				node.installationId = null;
			} else {
				Optional<ZkNode> associatedZkNode = isManagedByCrp(node.installationId, zkInstances);
				associatedZkNode.ifPresentOrElse(zkNode -> {
					node.forestId = zkNode.forestId;
					if (node.state.equals("CORE_STATE_CLONING")) {
						node.type = NodeType.TAIL;
					} else {
						node.type = NodeType.MASTER;
					}
				}, () -> node.type = NodeType.FORK);
			}
		}
		return nodes;
	}

	private Optional<ZkNode> isManagedByCrp(String installationId, Set<ZkNode> zkInstances) {
		String cleanedId = installationId.replace("bluemind-", "");
		return zkInstances.stream().filter(zk -> zk.installationId.equals(cleanedId)).findFirst();
	}

	@Override
	protected String toJson(Set<NodeInfo> data) {
		return JsonUtils.asString(data);
	}

}
