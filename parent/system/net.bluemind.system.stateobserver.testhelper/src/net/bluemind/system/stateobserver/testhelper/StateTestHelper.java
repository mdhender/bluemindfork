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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.state.StateContext;

public class StateTestHelper {

	public static CompletableFuture<Void> runningLatch = new CompletableFuture<>();

	/**
	 * This helper is useful when a unit test needs core and a component that
	 * observes state (eg. ips which blocks imap connections when state is not
	 * running).
	 */
	public static void blockUntilRunning() {
		if (runningLatch.isDone()) {
			System.out.println("We reached running state at least one time");
			return;
		}
		StateContext.setState("core.started");
		StateContext.setState("core.upgrade.start");
		StateContext.setState("core.upgrade.end");
		Vertx vertx = VertxPlatform.getVertx();
		// simulate how the hearbeat would flow from one component to another
		vertx.eventBus().publish(Topic.CORE_NOTIFICATIONS,
				new JsonObject().putString("operation", "core.state.running"));
		vertx.setPeriodic(4000, tid -> {
			vertx.eventBus().publish(Topic.CORE_NOTIFICATIONS,
					new JsonObject().putString("operation", "core.state.running"));
		});
		try {
			runningLatch.get(1, TimeUnit.MINUTES);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new RuntimeException("Failed to reach running state in 1min.", e);
		}
	}

}
