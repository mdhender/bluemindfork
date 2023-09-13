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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.lib.vertx.VertxPlatform;

public class TaskManagerTests {

	private static TasksManager taskManager;

	@BeforeClass
	public static void beforeClass() {
		taskManager = new TasksManager(VertxPlatform.getVertx());
	}

	@Before
	public void before() {
		Path directory = Paths.get(System.getProperty("chronicle.queues.root", "/var/cache/bm-core/tasks-queues"));
		directory.toFile().mkdirs();
	}

	@After
	public void after() {
		System.err.println("After test.");
		TasksManager.reset();
	}

	@Test(timeout = 60000)
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
	public void testQuickTasks() throws InterruptedException {
		int cnt = 20_000;

		CountDownLatch cdl = new CountDownLatch(cnt - 513); // cache size is 512
		CompletableFuture<Void> comp = CompletableFuture.completedFuture(null);
		RuntimeException rt = new RuntimeException();
		rt.setStackTrace(new StackTraceElement[0]);
		VertxPlatform.eventBus().consumer("tasks.manager.cleanups.expire", msg -> {
			cdl.countDown();
		});

		for (int i = 0; i < cnt; i++) {
			IServerTask serverTask = (IServerTaskMonitor monitor) -> {
				monitor.end(true, null, null);
				return comp;
			};

			TaskRef taskRef = taskManager.run(serverTask);
			assertNotNull(taskRef);

		}
		boolean ok = cdl.await(2, TimeUnit.MINUTES);
		if (!ok) {
			System.err.println("Missing " + cdl.getCount() + " " + new Date());
		}
		assertTrue(ok);
	}

	@Test
	public void testManyActiveTasks() {
		int cnt = 200;
		int expectedMsg = 200_000;
		final CountDownLatch cdl = new CountDownLatch(cnt);
		CompletableFuture<Void> endTask = new CompletableFuture<>();
		AtomicLong msgs = new AtomicLong();

		for (int i = 0; i < cnt; i++) {
			String taskId = "tsk_" + i;
			IServerTask serverTask = new IServerTask() {

				private CompletableFuture<Void> taskProm = new CompletableFuture<>();

				private void logLoop(Vertx vx, IServerTaskMonitor monitor) {
					vx.setTimer(10, tid -> {
						vx.executeBlocking(() -> {
							monitor.log(taskId + " is alive: Lorem ipsum dolor sit amet, "
									+ "consectetur adipiscing elit. Donec ut porttitor neque. "
									+ "In mattis sagittis lobortis.");
							return null;
						}, true).andThen(ar -> {
							if (ar.failed()) {
								taskProm.completeExceptionally(ar.cause());
							} else if (!endTask.isDone()) {
								logLoop(vx, monitor);
							} else {
								monitor.progress(1, null);
								monitor.end(true, "gg " + tid, "yeah " + taskId);
								taskProm.complete(null);
							}
						});
					});
				}

				@Override
				public CompletableFuture<Void> execute(IServerTaskMonitor monitor) {
					monitor.begin(2.0 * (expectedMsg / cnt), "begin");
					logLoop(VertxPlatform.getVertx(), monitor);
					return taskProm;
				}
			};
			TaskRef taskRef = taskManager.run(serverTask);

			TaskManager task = taskManager.getTaskManager(taskRef.id);
			assertNotNull(task);
			long time = System.currentTimeMillis();
			ReadStream<Buffer> reader = task.log();
			reader.handler(b -> msgs.incrementAndGet());
			reader.endHandler(v -> {
				System.err.println(taskId + " ends after " + (System.currentTimeMillis() - time) + "ms ("
						+ Thread.currentThread().getName() + ")");
				cdl.countDown();
			});

		}
		while (msgs.get() < expectedMsg) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			System.err.println("Tasks logs have fetched " + msgs.get() + " message(s)");
		}
		endTask.complete(null);
		try {
			assertTrue(cdl.await(30, TimeUnit.SECONDS));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
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
			assertTrue("completion of " + taskRef.id + " took more than 30sec", cdl.await(30, TimeUnit.SECONDS));
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
