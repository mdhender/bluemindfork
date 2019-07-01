/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.system.state;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import com.google.common.io.Files;
import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.Gauge;
import com.netflix.spectator.api.Registry;

import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Producer;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.system.api.SystemState;

public class StateBroadcastingVerticle extends BusModBase {

	private static final Logger logger = LoggerFactory.getLogger(StateBroadcastingVerticle.class);

	public static final class Factory implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new StateBroadcastingVerticle();
		}

	}

	private static final AtomicLong lastSend = new AtomicLong();

	public void start() {
		super.start();
		logger.info("State broadcast verticle starting...");
		lastSend.set(MQ.clusterTime());

		Registry reg = MetricsRegistry.get();
		IdFactory metricsId = new IdFactory("heartbeat", reg, StateBroadcastingVerticle.class);
		Producer producer = MQ.getProducer(Topic.CORE_NOTIFICATIONS);

		Gauge latencyValue = reg.gauge(metricsId.name("period"));
		Gauge maxLatency = reg.maxGauge(metricsId.name("maxPeriod"));

		JsonObject origin = new JsonObject();
		// this exists since bm4
		File loc = new File("/etc/bm/server.uid");
		if (loc.exists()) {
			try {
				String location = Files.asCharSource(loc, StandardCharsets.UTF_8).readFirstLine();
				origin.putString("datalocation", location);
			} catch (IOException e) {
			}
		}
		origin.putString("product", metricsId.product());

		eb.registerHandler(SystemState.BROADCAST, (Message<JsonObject> msg) -> {
			JsonObject forCluster = msg.body().copy();
			String operation = forCluster.getString("operation", "undefined");

			long clusterTime = MQ.clusterTime();
			long previousTime = lastSend.getAndSet(clusterTime);
			long latency = Math.abs(clusterTime - previousTime);
			latencyValue.set(latency);
			maxLatency.set(latency);
			forCluster.putNumber("send-time", clusterTime);
			forCluster.putObject("origin", origin);

			Counter heartbeatsCounter = reg.counter(metricsId.name("broadcast", "state", operation));
			heartbeatsCounter.increment();

			producer.send(forCluster);
		});
		logger.info("State broadcast verticle started.");
	}

}
