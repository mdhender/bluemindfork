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

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.lib.vertx.VertxPlatform;

public class Bus {

	private Bus() {
	}

	public static <P> CompletableFuture<JsonObject> fetchJson(String dest, P msg) {
		CompletableFuture<JsonObject> result = new CompletableFuture<>();
		VertxPlatform.eventBus().request(dest, msg, new DeliveryOptions().setSendTimeout(10000),
				(AsyncResult<Message<JsonObject>> vertxRes) -> {
					if (vertxRes.succeeded()) {
						result.complete(vertxRes.result().body());
					} else {
						result.completeExceptionally(vertxRes.cause());
					}
				});
		return result;
	}

	public static void onMessage(String addr, Handler<Void> onNotif) {
		VertxPlatform.eventBus().consumer(addr, msg -> onNotif.handle(null));
	}

}
