/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ExecutorShutdownFuture extends CompletableFuture<Void> {
	private final ExecutorService executor;

	private ExecutorShutdownFuture(ExecutorService executor) {
		this.executor = executor;
	}

	public static ExecutorShutdownFuture create(ExecutorService executor) {
		ExecutorShutdownFuture future = new ExecutorShutdownFuture(executor);
		executor.shutdown();
		ExecutorShutdownFuture.schedule(future::tryToComplete);
		return future;
	}

	private void tryToComplete() {
		if (executor.isTerminated()) {
			complete(null);
		}

		ExecutorShutdownFuture.schedule(this::tryToComplete);
	}

	private static final ScheduledExecutorService SERVICE = Executors.newSingleThreadScheduledExecutor();

	private static void schedule(Runnable runnable) {
		SERVICE.schedule(runnable, 1, TimeUnit.SECONDS);
	}
}
