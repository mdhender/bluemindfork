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
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;

public class ReplicationLatencyTimer {

	private static final Logger logger = LoggerFactory.getLogger(ReplicationLatencyTimer.class);

	private final Vertx vertx;
	private final JsonObject probe;
	private final String domainUid;

	public ReplicationLatencyTimer(Vertx vx, SharedMailboxProbe probe) {
		this.vertx = vx;
		this.probe = Probe.of(probe).toJson();
		this.domainUid = probe.domainUid();
	}

	public void start() {
		vertx.setTimer(10000, tid -> triggerProbe());
	}

	private void triggerProbe() {
		ItemValue<Domain> domain = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomains.class).get(domainUid);
		if (domain == null) {
			logger.warn("Domain not found {}", domainUid);
			return;
		}

		vertx.eventBus().request("replication.latency.probe", probe, new DeliveryOptions().setSendTimeout(90000),
				result -> {
					if (result.failed()) {
						logger.error("probing failed: {}", result.cause().getMessage());
					}
					start();
				});
	}

}
