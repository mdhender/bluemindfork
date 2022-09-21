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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.lib.vertx.VertxPlatform;

public class TaskManagerTests {

	private TasksManager taskManager;

	@Before
	public void before() throws IOException {
		taskManager = new TasksManager(VertxPlatform.getVertx());

		Path directory = Paths.get("/var/cache/bm-core/tasks-queues");
		directory.toFile().mkdirs();
		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.deleteIfExists(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.deleteIfExists(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	@After
	public void after() {
		System.err.println("After test.");
	}

	@Test(timeout = 30000)
	public void repeatFailures() throws Exception {
		for (int i = 0; i < 1024; i++) {
			System.err.println("start task " + i);
			try {
				testFailingTaskLog();
				System.err.println("task " + i + " ends.");
			} catch (Exception e) {
				e.printStackTrace();
				throw e;
			}
		}

	}

	@Test
	public void testLogStreamOnFinishedTask() throws Exception {
		final CountDownLatch cdl = new CountDownLatch(1);
		IServerTask serverTask = monitor -> {
			monitor.begin(5, "begin");
			throw new NullPointerException("null me dude");
		};
		TaskRef taskRef = taskManager.run(serverTask);

		TaskManager task = taskManager.getTaskManager(taskRef.id);
		assertNotNull(task);

		Thread.sleep(500);

		ReadStream<Buffer> reader = task.log();
		final List<JsonObject> result = new ArrayList<>();

		reader.handler((Buffer event) -> {
			result.add(new JsonObject(event.toString()));
		});

		reader.endHandler(event -> cdl.countDown());

		try {
			cdl.await(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {

		}

		assertEquals(2, result.size());

		assertEquals("begin", result.get(0).getString("message"));

		assertTrue(result.get(1).getBoolean("end"));

		assertNotNull(task.status());
		assertEquals(TaskStatus.State.InError, task.status().state);
		task.cleanUp();
	}

	@Test
	public void testManyActiveTasks() {
		int cnt = 2048;
		final CountDownLatch cdl = new CountDownLatch(cnt);
		CompletableFuture<Void> endTask = new CompletableFuture<>();
		AtomicLong msgs = new AtomicLong();

		for (int i = 0; i < cnt; i++) {
			String taskId = "tsk_" + i;
			IServerTask serverTask = new BlockingServerTask() {

				private void logLoop(Vertx vx, IServerTaskMonitor monitor) {
					vx.setTimer(10, tid -> {
						monitor.log(taskId + " is alive: Lorem ipsum dolor sit amet, "
								+ "consectetur adipiscing elit. Donec ut porttitor neque. "
								+ "In mattis sagittis lobortis.");
						if (!endTask.isDone()) {
							logLoop(vx, monitor);
						} else {
							monitor.progress(5, null);
							monitor.end(true, "gg " + tid, "yeah");
						}
					});
				}

				@Override
				public void run(IServerTaskMonitor monitor) {
					monitor.begin(5, "begin");
					logLoop(VertxPlatform.getVertx(), monitor);
				}
			};
			TaskRef taskRef = taskManager.run(serverTask);

			TaskManager task = taskManager.getTaskManager(taskRef.id);
			assertNotNull(task);

			ReadStream<Buffer> reader = task.log();
			reader.handler(b -> msgs.incrementAndGet());
			reader.endHandler(v -> cdl.countDown());

		}
		while (msgs.get() < 500_000) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			System.err.println("Tasks logs have fetched " + msgs.get() + " message(s)");
		}
		endTask.complete(null);
		try {
			cdl.await(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {

		}
	}

	@Test(timeout = 10000)
	public void testFailingTaskLog() throws Exception {

		final CountDownLatch cdl = new CountDownLatch(1);
		IServerTask serverTask = monitor -> {
			monitor.begin(5, "begin 5 testFailingTaskLog");
			throw new NullPointerException("testFailingTaskLog NPE");
		};
		TaskRef taskRef = taskManager.run(serverTask);

		TaskManager task = taskManager.getTaskManager(taskRef.id);
		assertNotNull(task);

		ReadStream<Buffer> reader = task.log();
		final List<JsonObject> result = new ArrayList<>();

		reader.handler(event -> result.add(new JsonObject(event.toString())));
		reader.endHandler(event -> cdl.countDown());
		reader.exceptionHandler(event -> cdl.countDown());

		try {
			cdl.await(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}

		assertEquals(2, result.size());

		assertEquals("begin 5 testFailingTaskLog", result.get(0).getString("message"));

		assertTrue(result.get(1).getBoolean("end"));

		assertNotNull(task.status());
		assertEquals(TaskStatus.State.InError, task.status().state);
		task.cleanUp();
	}

	@Test
	public void testRegisterAndReadLog() throws Exception {

		final CountDownLatch cdl = new CountDownLatch(1);
		IServerTask serverTask = monitor -> {
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
			return CompletableFuture.completedFuture(null);
		};
		TaskRef taskRef = taskManager.run(serverTask);

		TaskManager task = taskManager.getTaskManager(taskRef.id);
		assertNotNull(task);

		ReadStream<Buffer> reader = task.log();
		final List<JsonObject> result = new ArrayList<>();

		reader.handler((Buffer event) -> {
			System.err.println("got buf " + event);
			result.add(new JsonObject(event.toString()));
		});

		reader.endHandler(event -> cdl.countDown());

		try {
			cdl.await(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {

		}

		assertEquals(4, result.size());

		assertEquals("begin", result.get(0).getString("message"));

		assertTrue(result.get(3).getBoolean("end"));

		assertNotNull(task.status());
		assertEquals(TaskStatus.State.Success, task.status().state);
		task.cleanUp();
	}

	@Test
	public void testFailingTask() throws Exception {

		IServerTask serverTask = monitor -> {
			monitor.begin(5, "begin");
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
			monitor.progress(1, "gogo fail");
			return CompletableFuture.failedFuture(new Exception("failed"));
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
		IServerTask serverTask = monitor -> {
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
			return CompletableFuture.completedFuture(null);
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
		log.handler(arg0 -> {
			JsonObject o = new JsonObject(arg0.toString());
			logs.add(o);
			System.out.println(arg0.toString());
		});

		assertEquals((10 * 10) + 2, logs.size());
	}

	@Test
	public void testSubMonitor() throws Exception {

		IServerTask serverTask = monitor -> {
			monitor.begin(1, "begin");
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}

			IServerTaskMonitor subMonitor = monitor.subWork("test", 1);
			subMonitor.begin(10, null);
			for (int i = 0; i < 10; i++) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				subMonitor.progress(1, "step " + i);
			}
			return CompletableFuture.completedFuture(null);
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
		log.handler(arg0 -> {
			JsonObject o = new JsonObject(arg0.toString());
			logs.add(o);
			System.out.println(arg0.toString());
		});

		assertEquals(10 + 2, logs.size());
		// last message
		assertTrue(logs.get(11).getBoolean("end"));
	}
}
