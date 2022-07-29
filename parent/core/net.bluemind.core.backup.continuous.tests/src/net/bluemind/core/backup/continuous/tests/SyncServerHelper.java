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
package net.bluemind.core.backup.continuous.tests;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.vertx.core.json.JsonObject;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.api.SystemState;

public class SyncServerHelper {

	public static void waitFor() throws InterruptedException {

		VertxPlatform.eventBus().publish(SystemState.BROADCAST,
				new JsonObject().put("operation", SystemState.CORE_STATE_RUNNING.operation()));
		CountDownLatch latch = new CountDownLatch(1);
		check(latch);
		latch.await(120, TimeUnit.SECONDS);
	}

	private static void check(CountDownLatch latch) throws InterruptedException {
		for (int i = 0; i < 5; i++) {
			String ip = "127.0.0.1";
			int port = 2501;
			try {
				Socket s = new Socket();
				s.connect(new InetSocketAddress(ip, port), 2000);
				s.close();
				latch.countDown();
				return;
			} catch (IOException e1) {
			}
			Thread.sleep(1000);
		}

	}

}
