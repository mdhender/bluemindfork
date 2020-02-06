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
package net.bluemind.vertx.testhelper.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public class DoneHandler<T> implements Handler<AsyncResult<T>> {

	private Set<T> value;
	private AtomicInteger steps;
	private CompletableFuture<Set<T>> future;
	private static final Logger logger = LoggerFactory.getLogger(DoneHandler.class);

	public DoneHandler(int steps) {
		this.steps = new AtomicInteger(steps);
		value = new HashSet<>();
		this.future = new CompletableFuture<Set<T>>();
		if (steps == 0) {
			this.future.complete(value);
		}
		logger.debug("Created, remaining {}", steps);
	}

	@Override
	public void handle(AsyncResult<T> event) {
		int remaining = steps.decrementAndGet();
		logger.info("{} Remaining: {}, success: {}", this, remaining, event.succeeded());
		if (event.succeeded()) {
			T result = event.result();
			if (result != null) {
				value.add(event.result());
			}
			if (remaining <= 0) {
				future.complete(value);
			}
		} else {
			logger.warn("undeploy failed.");
			future.completeExceptionally(event.cause());
		}
	}

	public CompletableFuture<Set<T>> promise() {
		return future;
	}

}
