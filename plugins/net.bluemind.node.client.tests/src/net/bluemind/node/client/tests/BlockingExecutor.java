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
package net.bluemind.node.client.tests;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BlockingExecutor implements ExecutorService {

	private final ExecutorService executorService;

	public BlockingExecutor(int numThreads) {
		BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<Runnable>(numThreads);
		final RejectedExecutionHandler rejectedExecutionHandler = new RejectedExecutionHandler() {

			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				// System.out.println("task " + r +
				// " rejected, sleeping a bit");
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
				}
				executor.execute(r);
			}
		};
		this.executorService = new ThreadPoolExecutor(numThreads, numThreads, 0L, TimeUnit.MILLISECONDS, blockingQueue,
				rejectedExecutionHandler);

	}

	public void execute(Runnable command) {
		executorService.execute(command);
	}

	public void shutdown() {
		executorService.shutdown();
	}

	public List<Runnable> shutdownNow() {
		return executorService.shutdownNow();
	}

	public boolean isShutdown() {
		return executorService.isShutdown();
	}

	public boolean isTerminated() {
		return executorService.isTerminated();
	}

	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return executorService.awaitTermination(timeout, unit);
	}

	public <T> Future<T> submit(Callable<T> task) {
		return executorService.submit(task);
	}

	public <T> Future<T> submit(Runnable task, T result) {
		return executorService.submit(task, result);
	}

	public Future<?> submit(Runnable task) {
		return executorService.submit(task);
	}

	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		return executorService.invokeAll(tasks);
	}

	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException {
		return executorService.invokeAll(tasks, timeout, unit);
	}

	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		return executorService.invokeAny(tasks);
	}

	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return executorService.invokeAny(tasks, timeout, unit);
	}

}
