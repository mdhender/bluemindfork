/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.system.stateobserver.internal;

import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.Gauge;
import com.netflix.spectator.api.Registry;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.hornetq.client.vertx.IMessageForwarder;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

public class CoreForward implements IMessageForwarder {

	private IdFactory metricsId;
	private Registry reg;
	private Gauge latency;
	private Gauge latencyMax;
	private Counter received;

	public CoreForward() {
		this.reg = MetricsRegistry.get();
		this.metricsId = new IdFactory("heartbeat.receiver", reg, CoreForward.class);
		latency = reg.gauge(metricsId.name("latency"));
		latencyMax = reg.maxGauge(metricsId.name("latencyMax"));
		received = reg.counter(metricsId.name("received"));
	}

	@Override
	public String getTopic() {
		return Topic.CORE_NOTIFICATIONS;
	}

	@Override
	public void forward(Vertx vertx, OOPMessage message) {
		received.increment();
		JsonObject toForward = message.toJson();
		long incomingClusterTime = toForward.getLong("send-time", 0L);
		if (incomingClusterTime > 0) {
			long curTime = MQ.clusterTime();
			long latencyMs = Math.abs(curTime - incomingClusterTime);
			latency.set(latencyMs);
			latencyMax.set(latencyMs);
		}
		vertx.eventBus().publish(Topic.CORE_NOTIFICATIONS, toForward);
	}

}
