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

import net.bluemind.core.api.Stream;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskStatus;

public class TaskService implements ITask {

	private TaskManager manager;

	public TaskService(TaskManager manager) {
		this.manager = manager;
	}

	@Override
	public TaskStatus status() {
		return manager.status();
	}

	@Override
	public Stream log() {
		return VertxStream.stream(manager.log());
	}

	@Override
	public List<String> getCurrentLogs() {
		return manager.getCurrentLogs();
	}

}
