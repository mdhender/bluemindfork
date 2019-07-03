/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.vertx.testhelper;

import java.util.concurrent.CompletableFuture;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.lib.vertx.VertxPlatform;

public class Bus {

	public static <PAYLOAD> CompletableFuture<JsonObject> fetchJson(String dest, PAYLOAD msg) {
		CompletableFuture<JsonObject> result = new CompletableFuture<>();
		VertxPlatform.eventBus().sendWithTimeout(dest, msg, 10000, (AsyncResult<Message<JsonObject>> vertxRes) -> {
			if (vertxRes.succeeded()) {
				result.complete(vertxRes.result().body());
			} else {
				result.completeExceptionally(vertxRes.cause());
			}
		});
		return result;
	}

	public static void onMessage(String addr, Handler<Void> onNotif) {
		VertxPlatform.eventBus().registerLocalHandler(addr, (Message<?> msg) -> onNotif.handle(null));
	}

}
