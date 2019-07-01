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
package net.bluemind.ui.adminconsole.progress.ui;

import java.util.Arrays;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.api.gwt.endpoint.TaskGwtEndpoint;

public class ProgressHandler implements RepeatingCommand, AsyncHandler<TaskStatus> {

	private ProgressScreen ps;
	private TaskGwtEndpoint taskApi;

	public ProgressHandler(ProgressScreen progressScreen, TaskGwtEndpoint taskApi) {
		this.ps = progressScreen;
		this.taskApi = taskApi;
	}

	@Override
	public void success(final TaskStatus result) {
		GWT.log("status received: " + result);
		ps.addOutput(Arrays.asList(result.lastLogEntry));
		if (result.state.ended) {
			GWT.log("ended, getting last logs.");
			AsyncHandler<List<String>> handler = new AsyncHandler<List<String>>() {

				@Override
				public void success(List<String> value) {
					ps.addOutput(value);
					if (result.state.succeed) {
						ps.setProgress(100);
					}
					ps.setTaskFinished(true, result.result);
				}

				@Override
				public void failure(Throwable e) {
					ps.setTaskFinished(false, result.result);
				}
			};
			taskApi.getCurrentLogs(handler);
		} else {
			scheduleRefresh();
		}
	}

	public void failure(Throwable t) {
		GWT.log("Failure", t);
		ps.setTaskFinished(true, "{ \"cause\": \"" + t.getMessage() + "\" }");
	}

	private void scheduleRefresh() {
		Scheduler.get().scheduleFixedDelay(this, 1000);
	}

	@Override
	public boolean execute() {
		taskApi.status(this);
		return false;
	}

}
