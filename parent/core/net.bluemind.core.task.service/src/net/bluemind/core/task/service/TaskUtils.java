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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.api.TaskStatus.State;

public class TaskUtils {

	private static Logger logger = LoggerFactory.getLogger(TaskUtils.class);

	public static void forwardProgress(ITask task, IServerTaskMonitor monitor) {
		TaskStatus status = task.status();
		double steps = -1;
		List<String> lastLogs = new ArrayList<>();
		double current = 0;
		logger.info("begin monitor task steps {} progress {} ", status.steps, status.progress);

		do {
			status = task.status();
			List<String> logs = task.getCurrentLogs();
			for (int i = lastLogs.size(); i < logs.size(); i++) {
				monitor.log(logs.get(i));
			}
			lastLogs = logs;

			if (steps == -1 && status.steps > 0) {
				logger.info("notify begin monitor task steps {} progress {} ", status.steps, status.progress);
				steps = status.steps;
				monitor.begin(status.steps, "");
			}

			if (status.progress - current > 0) {
				logger.info("progress task {}/{} mark progres {}", status.progress, status.steps,
						status.progress - current);
				monitor.progress(status.progress - current, "");
			}
			current = status.progress;
			if (!status.state.ended) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					logger.warn("error during sleep", e);
					Thread.currentThread().interrupt();
				}
			}
		} while (!status.state.ended);

		monitor.end(true, null, status.result);
		logger.info("task finished");
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

	public static TaskStatus wait(IServiceProvider provider, TaskRef ref) {
		ITask taskApi = provider.instance(ITask.class, ref.id + "");
		TaskStatus ts = null;
		long count = 1;
		do {
			try {
				Thread.sleep(Math.min(1000, 10 * count++));
			} catch (InterruptedException e) {
				logger.warn("Task has been interrupted");
				Thread.currentThread().interrupt();
				if (ts != null) {
					ts.state = State.InError;
					break;
				}
			}
			ts = taskApi.status();
		} while (!ts.state.ended);
		return ts;
	}
}
