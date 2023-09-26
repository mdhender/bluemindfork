/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.retry.support;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

public class RetryRequester {

	private String addr;
	private EventBus eb;
	private DeliveryOptions delOpts;

	public RetryRequester(EventBus eb, String topic) {
		this.addr = "retry." + topic;
		this.eb = eb;
		this.delOpts = new DeliveryOptions().setSendTimeout(1000);
	}

	public void request(JsonObject js) {
		CompletableFuture<Void> block = new CompletableFuture<>();
		CompletableFuture.runAsync(() -> eb.request(addr, js, delOpts).andThen(ar -> {
			if (ar.failed()) {
				block.completeExceptionally(ar.cause());
			} else {
				block.complete(null);
			}
		}));
		block.orTimeout(10, TimeUnit.SECONDS).join();
	}

}
