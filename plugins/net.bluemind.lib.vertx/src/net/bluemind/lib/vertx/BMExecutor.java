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
package net.bluemind.lib.vertx;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.impl.VertxThreadFactory;

import com.google.common.collect.ImmutableSet;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.patterns.PolledMeter;

import net.bluemind.core.utils.GlobalConstants;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

public class BMExecutor {

	private static final Logger logger = LoggerFactory.getLogger(BMExecutor.class);
	private static final int DEFAULT_QUEUE = Math.max(1024, 4 * 32 * (2 + Runtime.getRuntime().availableProcessors()));
	private static final int DEFAULT_WORKER_POOL_SIZE = Math.max(Runtime.getRuntime().availableProcessors() * 2, 30);
	private static final Vertx timerMgmt = VertxPlatform.getVertx();

	private volatile long lastFullLog = 0;
	private volatile long lastInfoLog = 0;

	private final ThreadPoolExecutor executor;
	private final ConcurrentHashMap<Runnable, Thread> runningThreads;
	private final LongAdder timedoutCounter;
	private final LongAdder rejectionCounter;
	private final String name;
	private final int threadCounts;
	private final BMExecutorService asExecutorService;
	private final Registry reg;
	private final IdFactory idFactory;

	public interface IHasPriority {
		int priority();
	}

	private static final class BMRejectedExecutionHandler implements RejectedExecutionHandler {

		private final LongAdder rejections;

		public BMRejectedExecutionHandler(LongAdder rejected) {
			this.rejections = rejected;
		}

		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			rejections.increment();
			if (r instanceof BMFutureTask) {
				BMFutureTask task = (BMFutureTask) r;
				task.cancel(false);
				task.task.cancelled();
			} else if (r instanceof FutureTask) {
				FutureTask<?> task = (FutureTask<?>) r;
				task.cancel(false);
			} else {
				logger.error("run rejected class {}, discard", r.getClass());
			}
		}
	}

	public interface BMTaskMonitor {

		public boolean alive();
	}

	private static final BMTaskMonitor DIRECT_MON = () -> true;

	private static final class InternalBMTaskMonitor implements BMTaskMonitor {
		private boolean cancelled;
		private long aliveTime;

		public InternalBMTaskMonitor() {
			aliveTime = System.currentTimeMillis();
		}

		public boolean alive() {
			if (cancelled) {
				return false;
			} else {
				aliveTime = System.currentTimeMillis();
				return true;
			}
		}

		public void cancelled() {
			this.cancelled = true;
		}
	}

	public interface BMTask extends IHasPriority {
		public void run(BMTaskMonitor monitor);

		public void cancelled();

		/**
		 * highest priority will run first
		 */
		default int priority() {
			return 0;
		}

	}

	private static final int priority(Runnable r) {
		return r instanceof IHasPriority ? ((IHasPriority) r).priority() : 0;
	}

	public BMExecutor(String name) {
		this(DEFAULT_WORKER_POOL_SIZE, name);
	}

	public BMExecutor(int thread, String name) {
		this.name = name;
		this.threadCounts = thread;
		this.reg = MetricsRegistry.get();
		this.idFactory = new IdFactory("executor", reg, BMExecutor.class);
		this.runningThreads = PolledMeter.using(reg).withId(idFactory.name("active", "name", name))
				.monitorSize(new ConcurrentHashMap<>());
		timedoutCounter = PolledMeter.using(reg).withId(idFactory.name("timedOut", "name", name))
				.monitorValue(new LongAdder());
		rejectionCounter = PolledMeter.using(reg).withId(idFactory.name("rejected", "name", name))
				.monitorValue(new LongAdder());

		BlockingQueue<Runnable> prioQueue = new PriorityBlockingQueue<>(DEFAULT_QUEUE,
				(o1, o2) -> Integer.compare(priority(o2), priority(o1)));

		this.executor = new ThreadPoolExecutor(thread, thread, 0L, TimeUnit.MILLISECONDS, prioQueue,
				new VertxThreadFactory(name), new BMRejectedExecutionHandler(rejectionCounter)) {

			@Override
			protected void beforeExecute(Thread t, Runnable r) {
				runningThreads.put(r, t);
				super.beforeExecute(t, r);
			}

			@Override
			protected void afterExecute(Runnable r, Throwable t) {
				super.afterExecute(r, t);
				runningThreads.remove(r);
			}

		};
		PolledMeter.using(reg).withId(idFactory.name("queued", "name", name)).monitorSize(executor.getQueue());
		this.asExecutorService = new BMExecutorService(this);

	}

	public ExecutorService asExecutorService() {
		return asExecutorService;
	}

	private static final String logMsg = "Executor/{} running: {} on {}, queue size: {}, total timed-out: {}, rejected: {}";

	protected void logExecutorState() {
		int active = runningThreads.size();
		if (active > (threadCounts * 0.8)) {
			long now = System.currentTimeMillis();
			if (lastInfoLog < (now - 2000)) {
				lastInfoLog = now;
				logger.info(logMsg, name, active, threadCounts, executor.getQueue().size(), timedoutCounter.longValue(),
						rejectionCounter.longValue());
			}

			if (lastFullLog < (now - 60000)) {
				lastFullLog = now;
				logRunningTasks((msg, params) -> logger.info(msg, params));
			}
		} else if (logger.isDebugEnabled() && active > (threadCounts * 0.5)) {
			logger.debug(logMsg, name, active, threadCounts, executor.getQueue().size(), timedoutCounter.longValue(),
					rejectionCounter.longValue());
		}
	}

	@FunctionalInterface
	private interface LogFunction {
		public void log(String message, Object... parameters);
	}

	private void logRunningTasks(LogFunction func) {
		Set<Entry<Runnable, Thread>> runningTasksSnapshot = ImmutableSet.copyOf(runningThreads.entrySet());
		runningTasksSnapshot.stream().forEach(entry -> {
			StackTraceElement[] stack = entry.getValue().getStackTrace();
			String stackAsString = Arrays.stream(stack).map(ste -> ste.toString()).collect(Collectors.joining("\n\t"));
			func.log("running thread: {} task {}, stack {}", entry.getValue(), entry.getKey(), stackAsString);
		});

	}

	public void execute(BMTask command) {
		execute(command, GlobalConstants.DEFAULT_TIMEOUT);
	}

	private static final class BMDirectTask extends FutureTask<Void> implements IHasPriority {

		private final int priority;

		public BMDirectTask(BMTask task) {
			super(() -> task.run(DIRECT_MON), null);
			this.priority = Math.max(10, task.priority());
		}

		@Override
		public int priority() {
			return priority;
		}

	}

	private class BMFutureTask extends FutureTask<Void> implements IHasPriority {

		private final InternalBMTaskMonitor monitor;
		private final BMTask task;
		private long timeoutId;

		public BMFutureTask(BMTask task, InternalBMTaskMonitor monitor) {
			super(() -> {
				task.run(monitor);
				return null;
			});
			this.monitor = monitor;
			this.task = task;
		}

		@Override
		public void done() {
			timerMgmt.cancelTimer(timeoutId);
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			logger.error("task {} cancelled", this);
			timedoutCounter.increment();
			logExecutorState();
			boolean ret = super.cancel(mayInterruptIfRunning);
			monitor.cancelled();
			return ret;
		}

		@Override
		public String toString() {
			return task.toString();
		}

		@Override
		public int priority() {
			return task.priority();
		}
	}

	public void execute(BMTask command, long timeout) {
		InternalBMTaskMonitor monitor = new InternalBMTaskMonitor();

		BMFutureTask task = new BMFutureTask(command, monitor);
		handleTimeout(task, command, monitor, timeout);
		executor.execute(task);
		logExecutorState();
	}

	public void executeDirect(BMTask command) {
		BMDirectTask task = new BMDirectTask(command);
		executor.execute(task);
		logExecutorState();
	}

	private void handleTimeout(BMFutureTask task, BMTask command, InternalBMTaskMonitor monitor, long timeout) {

		task.timeoutId = timerMgmt.setTimer(timeout, new Handler<Long>() {
			@Override
			public void handle(Long event) {
				if (!task.isDone() && !task.isCancelled()) {
					long currentTime = System.currentTimeMillis();
					if (currentTime - monitor.aliveTime > timeout) {
						task.cancel(true);
						command.cancelled();
					} else {
						handleTimeout(task, command, monitor, timeout);
					}
				}
			}
		});
	}

}
