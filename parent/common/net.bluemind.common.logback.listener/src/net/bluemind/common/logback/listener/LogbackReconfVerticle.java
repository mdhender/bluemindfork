/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.common.logback.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.json.JsonObject;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;

public class LogbackReconfVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(LogbackReconfVerticle.class);

	public static class Reg implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new LogbackReconfVerticle();
		}

	}

	@Override
	public void start() {
		logger.info("LogbackReconfVerticle starting");
		MQ.init().thenAccept(v -> {
			logger.info("LogbackReconfVerticle init done");
			MQ.registerConsumer(Topic.LOGBACK_CONFIG, msg -> {
				logger.info("received message {}", msg.toJson().encodePrettily());
				JsonObject js = msg.toJson();
				String endpoint = js.getString("endpoint", "unknown");
				String user = js.getString("user");
				if (user != null) {
					boolean enabled = js.getBoolean("enabled", false);
					String toSet = user + "." + endpoint + ".logging";
					String curval = System.getProperty(toSet);
					System.setProperty(toSet, Boolean.toString(enabled));
					logger.warn("Per-user logging {}@{} => {} (prop {} value was '{}')", user, endpoint, enabled, toSet,
							curval);
				}
			});
			logger.info("Waiting for logback re-configuration orders on topic {}", Topic.LOGBACK_CONFIG);
		});
	}

}