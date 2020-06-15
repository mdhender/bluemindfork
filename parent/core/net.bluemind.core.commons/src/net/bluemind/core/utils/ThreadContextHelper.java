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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.utils;

import java.util.concurrent.CompletableFuture;

import net.bluemind.lib.vertx.VertxPlatform;

public final class ThreadContextHelper {

	private ThreadContextHelper() {

	}

	public static <T> CompletableFuture<T> inWorkerThread(CompletableFuture<T> eventLoopFuture) {
		CompletableFuture<T> dependentsInWorkerThread = new CompletableFuture<>();
		eventLoopFuture.thenAccept(ident -> VertxPlatform.getVertx().executeBlocking(prom -> {
			dependentsInWorkerThread.complete(ident);
			prom.complete();
		}, false, res -> {
		}));
		return dependentsInWorkerThread;
	}
}
