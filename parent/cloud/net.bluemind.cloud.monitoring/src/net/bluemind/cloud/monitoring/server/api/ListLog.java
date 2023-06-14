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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import com.typesafe.config.Config;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.central.reverse.proxy.model.common.kafka.KafkaAdminClient;
import net.bluemind.core.utils.JsonUtils;

public class ListLog extends NodeConsumer<List<String>> {

	private final KafkaAdminClient adminClient;
	private final Config config;
	private final Vertx vertx;

	public ListLog(KafkaAdminClient adminClient, Config config, Vertx vertx) {
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
				CompletableFuture<List<String>> clusterNodes = ret.map(nodes -> nodes.stream()
						.filter(this::isNodeInfoTopic).findFirst().map(this::retrieveBlueMindNodes)
						.orElse(CompletableFuture.completedFuture(Collections.emptyList()))).result();
				super.response(request, clusterNodes);
			}
		});

	}

	private CompletableFuture<List<String>> retrieveBlueMindNodes(String nodeInfo) {
		List<String> log = new ArrayList<>();
		CompletableFuture<List<String>> completion = new CompletableFuture<>();

		Handler<ConsumerRecord<String, String>> recordHandler = (record) -> {
			log.add(record.timestamp() + ": " + record.value());
		};

		super.consume(config, vertx, nodeInfo, recordHandler, () -> completion.complete(log));

		return completion;
	}

	@Override
	protected String toJson(List<String> data) {
		return JsonUtils.asString(data);
	}

}
