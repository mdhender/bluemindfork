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

import io.vertx.core.eventbus.EventBus;
import net.bluemind.core.task.service.AbstractTaskMonitor;
import net.bluemind.core.task.service.LoggingTaskMonitor;

public class TaskMonitor extends AbstractTaskMonitor {
	private EventBus eventBus;
	private String taskId;
	private boolean ended;

	public TaskMonitor(EventBus eventBus, String taskId) {
		super(0);
		this.eventBus = eventBus;
		this.taskId = taskId;
	}

	@Override
	public void begin(double work, String log) {
		LoggingTaskMonitor.logger.debug("send begin {} {}", work, log);
		eventBus.publish(TasksManager.TASKS_MANAGER_EVENT, MonitorMessage.begin(taskId, work, log));
	}

	@Override
	public void progress(double step, String log) {
		LoggingTaskMonitor.logger.debug("send progress {} {}", step, log);
		eventBus.publish(TasksManager.TASKS_MANAGER_EVENT, MonitorMessage.progress(taskId, step, log));
	}

	@Override
	public void log(String log) {
		LoggingTaskMonitor.logger.debug("send log {}", log);
		eventBus.publish(TasksManager.TASKS_MANAGER_EVENT, MonitorMessage.log(taskId, log));
	}

	@Override
	public void end(boolean success, String log, String result) {
		if (ended) {
			return;
		}
		ended = true;
		LoggingTaskMonitor.logger.info("[{}] send end {} result {}", taskId, log, result);
		eventBus.publish(TasksManager.TASKS_MANAGER_EVENT, MonitorMessage.end(taskId, success, log, result));

	}

	public boolean ended() {
		return ended;
	}
}
