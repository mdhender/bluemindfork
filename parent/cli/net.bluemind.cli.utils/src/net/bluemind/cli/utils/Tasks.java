/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.cli.utils;

import java.util.List;

import com.google.common.base.Strings;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;

public class Tasks {

	public static TaskStatus follow(CliContext ctx, TaskRef ref) {
		return follow(ctx, true, ref);
	}

	public static TaskStatus follow(CliContext ctx, boolean shouldLog, TaskRef ref) {
		ITask trackApi = ctx.adminApi().instance(ITask.class, ref.id);
		TaskStatus ts = null;
		int logIdx = 0;
		do {
			ts = trackApi.status();
			if (shouldLog) {
				List<String> logs = trackApi.getCurrentLogs();
				for (; logIdx < logs.size(); logIdx++) {
					String log = logs.get(logIdx);
					if (!Strings.isNullOrEmpty(log)) {
						ctx.info(log);
					}
				}
			}
			if (!ts.state.ended) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		} while (!ts.state.ended);
		return ts;

	}

}
