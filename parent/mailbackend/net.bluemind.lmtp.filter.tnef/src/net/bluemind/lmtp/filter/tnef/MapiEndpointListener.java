/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.lmtp.filter.tnef;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;

public class MapiEndpointListener extends AbstractVerticle {

	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new MapiEndpointListener();
		}

	}

	@Override
	public void start() throws Exception {
		MQ.init(() -> MQ.registerConsumer("mapi.server.endpoint", msg -> {
			String tnefEndpoint = msg.toJson().getString("tnef");
			MapiEndpoint.register(tnefEndpoint);
		}));

	}

}
