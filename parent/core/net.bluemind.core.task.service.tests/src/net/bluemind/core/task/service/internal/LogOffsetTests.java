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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.lib.vertx.VertxPlatform;

public class LogOffsetTests {

	private TasksManager taskManager;

	@Before
	public void before() {
		taskManager = new TasksManager(VertxPlatform.getVertx());
	}

	@Test
	public void testLogOffsets() throws Exception {

		int loops = 1024;

		IServerTask serverTask = new BlockingServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) {
				monitor.begin(loops, "begin");
				for (int i = 0; i < loops; i++) {
					monitor.log("Coucou loop " + i);
					monitor.progress(1, null);
					try {
						Thread.sleep(5);
					} catch (InterruptedException e) {
					}
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
				monitor.end(true, "yeah", "yeah");
			}
		};
		TaskRef taskRef = taskManager.run(serverTask);

		TaskManager task = taskManager.getTaskManager(taskRef.id);
		assertNotNull(task);

		TaskStatus status = null;
		int offset = 0;
		do {
			status = task.status();
			System.err.println("offset: " + offset);
			List<String> logs = task.getCurrentLogs(offset);
			for (String k : logs) {
				System.err.println("        L: " + k);
			}
			offset += logs.size();
			Thread.sleep(20);
		} while (!status.state.ended);

		task.cleanUp();
		assertNotNull(status);
		assertEquals(TaskStatus.State.Success, status.state);
	}

}
