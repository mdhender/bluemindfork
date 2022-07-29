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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.cyrus.replication.server.tests;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.vertx.core.json.JsonObject;
import net.bluemind.backend.cyrus.replication.client.SyncClient;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.api.SystemState;

public class SyncServerHelper {

	public static void waitFor() throws InterruptedException {

		VertxPlatform.eventBus().publish(SystemState.BROADCAST,
				new JsonObject().put("operation", SystemState.CORE_STATE_RUNNING.operation()));

		CountDownLatch latch = new CountDownLatch(1);
		check(latch);
		latch.await(60, TimeUnit.SECONDS);
	}

	private static void check(CountDownLatch latch) throws InterruptedException {
		AtomicBoolean connected = new AtomicBoolean();
		for (int i = 0; i < 5; i++) {
			if (connected.get()) {
				break;
			}
			SyncClient sc = new SyncClient("127.0.0.1", 2501);
			try {
				sc.connect().thenCompose(v -> {
					latch.countDown();
					connected.set(true);
					return sc.disconnect();
				}).exceptionally(e -> {
					// not connected
					return null;
				}).get(2, TimeUnit.SECONDS);
			} catch (Exception e) {
			}
			if (!connected.get()) {
				Thread.sleep(1000);
			} else {
				return;
			}
		}

	}

}
