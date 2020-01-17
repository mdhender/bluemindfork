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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.lib.vertx.VertxPlatform;

public class TaskManagerTests {

	private TasksManager taskManager;

	@Before
	public void before() {
		taskManager = new TasksManager(VertxPlatform.getVertx());
	}

	@Test
	public void testRegisterAndReadLog() throws Exception {

		final CountDownLatch cdl = new CountDownLatch(1);
		IServerTask serverTask = new IServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) {
				monitor.begin(5, "begin");
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
				monitor.progress(1, "processing");
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
				monitor.progress(2, "processing...");
			}
		};
		TaskRef taskRef = taskManager.run(serverTask);

		TaskManager task = taskManager.getTaskManager(taskRef.id);
		assertNotNull(task);

		ReadStream<Buffer> reader = task.log();
		final List<JsonObject> result = new ArrayList<>();

		reader.handler(new Handler<Buffer>() {

			@Override
			public void handle(Buffer event) {
				result.add(new JsonObject(event.toString()));
			}
		});

		reader.endHandler(new Handler<Void>() {

			@Override
			public void handle(Void event) {
				cdl.countDown();
			}
		});

		try {
			cdl.await();
		} catch (InterruptedException e) {

		}

		assertEquals(4, result.size());

		assertEquals("begin", result.get(0).getString("message"));

		assertTrue(result.get(3).getBoolean("end"));

		assertNotNull(task.status());
		assertEquals(TaskStatus.State.Success, task.status().state);
	}

	@Test
	public void testFailingTask() throws Exception {

		IServerTask serverTask = new IServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) throws Exception {
				monitor.begin(5, "begin");
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
				monitor.progress(1, "gogo fail");

				throw new Exception("failed");
			}
		};

		TaskRef tref = taskManager.run(serverTask);

		TaskManager task = taskManager.getTaskManager(tref.id);
		assertNotNull(task);

		// be sure task is finshed
		Thread.sleep(200);

		TaskStatus status = task.status();
		assertEquals(TaskStatus.State.InError, status.state);
		assertEquals("failed", status.lastLogEntry);
	}

	@Test
	public void testSubMonitorInception() throws Exception {
		IServerTask serverTask = new IServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) throws Exception {
				monitor.begin(1, "begin");
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
				}

				IServerTaskMonitor subMonitor = monitor.subWork("test", 1);
				subMonitor.begin(10, null);
				for (int i = 0; i < 10; i++) {
					IServerTaskMonitor subsubMonitor = subMonitor.subWork("inception", 1);
					subsubMonitor.begin(10, null);
					for (int j = 0; j < 10; j++) {
						subsubMonitor.progress(1, "sub [" + i + "][" + j + "]");
					}
				}

			}
		};

		TaskRef tref = taskManager.run(serverTask);

		TaskManager task = taskManager.getTaskManager(tref.id);
		assertNotNull(task);

		// be sure task is finshed
		Thread.sleep(1000);

		TaskStatus status = task.status();
		assertEquals(TaskStatus.State.Success, status.state);

		ReadStream<Buffer> log = task.log();
		final List<JsonObject> logs = new LinkedList<>();
		log.handler(new Handler<Buffer>() {

			@Override
			public void handle(Buffer arg0) {
				JsonObject o = new JsonObject(arg0.toString());
				logs.add(o);
				System.out.println(arg0.toString());
			}

		});

		assertEquals((10 * 10) + 2, logs.size());
	}

	@Test
	public void testSubMonitor() throws Exception {

		IServerTask serverTask = new IServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) throws Exception {
				monitor.begin(1, "begin");
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
				}

				IServerTaskMonitor subMonitor = monitor.subWork("test", 1);
				subMonitor.begin(10, null);
				for (int i = 0; i < 10; i++) {
					Thread.sleep(10);
					subMonitor.progress(1, "step " + i);
				}

			}
		};

		TaskRef tref = taskManager.run(serverTask);

		TaskManager task = taskManager.getTaskManager(tref.id);
		assertNotNull(task);

		// be sure task is finshed
		Thread.sleep(1000);

		TaskStatus status = task.status();
		assertEquals(TaskStatus.State.Success, status.state);

		ReadStream<Buffer> log = task.log();
		final List<JsonObject> logs = new LinkedList<>();
		log.handler(new Handler<Buffer>() {

			@Override
			public void handle(Buffer arg0) {
				JsonObject o = new JsonObject(arg0.toString());
				logs.add(o);
				System.out.println(arg0.toString());
			}

		});

		assertEquals(10 + 2, logs.size());
		// last message
		assertTrue(logs.get(11).getBoolean("end"));
	}
}
