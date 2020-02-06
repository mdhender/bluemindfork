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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.cyrus.replication.link.probe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import net.bluemind.backend.cyrus.replication.link.probe.LatencyMonitorWorker.Probe;

public class ReplicationLatencyTimer {

	private static final Logger logger = LoggerFactory.getLogger(ReplicationLatencyTimer.class);

	private final Vertx vertx;
	private final JsonObject probe;

	public ReplicationLatencyTimer(Vertx vx, SharedMailboxProbe probe) {
		this.vertx = vx;
		this.probe = Probe.of(probe).toJson();
	}

	public void start() {
		vertx.setTimer(10000, tid -> triggerProbe());
	}

	private void triggerProbe() {
		vertx.eventBus().request("replication.latency.probe", probe, new DeliveryOptions().setSendTimeout(90000),
				result -> {
					if (result.failed()) {
						logger.error("probing failed: {}", result.cause().getMessage());
					}
					start();
				});
	}

}
