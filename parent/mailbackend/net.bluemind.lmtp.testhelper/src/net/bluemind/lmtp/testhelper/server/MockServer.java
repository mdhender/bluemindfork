/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.lmtp.testhelper.server;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.bluemind.lmtp.testhelper.model.MockServerStats;
import net.bluemind.vertx.testhelper.Deploy;

public class MockServer {

	public static final String BANNER = "220 mock server ready";

	private static final Set<String> deployed = new HashSet<>();
	public static final List<CompletableFuture<Void>> closeListeners = new LinkedList<>();

	public static void start() {
		Deploy.verticles(false, MockServerVerticle::new).thenAccept(depIds -> deployed.addAll(depIds)).join();
	}

	public static void stop() {
		Deploy.afterTest(deployed);
	}

	public static CompletableFuture<Void> expectClose() {
		CompletableFuture<Void> cf = new CompletableFuture<>();
		closeListeners.add(cf);
		return cf;
	}

	public static void closeEvent() {
		MockServerStats.get().disconnect();
		for (CompletableFuture<Void> cf : closeListeners) {
			if (!cf.isDone()) {
				cf.complete(null);
			}
		}
	}

}
