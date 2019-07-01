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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.internal.TasksManager;

public class TaskTests {

	@Test
	public void testGetStatus() throws ServerFault, InterruptedException {

		IServerTask serverTask = new IServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) {
				monitor.begin(5, "begin");
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
				monitor.progress(1, "processing");
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
				monitor.progress(2, "processing...");

				monitor.end(true, "test", "resultTest");
			}
		};
		TaskRef taskRef = getTasksManager().run(serverTask);
		ITask task = getTask("" + taskRef.id);
		assertNotNull(task);

		Thread.sleep(100);
		TaskStatus status = task.status();
		assertNotNull(status);
		assertEquals(TaskStatus.State.InProgress, status.state);

		Thread.sleep(1000);
		status = task.status();
		assertNotNull(status);
		assertEquals(TaskStatus.State.Success, status.state);
		assertEquals("resultTest", status.result);

	}

	@Test
	public void testThreadPoolCapacity() throws ServerFault, InterruptedException {
		IServerTask serverTask = new IServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) {
				monitor.begin(5, "begin");
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
				}
				monitor.progress(1, "processing");
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
				monitor.progress(2, "processing...");

				monitor.end(true, "test", "resultTest");
			}
		};

		List<TaskRef> refs = new ArrayList<>();

		for (int i = 0; i < TasksManager.MAX_TASK_COUNT; i++) {
			TaskRef taskRef = getTasksManager().run(serverTask);
			ITask task = getTask("" + taskRef.id);
			assertNotNull(task);
			refs.add(taskRef);
			Thread.sleep(100);
			assertEquals(TaskStatus.State.InProgress, task.status().state);
		}

		try {
			TaskRef taskRef = getTasksManager().run(serverTask);
			Thread.sleep(1000);
			ITask task = getTask("" + taskRef.id);
			assertNotNull(task);
			refs.add(taskRef);
			assertEquals(TaskStatus.State.NotStarted, task.status().state);
		} catch (Exception e) {
			System.err.println(e.getClass().getName());
		}

		refs.forEach(ref -> waitForTaskRef(ref));

	}

	private TaskStatus waitForTaskRef(TaskRef taskRef) throws ServerFault {
		ITask task = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITask.class, taskRef.id);
		while (!task.status().state.ended) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}

		return task.status();
	}

	protected ITasksManager getTasksManager() throws ServerFault {
		return ServerSideServiceProvider.getProvider(SecurityContext.ANONYMOUS).instance(ITasksManager.class);
	}

	protected ITask getTask(String taskId) throws ServerFault {
		return ServerSideServiceProvider.getProvider(SecurityContext.ANONYMOUS).instance(ITask.class, taskId);
	}
}
