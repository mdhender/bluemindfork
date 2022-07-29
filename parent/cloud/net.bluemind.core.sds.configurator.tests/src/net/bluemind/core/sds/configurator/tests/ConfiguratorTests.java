/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.core.sds.configurator.tests;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.lib.vertx.VertxPlatform;

public class ConfiguratorTests {

	@Before
	public void before() throws InterruptedException, ExecutionException, TimeoutException {
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
	}

	@Test
	public void reconfigure() throws InterruptedException, ExecutionException, TimeoutException {
		EventBus eb = VertxPlatform.eventBus();
		JsonObject payload = new JsonObject()//
				.put("backend", "127.0.0.1")//
				.put("config", new JsonObject())//
		;

		CompletableFuture<Void> cf = new CompletableFuture<>();
		eb.request("sds.sysconf.changed", payload, new DeliveryOptions().setSendTimeout(5000),
				(AsyncResult<Message<Boolean>> result) -> {
					if (result.succeeded()) {
						cf.complete(null);
					} else {
						cf.completeExceptionally(result.cause());
					}
				});
		cf.get(10, TimeUnit.SECONDS);
	}

}
