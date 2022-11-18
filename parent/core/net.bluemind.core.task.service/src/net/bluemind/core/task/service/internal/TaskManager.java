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

import java.util.List;

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

public abstract class TaskManager implements Handler<Message<JsonObject>> {

	private static final Logger logger = LoggerFactory.getLogger(TaskManager.class);

	protected final String taskId;

	protected TaskStatus status = new TaskStatus();
	private double steps;
	private double currentStep;

	protected TaskManager(String taskId) {
		this.taskId = taskId;

	}

	public void cleanUp() {
	}

	public abstract ReadStream<Buffer> log();

	public abstract List<String> getCurrentLogs(int offset);

	@Override
	public final void handle(final Message<JsonObject> event) {
		JsonObject evBody = event.body();
		if (logger.isDebugEnabled()) {
			logger.debug("log message {} for task {}", evBody.getString("message"), taskId);
		}
		updateStatus(evBody);
		MessageType type = MessageType.valueOf(evBody.getString("type"));

		if (type == MessageType.begin) {
			steps = evBody.getDouble("work");
			currentStep = 0;
		} else if (type == MessageType.progress) {
			currentStep += evBody.getDouble("step");
		}

		boolean end = type == MessageType.end;
		if (end) {
			currentStep = steps;
		}

		JsonObject log = prepareLog(evBody, end);
		pushLog(log, end);

	}

	private JsonObject prepareLog(JsonObject evBody, boolean end) {
		JsonObject log = new JsonObject();
		log.put("done", currentStep).put("total", steps);
		log.put("message", evBody.getString("message"));
		log.put("end", end);
		if (end) {
			log.put("status", JsonUtils.asString(this.status));
		}
		return log;
	}

	protected abstract void pushLog(JsonObject log, boolean end);

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
		logger.debug("update task {} status: {} {} on {}", taskId, newStatus.state, newStatus.progress,
				newStatus.steps);
		this.status = newStatus;
	}

	public final TaskStatus status() {
		return status;
	}

	public final String getId() {
		return taskId;
	}

}
