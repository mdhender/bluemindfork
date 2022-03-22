/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.backend.mail.replica.service.tests;

import java.util.concurrent.atomic.LongAdder;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;

public class CircuitBreaksCounter extends AbstractVerticle {

	private static final LongAdder adder = new LongAdder();

	@Override
	public void start() throws Exception {
		vertx.eventBus().consumer("circuit-breaker." + "replication", (Message<JsonObject> msg) -> {
			long cnt = msg.body().getLong("count");
			adder.add(cnt);
			System.err.println("It broke :'( (+" + cnt + ")");
		});
	}

	public static final long breaks() {
		return adder.sum();
	}

	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new CircuitBreaksCounter();
		}

	}

}
