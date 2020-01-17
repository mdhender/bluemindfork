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

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.core.task.service.LoggingTaskMonitor;
import net.bluemind.lib.vertx.WorkerExecutorService;

public class TasksManager implements ITasksManager {

	private static Logger logger = LoggerFactory.getLogger(TasksManager.class);
	private ConcurrentHashMap<String, TaskManager> tasks = new ConcurrentHashMap<>();
	private Vertx vertx;
	public static final int MAX_TASK_COUNT = 10;
	private ExecutorService executer = new WorkerExecutorService("bm-tasks", 15, 1, TimeUnit.DAYS);

	public TasksManager(Vertx vertx) {
		this.vertx = vertx;
	}

	@Override
	public TaskRef run(final String taskId, final IServerTask serverTask) throws ServerFault {
		MessageConsumer<JsonObject> cons = vertx.eventBus().consumer(addr(taskId));
		final TaskManager task = new TaskManager(taskId, cons);
		final TaskMonitor monitor = new TaskMonitor(vertx.eventBus(), addr(taskId));
		final LoggingTaskMonitor loggingMonitor = new LoggingTaskMonitor(null, monitor, 0);
		TaskManager oldTask = tasks.putIfAbsent(taskId, task);
		if (oldTask != null) {
			if (oldTask.status().state.ended) {
				cleanupTask(oldTask);
				tasks.put(taskId, task);
			} else {
				throw new ServerFault("task " + taskId + " already running");
			}
		}
		try {
			executeTask(taskId, serverTask, loggingMonitor, task);
		} catch (RejectedExecutionException e) {
			cleanupTask(task);
			throw new ServerFault("The task has been rejected by the thread pool", ErrorCode.FAILURE);
		}
		return TaskRef.create(taskId);
	}

	private void executeTask(final String taskId, final IServerTask serverTask, LoggingTaskMonitor loggingMonitor,
			TaskManager task) {
		executer.execute(() -> {

			try {
				serverTask.run(loggingMonitor);
				loggingMonitor.end(true, "OK", null);
			} catch (Exception e) {
				logger.error("error in task " + taskId, e);
				loggingMonitor.end(false, e.getMessage(), null);
			} finally {
				vertx.setTimer(1000 * 60 * 10l, event -> cleanupTask(task));
			}
		});
	}

	@Override
	public TaskRef run(final IServerTask serverTask) throws ServerFault {
		final String taskId = UUID.randomUUID().toString();
		return run(taskId, serverTask);
	}

	private void cleanupTask(TaskManager task) {
		TaskManager tsk = tasks.remove(task.getId());
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

	private String addr(String taskId) {
		return "tasks-" + taskId;
	}
}
