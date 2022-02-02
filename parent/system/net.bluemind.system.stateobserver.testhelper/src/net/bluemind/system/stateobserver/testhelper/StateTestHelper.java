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
package net.bluemind.system.stateobserver.testhelper;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.api.SystemState;
import net.bluemind.system.state.StateContext;

public class StateTestHelper {

	public static final String BUS_ADDR = "state.test.internal";
	private static final Logger logger = LoggerFactory.getLogger(StateTestHelper.class);

	private static final AtomicReference<Long> timerId = new AtomicReference<>();

	private StateTestHelper() {

	}

	/**
	 * This helper is useful when a unit test needs core and a component that
	 * observes state (eg. ips which blocks imap connections when state is not
	 * running).
	 */
	public static synchronized CompletableFuture<Void> blockUntilRunning() {
		if (StateContext.getState() == SystemState.CORE_STATE_RUNNING) {
			return CompletableFuture.completedFuture(null);
		}

		CompletableFuture<Void> doneProm = new CompletableFuture<>();

		Vertx vertx = VertxPlatform.getVertx();
		EventBus eb = vertx.eventBus();

		MessageConsumer<String> cons = eb.consumer(BUS_ADDR);
		cons.handler(done -> {
			cons.unregister();
			logger.info("running event received '{}'", done.body());
			doneProm.complete(null);
		});

		StateContext.setState("core.started");
		StateContext.setState("core.upgrade.start");
		StateContext.setState("core.upgrade.end");

		if (timerId.get() != null) {
			vertx.cancelTimer(timerId.get());
		}
		// simulate how the hearbeat would flow from one component to another
		vertx.eventBus().publish(Topic.CORE_NOTIFICATIONS, new JsonObject().put("operation", "core.state.running"));
		timerId.set(vertx.setPeriodic(4000,
				tid -> eb.publish(Topic.CORE_NOTIFICATIONS, new JsonObject().put("operation", "core.state.running"))));
		return doneProm;
	}

}
