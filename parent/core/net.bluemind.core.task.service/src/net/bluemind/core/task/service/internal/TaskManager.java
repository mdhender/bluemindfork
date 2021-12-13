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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.internal.MonitorMessage.MessageType;
import net.bluemind.core.utils.JsonUtils;

public class TaskManager implements Handler<Message<JsonObject>> {

	private static final Logger logger = LoggerFactory.getLogger(TaskManager.class);
	private Collection<JsonObject> logs = new ConcurrentLinkedDeque<>();

	private Object lock = new Object();

	private List<LogStream> readers = new ArrayList<>();

	private TaskStatus status = new TaskStatus();
	private double steps;
	private double currentStep;
	private String taskId;

	public TaskManager(String taskId) {
		this.taskId = taskId;
	}

	public void cleanUp() {
	}

	public ReadStream<Buffer> log() {
		synchronized (lock) {
			LogStream reader = new LogStream();

			registerReader(reader);
			return reader;
		}

	}

	public List<String> getCurrentLogs() {

		synchronized (lock) {
			List<String> ret = new ArrayList<>(logs.size());
			for (JsonObject o : logs) {
				ret.add(o.getString("message"));
			}
			return ret;
		}
	}

	private void registerReader(LogStream reader) {
		this.readers.add(reader);
		for (JsonObject l : logs) {
			reader.pushData(l);
			if (Boolean.TRUE.equals(l.getBoolean("end"))) {
				reader.end();
			}
		}

	}

	@Override
	public void handle(final Message<JsonObject> event) {
		logger.debug("log message {} for task {}", event.body().getString("message"), taskId);

		updateStatus(event.body());
		MessageType type = MessageType.valueOf(event.body().getString("type"));
		synchronized (lock) {

			if (type == MessageType.begin) {
				steps = event.body().getDouble("work");
				currentStep = 0;
			} else if (type == MessageType.progress) {
				currentStep += event.body().getDouble("step");
			}

			boolean end = type == MessageType.end;
			if (end) {
				currentStep = steps;
			}
			pushLog(currentStep, steps, event.body().getString("message"), end);

		}
	}

	private void pushLog(double currentStep2, double steps2, String message, boolean end) {
		JsonObject log = new JsonObject();
		log.put("done", currentStep2);
		log.put("total", steps2);
		log.put("message", message);
		log.put("end", end);
		logs.add(log);
		for (LogStream reader : readers) {
			if (end) {
				log.put("status", JsonUtils.asString(this.status));
			}
			reader.pushData(log);
			if (end) {
				reader.end();
			}
		}
	}

	private void updateStatus(JsonObject body) {
		MessageType type = MessageType.valueOf(body.getString("type"));
		boolean success = body.getBoolean("success", false);
		/*
		 * If the current status is ended (success or error), we must not overwrite the
		 * status of the current task
		 */
		boolean ended = status.state.ended || type == MessageType.end;
		TaskStatus newStatus = TaskStatus.create(steps, currentStep, body.getString("message"),
				TaskStatus.State.status(success, ended), body.getString("result"));
		logger.info("update task {} status: {} {} on {}", taskId, newStatus.state, newStatus.progress, newStatus.steps);
		this.status = newStatus;
	}

	public TaskStatus status() {
		TaskStatus s = status;
		logger.info("retrieve task status : {} {} on {}", status.state, status.progress, status.steps);
		return s;
	}

	public String getId() {
		return taskId;
	}

	@Override
	public String toString() {
		return "TaskManager{id=" + taskId + ", status=" + status + "}";
	}
}
