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
package net.bluemind.lib.vertx.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import com.netflix.spectator.api.Registry;

import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

public class CircuitBreaker<T> {

	private static final Logger logger = LoggerFactory.getLogger(CircuitBreaker.class);
	private static final Registry registry = MetricsRegistry.get();
	private static final IdFactory idFactory = new IdFactory(MetricsRegistry.get(), CircuitBreaker.class);
	private final Function<T, String> partition;
	private final Multiset<String> errorCounts;
	private final String name;

	public CircuitBreaker(String name, Function<T, String> partition) {
		this.name = name;
		this.partition = partition;
		this.errorCounts = ConcurrentHashMultiset.create();
	}

	public void noticeError(T errorSource) {
		String partKey = partition.apply(errorSource);
		int newCount = errorCounts.add(partKey, 1);
		logger.warn("[{}] noticed error, errorCount: {}", partKey, newCount);
	}

	public void noticeSuccess(T errorSource) {
		errorCounts.remove(partition.apply(errorSource));
	}

	public <R> CompletableFuture<R> applyCall(Vertx vertx, T partitionable, Callable<R> to) {
		CompletableFuture<R> future = new CompletableFuture<>();
		String partKey = partition.apply(partitionable);
		int count = errorCounts.count(partKey);
		if (count == 0) {
			try {
				future.complete(to.call());
			} catch (Exception e) {
				future.completeExceptionally(e);
			}
		} else {
			long delayMs = Math.min(5, count) * 500;
			registry.counter(idFactory.name(name + ".circuitBreakerDelays", "delay", Long.toString(delayMs)))
					.increment();
			logger.warn("[{}] Adding a {}ms delay to error-prone operation, errorCount: {}", partKey, delayMs, count);
			vertx.setTimer(delayMs, tid -> {
				try {
					future.complete(to.call());
				} catch (Exception e) {
					future.completeExceptionally(e);
				}
			});
		}
		return future;
	}

	public <R> CompletableFuture<R> applyPromised(Vertx vertx, T partitionable, Supplier<CompletableFuture<R>> to) {
		String partKey = partition.apply(partitionable);
		int count = errorCounts.count(partKey);
		if (count == 0) {
			return to.get();
		} else {
			CompletableFuture<R> future = new CompletableFuture<>();
			long delayMs = Math.min(5, count) * 500;
			registry.counter(idFactory.name(name + ".circuitBreakerDelays", "delay", Long.toString(delayMs)))
					.increment();
			logger.warn("[{}] Adding a {}ms delay to error-prone operation, errorCount: {}", partKey, delayMs, count);
			vertx.setTimer(delayMs, tid -> {
				to.get().whenComplete((R res, Throwable ex) -> {
					if (ex != null) {
						future.completeExceptionally(ex);
					} else {
						future.complete(res);
					}
				});
			});
			return future;
		}
	}

}
