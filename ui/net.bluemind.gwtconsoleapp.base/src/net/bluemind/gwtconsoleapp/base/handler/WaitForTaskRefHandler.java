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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.gwtconsoleapp.base.handler;

import com.google.gwt.user.client.Timer;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.api.TaskStatus.State;
import net.bluemind.core.task.api.gwt.endpoint.TaskGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.Ajax;

public abstract class WaitForTaskRefHandler implements AsyncHandler<TaskRef> {

	public WaitForTaskRefHandler() {
	}

	@Override
	public void success(TaskRef tr) {
		TaskGwtEndpoint taskService = new TaskGwtEndpoint(Ajax.TOKEN.getSessionId(), String.valueOf(tr.id));
		waitForTaskRef(taskService);
	}

	@Override
	public void failure(Throwable e) {
		TaskStatus status = new TaskStatus();
		status.state = State.status(false, true);
		status.lastLogEntry = e.getMessage();
		onFailure(status);
	}

	private void waitForTaskRef(final TaskGwtEndpoint taskService) {

		taskService.status(new DefaultAsyncHandler<TaskStatus>() {

			@Override
			public void success(TaskStatus status) {
				if (status.state.ended) {
					if (status.state.succeed) {
						onSuccess(status);
					} else {
						onFailure(status);
					}
				} else {
					Timer t = new Timer() {
						@Override
						public void run() {
							waitForTaskRef(taskService);
						}
					};
					t.schedule(500);
				}
			}

		});
	}

	public abstract void onSuccess(TaskStatus status);

	public abstract void onFailure(TaskStatus status);

}
