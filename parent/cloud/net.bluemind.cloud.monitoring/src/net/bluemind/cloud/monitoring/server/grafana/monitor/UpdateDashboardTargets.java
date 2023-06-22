/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.cloud.monitoring.server.grafana.monitor;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.cloud.monitoring.server.grafana.monitor.GrafanaConfig.GrafanaConfigRoot;
import net.bluemind.lib.grafana.exception.GrafanaException;

public class UpdateDashboardTargets extends AbstractVerticle {

	public static final String TOPOLOGY_CHANGED = "topology.changed";
	public static final Logger logger = LoggerFactory.getLogger(UpdateDashboardTargets.class);

	@Override
	public void start(Promise<Void> startPromise) throws Exception {

		if (GrafanaConfig.get().getBoolean(GrafanaConfigRoot.ACTIVE)) {
			vertx.eventBus().consumer(TOPOLOGY_CHANGED, event -> {
				JsonArray jsonMetrics = ((JsonObject) event.body()).getJsonArray("metrics");
				List<String> metrics = jsonMetrics.stream().map(Object::toString).toList();
				vertx.executeBlocking(p -> {
					try {
						GrafanaVisualization.update(metrics);
					} catch (GrafanaException e) {
						logger.error(e.getMessage());
					} catch (InterruptedException e) {
						logger.error(e.getMessage());
						Thread.currentThread().interrupt();
					}
					p.complete();
				});
			});
		}

		startPromise.complete();
	}

}
