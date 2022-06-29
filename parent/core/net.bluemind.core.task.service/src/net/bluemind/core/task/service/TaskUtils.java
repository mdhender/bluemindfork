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
package net.bluemind.core.task.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.parsetools.JsonParser;
import io.vertx.core.streams.ReadStream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.utils.JsonUtils;

public class TaskUtils {

	private static final Logger logger = LoggerFactory.getLogger(TaskUtils.class);

	private TaskUtils() {

	}

	public static void forwardProgress(ITask task, IServerTaskMonitor monitor) {
		monitor.begin(1, "");
		TaskStatus status = wait(task, monitor::log);
		monitor.end(true, null, status.result);
		logger.info("[{}] task finished {}", task, status);
	}

	public static TaskStatus waitForInterruptible(IServiceProvider provider, TaskRef ref) throws InterruptedException {
		ITask taskApi = provider.instance(ITask.class, ref.id + "");
		TaskStatus ts = null;
		long count = 1;
		do {
			Thread.sleep(Math.min(1000, 10 * count++));
			ts = taskApi.status();
		} while (!ts.state.ended);
		return ts;
	}

	public static String logStreamWait(IServiceProvider provider, TaskRef ref) {
		ITask taskApi = provider.instance(ITask.class, ref.id + "");
		return GenericStream.streamToString(taskApi.log());
	}

	public static TaskStatus wait(IServiceProvider provider, TaskRef ref, Consumer<String> log) {
		ITask taskApi = provider.instance(ITask.class, ref.id + "");
		return wait(taskApi, log);
	}

	public static TaskStatus wait(ITask taskApi, Consumer<String> log) {
		ReadStream<Buffer> read = VertxStream.read(taskApi.log());
		CompletableFuture<TaskStatus> status = new CompletableFuture<>();
		read.handler(JsonParser.newParser().objectValueMode().handler(event -> {
			JsonObject data = event.objectValue();
			String lastLog = data.getString("message");
			log.accept(lastLog);
			if (data.getBoolean("end").booleanValue()) {
				status.complete(JsonUtils.read(data.getString("status"), TaskStatus.class));
			}
		}));

		try {
			return status.get();
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	public static ExtendedTaskStatus wait(IServiceProvider provider, TaskRef ref) {
		List<String> logs = new ArrayList<>();
		TaskStatus ts = wait(provider, ref, logs::add);
		return new ExtendedTaskStatus(ts, logs);
	}

	public static class ExtendedTaskStatus extends TaskStatus {
		public final List<String> logs;

		public ExtendedTaskStatus(TaskStatus status, List<String> logs) {
			this.logs = logs;
			super.lastLogEntry = status.lastLogEntry;
			super.progress = status.progress;
			super.result = status.result;
			super.state = status.state;
			super.steps = status.steps;
		}
	}
}
