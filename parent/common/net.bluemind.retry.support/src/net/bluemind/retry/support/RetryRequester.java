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

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.netflix.spectator.api.Clock;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Timer;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

public class RetryRequester {

	private final String addr;
	private final EventBus eb;
	private final DeliveryOptions delOpts;
	private final Registry reg;
	private final IdFactory idFactory;
	private final Timer timer;
	private final Clock clock;

	public RetryRequester(EventBus eb, String topic) {
		this.addr = "retry." + topic;
		this.eb = eb;
		this.delOpts = new DeliveryOptions().setSendTimeout(10000);
		this.reg = MetricsRegistry.get();
		this.idFactory = new IdFactory("retry", reg, RetryRequester.class);
		Id timingId = idFactory.name("requester.timing", "topic", topic);
		this.timer = reg.timer(timingId);
		this.clock = reg.clock();
	}

	public void request(JsonObject js) {
		long start = clock.monotonicTime();
		CompletableFuture<Void> block = new CompletableFuture<>();
		CompletableFuture.runAsync(() -> {
			eb.request(addr, js, delOpts).andThen(ar -> {
				if (ar.failed()) {
					block.completeExceptionally(ar.cause());
				} else {
					block.complete(null);
				}
				long delta = clock.monotonicTime() - start;
				timer.record(Duration.ofNanos(delta));
			});
		});
		block.orTimeout(10, TimeUnit.SECONDS).join();
	}

}
