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
package net.bluemind.cloud.monitoring.server;

import com.typesafe.config.Config;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.central.reverse.proxy.model.common.kafka.KafkaAdminClient;
import net.bluemind.cloud.monitoring.server.api.ListAllBrokers;
import net.bluemind.cloud.monitoring.server.api.ListAllNodes;
import net.bluemind.cloud.monitoring.server.api.ListLog;
import net.bluemind.cloud.monitoring.server.grafana.Metrics;
import net.bluemind.cloud.monitoring.server.grafana.Topology;
import net.bluemind.lib.vertx.RouteMatcher;

public class MonitoringRouter {

	public static Handler<HttpServerRequest> create(Vertx vertx, Config config, KafkaAdminClient adminClient) {

		RouteMatcher routeMatcher = new RouteMatcher(vertx);
		routeMatcher.noMatch((request) -> request.response().setStatusCode(503).end());

		routeMatcher.get("/monitoring/nodes", new ListAllNodes(adminClient, config, vertx));
		routeMatcher.get("/monitoring/brokers", new ListAllBrokers(adminClient, config, vertx));
		routeMatcher.get("/monitoring/topic/raw", new ListLog(adminClient, config, vertx));

		// grafana
		routeMatcher.get("/monitoring/metrics", new Metrics(adminClient, config, vertx));
		routeMatcher.get("/monitoring/topology", new Topology(adminClient, config, vertx));

		return routeMatcher;
	}

}
