/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.node.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.io.ByteStreams;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.node.api.ExitList;
import net.bluemind.node.api.FileDescription;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.INodeClientFactory;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.ProcessHandler;
import net.bluemind.node.api.ProcessHandler.BlockingHandler;
import net.bluemind.node.api.ProcessHandler.NoOutBlockingHandler;
import net.bluemind.node.client.OkHttpNodeClientFactory;
import net.bluemind.node.server.BlueMindUnsecureNode;
import net.bluemind.node.server.busmod.SysCommand;
import net.bluemind.node.shared.ActiveExecQuery;
import net.bluemind.node.shared.ExecDescriptor;
import net.bluemind.node.shared.ExecRequest;
import net.bluemind.node.shared.ExecRequest.Options;
import net.bluemind.vertx.testhelper.Deploy;

public class NodeTests {

	private static INodeClientFactory factory;
	private INodeClient nc;

	@BeforeClass
	public static void beforeClass() throws Exception {
		Deploy.verticles(false, Collections.singletonList(BlueMindUnsecureNode::new), 8).get(5, TimeUnit.SECONDS);
		Deploy.verticles(true, SysCommand::new).get(5, TimeUnit.SECONDS);
		factory = new OkHttpNodeClientFactory();
	}

	@AfterClass
	public static void afterClass() {
		VertxPlatform.getVertx().close();
	}

	@Before
	public void before() {
		this.nc = factory.create("127.0.0.1");
	}

	@Test
	public void testPing() {
		try {
			nc.ping();
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testListEtc() {
		List<FileDescription> found = nc.listFiles("/etc");
		assertNotNull(found);
		assertFalse(found.isEmpty());
	}

	@Test
	public void testListNonExistingFile() {
		// folder
		List<FileDescription> found = nc.listFiles("/idontexist");
		assertNotNull(found);
		assertTrue(found.isEmpty());

		// file
		found = nc.listFiles("/tmp/idontexist.txt");
		assertNotNull(found);
		assertTrue(found.isEmpty());
	}

	@Test
	public void testReadStream() throws ServerFault, IOException {
		try (InputStream resolv = nc.openStream("/etc/resolv.conf")) {
			assertNotNull(resolv);
			byte[] content = ByteStreams.toByteArray(resolv);
			assertTrue(content.length > 0);
		}
	}

	@Test
	public void testExist() throws ServerFault, IOException {
		assertTrue(nc.exists("/etc/resolv.conf"));
		assertFalse(nc.exists("/etc/commando.dans.ton.q.sj.conf"));
	}

	@Test
	public void testManyReadStream() throws ServerFault {
		try (ExecutorService pool = Executors.newFixedThreadPool(4)) {

			CompletableFuture<?>[] concurrent = new CompletableFuture<?>[65535];
			for (int i = 0; i < concurrent.length; i++) {
				concurrent[i] = CompletableFuture.supplyAsync(() -> {
					try (InputStream resolv = nc.openStream("/etc/resolv.conf")) {
						assertNotNull(resolv);
						byte[] content = ByteStreams.toByteArray(resolv);
						assertTrue(content.length > 0);
						return content.length;
					} catch (Exception e) {
						throw new ServerFault(e);
					}
				}, pool);
			}
			CompletableFuture.allOf(concurrent).orTimeout(25, TimeUnit.SECONDS).join();
		}
	}

	@Test
	public void testListEtcWithConfExtension() {
		List<FileDescription> found = nc.listFiles("/etc", "conf");
		assertNotNull(found);
		assertFalse(found.isEmpty());
	}

	@Test
	public void testWriteReadDelete() {
		String dir = System.getProperty("java.io.tmpdir");
		String file = dir + "/" + System.currentTimeMillis() + ".junit";
		nc.writeFile(file, new ByteArrayInputStream("yeah".getBytes()));
		String reread = new String(nc.read(file));
		assertEquals("yeah", reread);
		nc.deleteFile(file);
	}

	@Test
	public void testWriteFilePermissions() throws IOException {
		String dir = System.getProperty("java.io.tmpdir");
		Path p = Paths.get(dir + "/" + System.currentTimeMillis() + ".junit");

		Set<PosixFilePermission> notOwnerWritable = PosixFilePermissions.fromString("r--------");
		FileAttribute<?> permissions = PosixFilePermissions.asFileAttribute(notOwnerWritable);
		Files.createFile(p, permissions);
		boolean RUN_AS_ROOT = System.getProperty("user.name").equals("root");

		UserPrincipal userPrincipal = null;
		if (RUN_AS_ROOT) {
			FileSystem fileSystem = p.getFileSystem();
			UserPrincipalLookupService service = fileSystem.getUserPrincipalLookupService();
			userPrincipal = service.lookupPrincipalByName("nobody");
			System.err.println("set owner: " + userPrincipal);
			Files.setOwner(p, userPrincipal);
		}

		nc.writeFile(p.toString(), new ByteArrayInputStream("yeah".getBytes()));

		if (RUN_AS_ROOT) {
			UserPrincipal newUserPrincipal = Files.getOwner(p);
			assertEquals(userPrincipal, newUserPrincipal);
		}

		Set<PosixFilePermission> newRights = Files.getPosixFilePermissions(p);
		assertEquals(notOwnerWritable, newRights);
		nc.deleteFile(p.toString());
	}

	@Test
	public void testAnonymousSleepOne() {
		TaskRef ref = nc.executeCommand(ExecRequest.anonymousWithoutOutput("/bin/sleep 1"));
		track(ref);
	}

	@Test
	public void testNamedSleepOneThenFindByGroup() {
		ExecRequest req = ExecRequest.named("junit", "x" + System.currentTimeMillis(), "/bin/sleep 1");
		TaskRef ref = nc.executeCommand(req);
		assertNotNull(ref);
		List<ExecDescriptor> found = nc.getActiveExecutions(ActiveExecQuery.byGroup("junit"));
		assertEquals(1, found.size());
		track(ref);
	}

	@Test
	public void testOutputOverWebsocket() {
		ExecRequest req = ExecRequest.named("junit", "x" + System.currentTimeMillis(), "/usr/bin/git --help");
		BlockingHandler handler = new ProcessHandler.BlockingHandler();
		nc.asyncExecute(req, handler);
		ExitList el = handler.get(15, TimeUnit.SECONDS);
		assertNotNull(el);
		for (String s : el) {
			System.out.println("O: " + s);
		}
		assertEquals(0, el.getExitCode());
	}

	@Test
	public void testSlowReceiverOverWebsocket() {
		ExecRequest req = ExecRequest.named("junit", "x" + System.currentTimeMillis(), "seq 1 5000");
		CompletableFuture<Integer> comp = new CompletableFuture<>();
		AtomicInteger count = new AtomicInteger();
		nc.asyncExecute(req, new ProcessHandler() {

			@Override
			public void log(String l, boolean cont) {
				count.incrementAndGet();
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
				}
			}

			@Override
			public void completed(int exitCode) {
				comp.complete(exitCode);
			}

			@Override
			public void starting(String taskRef) {
				// ok
			}

		});
		comp.join();
		assertEquals(5000, count.get());
	}

	@Test
	public void testBigOutputOverWebsocket() {
		// seq 1 1000000 on osx returns 2 times 1e+06 in the end...
		int lines = 100_000;
		ExecRequest req = ExecRequest.named("junit", "x" + System.currentTimeMillis(), "seq 1 " + lines);
		CompletableFuture<Integer> comp = new CompletableFuture<>();
		AtomicInteger count = new AtomicInteger();
		nc.asyncExecute(req, new ProcessHandler() {

			@Override
			public void log(String l, boolean cont) {
				count.incrementAndGet();
			}

			@Override
			public void completed(int exitCode) {
				comp.complete(exitCode);
			}

			@Override
			public void starting(String taskRef) {
				// ok
			}

		});
		Integer exitcode = comp.join();
		assertEquals(0, exitcode.intValue());
		System.err.println("count: " + count.get());
		assertEquals(lines, count.get());
	}

	@Test
	public void testVeryLongLinesOverWebsocket() {
		int lines = 100000;
		ExecRequest req = ExecRequest.named("junit", "x" + System.currentTimeMillis(), "seq -s - 1 " + lines);
		CompletableFuture<Integer> comp = new CompletableFuture<>();
		AtomicInteger count = new AtomicInteger();
		AtomicInteger max = new AtomicInteger();
		AtomicInteger splittedLines = new AtomicInteger();
		StringBuilder full = new StringBuilder();
		nc.asyncExecute(req, new ProcessHandler() {

			@Override
			public void log(String l, boolean cont) {
				count.incrementAndGet();
				full.append(l);
				if (!cont) {
					full.append('\n');
				} else {
					splittedLines.incrementAndGet();
				}
				max.set(Math.max(l.getBytes().length, max.get()));
			}

			@Override
			public void completed(int exitCode) {
				comp.complete(exitCode);
			}

			@Override
			public void starting(String taskRef) {
				// ok
			}

		});
		Integer exitcode = comp.join();
		assertEquals(0, exitcode.intValue());
		System.err.println("count: " + count.get() + ", maxLen: " + max.get());
		assertTrue(count.get() > 50);
		assertTrue(max.get() < 10240);
		assertTrue(splittedLines.get() > 0);
		String fullOutput = full.toString();
		assertEquals(1, fullOutput.chars().filter(theByte -> theByte == '\n').count());

	}

	@Test
	public void testBinaryOutputOverWebsocket() {
		byte[] withoutLF = new byte[32768];
		ThreadLocalRandom.current().nextBytes(withoutLF);
		for (int i = 0; i < withoutLF.length; i++) {
			if (withoutLF[i] == '\n') {
				withoutLF[i] = 0x00;
			}
		}
		// let's end with a LF
		withoutLF[withoutLF.length - 1] = '\n';

		String dir = System.getProperty("java.io.tmpdir");
		String file = dir + "/" + System.currentTimeMillis() + ".junit";
		nc.writeFile(file, new ByteArrayInputStream(withoutLF));
		System.err.println(file + " written.");
		ExecRequest req = ExecRequest.named("junit", "x" + System.currentTimeMillis(), "cat " + file);
		CompletableFuture<Integer> comp = new CompletableFuture<>();
		nc.asyncExecute(req, new ProcessHandler() {

			@Override
			public void log(String l, boolean cont) {
				System.err.println("L: '" + l + "', c: " + cont);
			}

			@Override
			public void completed(int exitCode) {
				comp.complete(exitCode);
			}

			@Override
			public void starting(String taskRef) {
				// ok
			}

		});
		try {
			Integer exitcode = comp.get(10, TimeUnit.SECONDS);
			assertEquals(1, exitcode.intValue());
		} catch (Exception e) {
			fail(e.getMessage());
		} finally {
			new File(file).delete();
		}
	}

	@Test
	public void testWriteTheRead64k() {
		byte[] withoutLF = new byte[65536];
		ThreadLocalRandom.current().nextBytes(withoutLF);
		byte[] enc = Base64.getEncoder().encode(withoutLF);

		String dir = System.getProperty("java.io.tmpdir");
		String file = dir + "/" + System.currentTimeMillis() + ".junit";
		nc.writeFile(file, new ByteArrayInputStream(enc));
		byte[] reRead = nc.read(file);
		assertEquals(enc.length, reRead.length);
	}

	@Test
	public void testMultipleOverWebsocket() throws InterruptedException {
		int COUNT = 5;
		CountDownLatch cdl = new CountDownLatch(COUNT);
		ProcessHandler simple = new ProcessHandler() {

			@Override
			public void log(String l, boolean cont) {
			}

			@Override
			public void completed(int exitCode) {
				cdl.countDown();
			}

			@Override
			public void starting(String taskRef) {
				System.out.println("starting " + taskRef);
			}

		};

		for (int i = 0; i < COUNT; i++) {
			nc.asyncExecute(ExecRequest.anonymous("/usr/bin/git --help"), simple);
		}
		cdl.await(5, TimeUnit.SECONDS);
	}

	@Test
	public void testExternalKillOverWebsocket() throws InterruptedException {
		int count = 5;
		CountDownLatch cdl = new CountDownLatch(count);
		CountDownLatch starts = new CountDownLatch(count);
		Set<String> activeTasks = ConcurrentHashMap.newKeySet();
		Set<Integer> exitCodes = ConcurrentHashMap.newKeySet();
		ProcessHandler simple = new ProcessHandler() {

			@Override
			public void log(String l, boolean cont) {
			}

			@Override
			public void completed(int exitCode) {
				exitCodes.add(exitCode);
				cdl.countDown();
			}

			@Override
			public void starting(String taskRef) {
				System.out.println("starting " + taskRef);
				activeTasks.add(taskRef);
				starts.countDown();
			}

		};

		for (int i = 0; i < count; i++) {
			nc.asyncExecute(ExecRequest.anonymous("/bin/sleep 4"), simple);
		}
		assertTrue(starts.await(1, TimeUnit.SECONDS));
		assertEquals(5, activeTasks.size());

		Iterator<String> it = activeTasks.iterator();
		nc.asyncExecute(ExecRequest.anonymous("/bin/sh -c 'kill -9 " + it.next() + "'"),
				new ProcessHandler.NoOutBlockingHandler());
		nc.asyncExecute(ExecRequest.anonymous("/bin/sh -c 'kill -15 " + it.next() + "'"),
				new ProcessHandler.NoOutBlockingHandler());

		System.err.println("Started " + activeTasks);
		assertTrue(cdl.await(10, TimeUnit.SECONDS));
		assertEquals(3, exitCodes.size());
		assertTrue(exitCodes.contains(0));
		assertTrue(exitCodes.contains(137)); // kill -9
		assertTrue(exitCodes.contains(143)); // kill -15
	}

	@Test
	public void testNoOutOverWebsocket() {
		ExecRequest req = ExecRequest.named("junit", "x" + System.currentTimeMillis(), "/bin/sleep 1");
		NoOutBlockingHandler handler = new ProcessHandler.NoOutBlockingHandler();
		nc.asyncExecute(req, handler);
		int result = handler.get(5, TimeUnit.SECONDS);
		assertEquals("exit code != 0 for /bin/sleep 1", 0, result);
	}

	@Test
	public void testInterruptWebsocketTask() throws InterruptedException, ExecutionException, TimeoutException {
		System.out.println("=============== testInterruptWebsocketTask starts " + new Date());
		ExecRequest req = ExecRequest.named("junit", "x" + System.currentTimeMillis(), "/bin/sleep 10");
		AtomicReference<String> ref = new AtomicReference<>();
		CompletableFuture<Integer> exitFuture = new CompletableFuture<>();
		ProcessHandler ph = new ProcessHandler() {

			@Override
			public void log(String l, boolean cont) {
				System.out.println("LOG: " + l);
			}

			@Override
			public void completed(int exitCode) {
				System.out.println(Thread.currentThread().getName() + " completed " + exitCode);
				exitFuture.complete(exitCode);
			}

			@Override
			public void starting(String taskRef) {
				System.out.println(Thread.currentThread().getName() + " Starting " + taskRef);
				ref.set(taskRef);
			}

		};
		nc.asyncExecute(req, ph);
		Thread.sleep(500);
		assertNotNull("starting was not called", ref.get());
		System.out.println("Interrupting " + ref.get());
		nc.interrupt(ExecDescriptor.forTask(ref.get()));
		int result = exitFuture.get(5, TimeUnit.SECONDS);

		assertEquals("When a process receives a kill -9, its exit code should be 128+signum", 128 + 9, result);
		System.out.println("=============== testInterruptWebsocketTask ends. (" + new Date());

	}

	@Test
	public void testNamedSleepOneThenFindByName() {
		String name = "x" + System.currentTimeMillis();
		ExecRequest req = ExecRequest.named("junit", name, "/bin/sleep 1");
		TaskRef ref = nc.executeCommand(req);
		assertNotNull(ref);
		List<ExecDescriptor> found = nc.getActiveExecutions(ActiveExecQuery.byName("junit", name));
		assertEquals(1, found.size());
		track(ref);
	}

	@Test
	public void testAnonSleepOneThenFindAll() {
		ExecRequest req = ExecRequest.anonymousWithoutOutput("/bin/sleep 1");
		TaskRef ref = nc.executeCommand(req);
		assertNotNull(ref);
		List<ExecDescriptor> found = nc.getActiveExecutions(ActiveExecQuery.all());
		assertEquals(1, found.size());
		track(ref);
		found = nc.getActiveExecutions(ActiveExecQuery.all());
		assertEquals(0, found.size());
	}

	@Test
	public void testAnonSleepInterrupted() {
		ExecRequest req = ExecRequest.anonymousWithoutOutput("/bin/sleep 4");
		TaskRef ref = nc.executeCommand(req);
		assertNotNull(ref);
		List<ExecDescriptor> found = nc.getActiveExecutions(ActiveExecQuery.all());
		assertEquals(1, found.size());
		System.out.println(found.get(0));
		nc.interrupt(ExecDescriptor.forTask(ref.id));
		found = nc.getActiveExecutions(ActiveExecQuery.all());
		assertEquals(0, found.size());
		track(ref);
	}

	@Test
	public void testExecOptionsFailIfExists() {
		ExecRequest req = ExecRequest.named("ju", "nit", "/bin/sleep 2", Options.DISCARD_OUTPUT,
				Options.FAIL_IF_EXISTS);
		TaskRef ref = nc.executeCommand(req);
		assertNotNull(ref);
		try {
			TaskRef secondRef = nc.executeCommand(req);
			track(ref);
			track(secondRef);
			fail("execute should fail here");
		} catch (Exception e) {
			System.out.println("Got " + e.getMessage() + ", great !");
			track(ref);
		}
	}

	@Test
	public void testExecOptionsFailIfGroupExists() {
		ExecRequest req1 = ExecRequest.named("ju", "nit1", "/bin/sleep 2");
		ExecRequest req2 = ExecRequest.named("ju", "nit2", "/bin/sleep 2", Options.FAIL_IF_GROUP_EXISTS);
		TaskRef ref = nc.executeCommand(req1);
		assertNotNull(ref);
		try {
			TaskRef secondRef = nc.executeCommand(req2);
			track(ref);
			track(secondRef);
			fail("execute should fail here");
		} catch (Exception e) {
			System.out.println("Got " + e.getMessage() + ", great !");
			track(ref);
		}
	}

	@Test
	public void testExecOptionsReplace() {
		String nit = "nit" + System.currentTimeMillis();
		ExecRequest req1 = ExecRequest.named("ju", nit, "/bin/sleep 2");
		ExecRequest req2 = ExecRequest.named("ju", nit, "/bin/sleep 2", Options.REPLACE_IF_EXISTS);
		TaskRef ref = nc.executeCommand(req1);
		assertNotNull(ref);
		ActiveExecQuery query = ActiveExecQuery.byName("ju", nit);
		List<ExecDescriptor> found = nc.getActiveExecutions(query);
		assertEquals(1, found.size());
		TaskRef secondRef = nc.executeCommand(req2);
		found = nc.getActiveExecutions(query);
		assertEquals(1, found.size());
		assertNotEquals(ref.id, secondRef.id);
		track(ref);
		track(secondRef);

	}

	@Test
	public void testExitCode() {
		ExecRequest req = ExecRequest.named("junit", "x" + System.currentTimeMillis(), "/bin/bash -c 'exit 42'");
		TaskRef ref = nc.executeCommand(req);
		assertNotNull(ref);
		ExitList el = NCUtils.waitFor(nc, ref);
		assertEquals(42, el.getExitCode());
	}

	@Test
	public void executeCommand_noFakeEmptyLines() {
		TaskRef ref = nc.executeCommand("sleep 1");
		ExitList values = NCUtils.waitFor(nc, ref);

		assertEquals(0, values.size());
	}

	@Test
	public void testListWithPlusSign() throws IOException {
		new File("/tmp/a + b").mkdirs();
		List<FileDescription> found = nc.listFiles("/tmp/a + b");
		assertNotNull(found);
	}

	private void track(TaskRef ref) {
		TaskStatus status;
		do {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
			status = nc.getExecutionStatus(ref);
		} while (status != null && !status.state.ended);
	}
}
