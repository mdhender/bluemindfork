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

import java.util.Collections;
import java.util.List;

import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskStatus;

public class ExpiredTaskService implements ITask {

	private TaskStatus ts;
	private String tid;

	public ExpiredTaskService(String tid, TaskStatus ts) {
		this.tid = tid;
		this.ts = ts;
	}

	@Override
	public TaskStatus status() {
		return ts;
	}

	@Override
	public Stream log() {
		throw new ServerFault("task " + tid + " is expired");
	}

	@Override
	public List<String> getCurrentLogs(Integer offset) {
		return Collections.emptyList();
	}

}
