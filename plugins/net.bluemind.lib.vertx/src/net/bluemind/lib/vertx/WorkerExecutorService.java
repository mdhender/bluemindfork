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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.lib.vertx;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import io.vertx.core.WorkerExecutor;

public class WorkerExecutorService extends AbstractExecutorService {

	private boolean shutdown;
	private final WorkerExecutor exec;
	private Optional<Runnable> preRunExecution;

	public WorkerExecutorService(String name, int size, long timeout, TimeUnit unit) {
		this(name, size, timeout, unit, null);
	}

	public WorkerExecutorService(String name, int size, Duration timeout) {
		this(name, size, timeout.toMillis(), TimeUnit.MILLISECONDS);
	}

	public WorkerExecutorService(String name, int size, long timeout, TimeUnit unit, Runnable preRunExecution) {
		super();
		this.preRunExecution = Optional.ofNullable(preRunExecution);
		this.exec = VertxPlatform.getVertx().createSharedWorkerExecutor(name, size, timeout, unit);
	}

	public WorkerExecutor impl() {
		return exec;
	}

	@Override
	public void shutdown() {
		exec.close();
		shutdown = true;
	}

	@Override
	public List<Runnable> shutdownNow() {
		return Collections.emptyList();
	}

	@Override
	public boolean isShutdown() {
		return shutdown;
	}

	@Override
	public boolean isTerminated() {
		return isShutdown();
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return shutdown;
	}

	@Override
	public void execute(Runnable command) {
		exec.executeBlocking(() -> {
			preRunExecution.ifPresent(Runnable::run);
			command.run();
			return null;
		}, false);
	}

}
