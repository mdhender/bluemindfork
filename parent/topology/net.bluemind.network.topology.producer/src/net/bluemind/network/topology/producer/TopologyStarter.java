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
package net.bluemind.network.topology.producer;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.json.JsonObject;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.lib.vertx.IVerticlePriority;

public class TopologyStarter extends AbstractVerticle {

	public static class Factory implements IVerticleFactory, IVerticlePriority {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new TopologyStarter();
		}

		@Override
		public int getPriority() {
			return 998;
		}

	}

	@Override
	public void start() {
		vertx.setTimer(500, tid -> vertx.eventBus().publish("topology.internal.startup", new JsonObject()));
	}

}
