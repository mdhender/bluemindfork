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
package net.bluemind.core.task.service.internal;

import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.MoreExecutors;
import com.typesafe.config.Config;

import io.netty.util.concurrent.FastThreadLocal;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.config.CoreConfig;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.core.task.service.LoggingTaskMonitor;
import net.bluemind.core.task.service.internal.cq.CQTaskManager;
import net.bluemind.core.utils.CancellableRunnable;
import net.bluemind.core.utils.FutureThreadInfo;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.lib.vertx.WorkerExecutorService;

public class TasksManager implements ITasksManager {

	private static Logger logger = LoggerFactory.getLogger(TasksManager.class);
	private static final Object ROOT_TASK_MARKER = new Object();
	public static final String TASKS_MANAGER_EVENT = "tasks-manager";

	private static final Cache<String, TaskManager> completedTasks;
	private static final ConcurrentHashMap<String, TaskManager> tasks = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<String, FutureThreadInfo> futures = new ConcurrentHashMap<>();
	private static final FastThreadLocal<Object> threadLocal = new FastThreadLocal<>();
	private static final ExecutorService executor;
	private static final ExecutorService directExecutor = MoreExecutors.newDirectExecutorService();

	private final Vertx vertx;

	static {
		Config coreConfig = CoreConfig.get();
		executor = new WorkerExecutorService("bm-tasks", coreConfig.getInt(CoreConfig.Pool.TASKS_SIZE), 1,
				TimeUnit.DAYS, () -> threadLocal.set(ROOT_TASK_MARKER));
		completedTasks = Caffeine.newBuilder()//
				.maximumSize(512)//
				.expireAfterWrite(coreConfig.getDuration(CoreConfig.Pool.TASKS_COMPLETED_TIMEOUT))//
				.evictionListener((String key, TaskManager value, RemovalCause cause) -> {
					if (value != null) {
						VertxPlatform.getVertx().setTimer(5000, tid -> {
							VertxPlatform.getVertx().executeBlocking(prom -> {
								VertxPlatform.eventBus().publish("tasks.manager.cleanups.expire", key);
								cleanupTask(value);
								prom.complete();
							});
						});
					}
				})//
				.build();
	}

	public static class EventBusReceiveVerticle extends AbstractVerticle {
		private TasksManager tasksManager;

		public EventBusReceiveVerticle(TasksManager tasksManager) {
			this.tasksManager = tasksManager;
		}

		@Override
		public void start() {
			EventBus eventBus = vertx.eventBus();
			MessageConsumer<JsonObject> eventBusConsumer = eventBus.consumer(TASKS_MANAGER_EVENT);
			eventBusConsumer.handler(msg -> {
				String taskId = msg.body().getString("id");
				TaskManager manager = tasksManager.getTaskManager(taskId);
				if (manager != null) {
					manager.handle(msg);
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("[{}] task manager not found", taskId);
					}
				}
			});
		}

	}

	public TasksManager(Vertx vertx) {
		this.vertx = vertx;
		vertx.deployVerticle(new EventBusReceiveVerticle(this));
	}

	@Override
	public TaskRef run(final IServerTask serverTask) {
		final String taskId = UUID.randomUUID().toString();
		return run(taskId, null, serverTask);
	}

	@Override
	public TaskRef run(Logger logger, final IServerTask serverTask) {
		final String taskId = UUID.randomUUID().toString();
		return run(taskId, logger, serverTask);
	}

	@Override
	public TaskRef run(final String taskId, final IServerTask serverTask) {
		return run(taskId, null, serverTask);
	}

	@Override
	public TaskRef run(final String taskId, Logger logger, final IServerTask serverTask) {
		final TaskManager task = new CQTaskManager(taskId);
		final TaskMonitor monitor = new TaskMonitor(vertx.eventBus(), taskId);
		final LoggingTaskMonitor loggingMonitor = new LoggingTaskMonitor(logger, monitor, 0);
		TaskManager oldTask = tasks.putIfAbsent(taskId, task);
		if (oldTask != null) {
			if (oldTask.status().state.ended) {
				cleanupTask(oldTask);
			} else {
				throw new ServerFault("task " + taskId + " already running");
			}
		}

		try {
			ExecutorService selectedExecutor = inTaskThread() ? directExecutor : executor;
			executeTask(taskId, serverTask, loggingMonitor, task, selectedExecutor);
		} catch (RejectedExecutionException e) {
			cleanupTask(task);
			throw new ServerFault("The task has been rejected by the thread pool", ErrorCode.FAILURE);
		}
		return TaskRef.create(taskId);
	}

	@Override
	public boolean inTaskThread() {
		return ROOT_TASK_MARKER == threadLocal.get();
	}

	private void executeTask(final String taskId, final IServerTask serverTask, LoggingTaskMonitor loggingMonitor,
			TaskManager task, ExecutorService es) {

		CancellableRunnable runnable = new CancellableRunnable() {

			@Override
			public void run() {
				try {
					serverTask.execute(loggingMonitor).thenAccept(r -> {
						loggingMonitor.end(true, "", null);
						completedTasks.put(taskId, task);
					}).exceptionally(e -> {
						String msg = e instanceof CompletionException && e.getCause() != null
								? e.getCause().getMessage()
								: e.getMessage();
						logger.error("error in task {}", taskId, e);
						loggingMonitor.end(false, msg, null);
						completedTasks.put(taskId, task);
						return null;
					});
				} catch (Exception e) {
					logger.error("error in task {}", taskId, e);
					loggingMonitor.end(false, e.getMessage(), null);
					completedTasks.put(taskId, task);
				}
			}

			@Override
			public void cancel() {
				serverTask.cancel();
			}
		};
		tasks.put(taskId, task);
		futures.put(taskId, new FutureThreadInfo(es.submit(runnable), runnable));
	}

	private static void cleanupTask(TaskManager task) {
		boolean removed = tasks.remove(task.getId(), task);
		if (removed) {
			futures.remove(task.getId());
			task.cleanUp();
		}
	}

	@Override
	public ITask getTask(String taskId) {
		TaskManager task = tasks.get(taskId);
		if (task != null) {
			return new TaskService(task);
		}
		return null;
	}

	public TaskManager getTaskManager(String taskId) {
		return tasks.get(taskId);
	}

	@Override
	public void cancel(String taskId) {
		if (futures.containsKey(taskId)) {
			logger.info("Cancelling Task {}", taskId);
			futures.get(taskId).runnable.cancel();
			futures.get(taskId).future.cancel(true);
		}
	}

	@VisibleForTesting
	public static void reset() {
		tasks.forEach((taskId, v) -> {
			futures.get(taskId).runnable.cancel();
			futures.get(taskId).future.cancel(true);
			cleanupTask(v);
		});
		tasks.clear();
		completedTasks.invalidateAll();
	}

}
