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
package net.bluemind.cloud.monitoring.server.grafana;

import java.util.concurrent.CompletableFuture;

import com.typesafe.config.Config;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.central.reverse.proxy.model.common.kafka.KafkaAdminClient;
import net.bluemind.cloud.monitoring.server.api.NodeConsumer;
import net.bluemind.cloud.monitoring.server.api.model.DataState;

public class Metrics extends NodeConsumer<String> {

	private final KafkaAdminClient adminClient;
	private final Config config;
	private final Vertx vertx;

	public Metrics(KafkaAdminClient adminClient, Config config, Vertx vertx) {
		this.adminClient = adminClient;
		this.config = config;
		this.vertx = vertx;
	}

	@Override
	public void handle(HttpServerRequest request) {
		request.response().headers().add("Access-Control-Allow-Origin", "*");
		super.response(request, CompletableFuture.completedFuture(""));

	}

	@Override
	protected String toJson(String data) {
		return DataState.getMetrics();
	}

}
