/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.bluemind.lib.vertx.BMExecutor.BMTask;
import net.bluemind.lib.vertx.BMExecutor.BMTaskMonitor;

public class BMExecutorService implements ExecutorService {

	private final BMExecutor bme;

	public BMExecutorService(BMExecutor bme) {
		this.bme = bme;
	}

	@Override
	public void execute(Runnable command) {
		bme.execute(new BMTask() {

			@Override
			public void run(BMTaskMonitor monitor) {
				command.run();
			}

			@Override
			public void cancelled() {
			}
		});
	}

	@Override
	public void shutdown() {
	}

	@Override
	public List<Runnable> shutdownNow() {
		return Collections.emptyList();
	}

	@Override
	public boolean isShutdown() {
		return false;
	}

	@Override
	public boolean isTerminated() {
		return false;
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return false;
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		CompletableFuture<T> fut = new CompletableFuture<>();
		bme.execute(new BMTask() {

			@Override
			public void run(BMTaskMonitor monitor) {
				try {
					T result = task.call();
					fut.complete(result);
				} catch (Exception e) {
					fut.completeExceptionally(e);
				}
			}

			@Override
			public void cancelled() {
				fut.cancel(true);
			}
		});
		return fut;
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		CompletableFuture<T> fut = new CompletableFuture<>();
		bme.execute(new BMTask() {

			@Override
			public void run(BMTaskMonitor monitor) {
				try {
					task.run();
					fut.complete(result);
				} catch (Exception e) {
					fut.completeExceptionally(e);
				}
			}

			@Override
			public void cancelled() {
				fut.cancel(true);
			}
		});
		return fut;
	}

	@Override
	public Future<?> submit(Runnable task) {
		return submit(task, (Void) null);
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		throw new UnsupportedOperationException("invokeAll");
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException {
		throw new UnsupportedOperationException("invokeAll with timeout");
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		throw new UnsupportedOperationException("invokeAny");
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		throw new UnsupportedOperationException("invokeAny with timeout");
	}

}
