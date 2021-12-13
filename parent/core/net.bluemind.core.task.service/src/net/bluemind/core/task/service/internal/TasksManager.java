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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.MoreExecutors;

import io.netty.util.concurrent.FastThreadLocal;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.core.task.service.LoggingTaskMonitor;
import net.bluemind.core.utils.CancellableRunnable;
import net.bluemind.core.utils.FutureThreadInfo;
import net.bluemind.lib.vertx.WorkerExecutorService;

public class TasksManager implements ITasksManager {

	private static Logger logger = LoggerFactory.getLogger(TasksManager.class);
	private static final Object ROOT_TASK_MARKER = new Object();
	public static final String TASKS_MANAGER_EVENT = "tasks-manager";

	private final ConcurrentHashMap<String, TaskManager> tasks = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, FutureThreadInfo> futures = new ConcurrentHashMap<>();
	private final Vertx vertx;
	private final FastThreadLocal<Object> threadLocal = new FastThreadLocal<>();
	private final ExecutorService executer = new WorkerExecutorService("bm-tasks", 15, 1, TimeUnit.DAYS,
			() -> threadLocal.set(ROOT_TASK_MARKER));
	private final ExecutorService directExecutor = MoreExecutors.newDirectExecutorService();

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
					logger.error("[{}] task manager not found", taskId);
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
		final TaskManager task = new TaskManager(taskId);
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
			ExecutorService selectedExecutor = executesInRunningRootTask() ? this.directExecutor : this.executer;
			executeTask(taskId, serverTask, loggingMonitor, task, selectedExecutor);
		} catch (RejectedExecutionException e) {
			cleanupTask(task);
			throw new ServerFault("The task has been rejected by the thread pool", ErrorCode.FAILURE);
		}
		return TaskRef.create(taskId);
	}

	private boolean executesInRunningRootTask() {
		return ROOT_TASK_MARKER == threadLocal.get();
	}

	private void executeTask(final String taskId, final IServerTask serverTask, LoggingTaskMonitor loggingMonitor,
			TaskManager task, ExecutorService es) {

		CancellableRunnable runnable = new CancellableRunnable() {

			@Override
			public void run() {
				try {
					serverTask.run(loggingMonitor);
					loggingMonitor.end(true, "OK", null);
				} catch (Exception e) {
					logger.error("error in task {}", taskId, e);
					loggingMonitor.end(false, e.getMessage(), null);
				} finally {
					vertx.setTimer(1000 * 60 * 10l, event -> cleanupTask(task));
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

	private void cleanupTask(TaskManager task) {
		TaskManager tsk = tasks.remove(task.getId());
		futures.remove(task.getId());
		if (tsk != null) {
			tsk.cleanUp();
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

}
