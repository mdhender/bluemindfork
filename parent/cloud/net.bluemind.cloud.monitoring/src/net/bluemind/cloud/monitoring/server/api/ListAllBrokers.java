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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.common.Node;

import com.typesafe.config.Config;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.central.reverse.proxy.model.common.kafka.KafkaAdminClient;
import net.bluemind.cloud.monitoring.server.api.model.KafkaNode;
import net.bluemind.core.utils.JsonUtils;

public class ListAllBrokers extends ApiCall<List<Node>> {

	private final KafkaAdminClient adminClient;

	public ListAllBrokers(KafkaAdminClient adminClient, Config config, Vertx vertx) {
		this.adminClient = adminClient;
	}

	@Override
	public void handle(HttpServerRequest request) {

		adminClient.describeCluster().andThen((ret) -> {
			if (ret.failed()) {
				super.error(request, ret);
			} else {
				super.response(request, CompletableFuture.completedFuture(new ArrayList<>(ret.result())));
			}
		});

	}

	@Override
	protected String toJson(List<Node> data) {
		return JsonUtils.asString(data.stream().map(n -> {
			KafkaNode node = new KafkaNode();
			node.host = n.host();
			node.id = n.id();
			node.idString = n.idString();
			node.port = n.port();
			node.rack = n.rack();
			return node;
		}).toList());
	}

}
