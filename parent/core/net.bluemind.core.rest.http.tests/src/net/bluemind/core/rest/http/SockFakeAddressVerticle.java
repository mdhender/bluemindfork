/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.core.rest.http;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.lib.vertx.IVerticleFactory;

public class SockFakeAddressVerticle extends AbstractVerticle {

	public static class Factory implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new SockFakeAddressVerticle();
		}

	}

	@Override
	public void start() throws Exception {
		EventBus eb = vertx.eventBus();
		AtomicReference<String> ack = new AtomicReference<>();
		eb.consumer("sockjs.tests.rocks", (Message<JsonObject> msg) -> {
			JsonObject js = msg.body();
			if (js.getString("ack") != null) {
				ack.set(js.getString("ack"));
			}
			msg.reply(msg.body());
		});
		vertx.setPeriodic(1, tid -> {
			JsonObject msg = new JsonObject().put("time", System.currentTimeMillis());
			Optional.ofNullable(ack.get()).ifPresent(a -> msg.put("ack", a));
			eb.publish("sockjs.tests.rocks", msg);
		});
	}

}
