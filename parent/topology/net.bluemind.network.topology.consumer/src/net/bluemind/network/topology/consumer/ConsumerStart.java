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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.network.topology.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.json.JsonObject;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Producer;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;

public class ConsumerStart extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(ConsumerStart.class);

	public static class Factory implements IUniqueVerticleFactory, IVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new ConsumerStart();
		}

	}

	@Override
	public void start() {
		MQ.init(() -> {
			TopologyConsumer consumer = new TopologyConsumer();
			MQ.registerConsumer("topology.updates", consumer);
			String jvmId = System.getProperty("net.bluemind.property.product", "unknown-jvm");
			logger.info("Consumer registered on topology.updates, requesting changes for {}", jvmId);
			Producer prod = MQ.registerProducer("topology.requests");
			prod.send(new JsonObject().put("origin", jvmId));
		});
	}

}
