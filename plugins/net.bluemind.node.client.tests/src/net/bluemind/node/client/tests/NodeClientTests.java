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
package net.bluemind.node.client.tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import junit.framework.TestCase;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.node.api.ExitList;
import net.bluemind.node.api.FileDescription;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.INodeClientFactory;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.client.AHCNodeClientFactory;

public class NodeClientTests extends TestCase {

	private static final INodeClientFactory facto = new AHCNodeClientFactory();

	public void testFactory() {
		try {
			new AHCNodeClientFactory();
		} catch (Throwable t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
	}

	public void testCreateClient() {
		try {
			INodeClient cli = facto.create("localhost");
			assertNotNull(cli);
			INodeClient cliTwo = facto.create("localhost");
			assertNotNull(cliTwo);
		} catch (ServerFault e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testPings() {
		try {
			INodeClient cli = facto.create("localhost");
			assertNotNull(cli);
			for (int i = 0; i < 50; i++) {
				System.out.println("Ping " + i + "...");
				cli.ping();
				System.out.println("Pong " + i + ".");
			}
		} catch (ServerFault e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testExecMissing() throws ServerFault {
		String cmd = "/Users/tom/doesNotExist.sh";

		INodeClient nc = client();
		try {
			nc.executeCommand(cmd);
			fail("That should not have started");
		} catch (ServerFault e) {
			// expected
			return;
		}
		fail("No serverfault");
	}

	public void testExitCode() throws ServerFault {
		String cmd = "/Users/tom/exit42.sh";

		INodeClient nc = client();
		TaskRef ref = nc.executeCommand(cmd);
		assertNotNull(ref);
		ExitList el = NCUtils.waitFor(nc, ref);
		assertEquals(42, el.getExitCode());
	}

	public void testExecFast() throws ServerFault {
		String cmd = "ls /";

		INodeClient nc = client();
		TaskRef ref = nc.executeCommand(cmd);
		assertNotNull(ref);
		List<String> output = NCUtils.waitFor(nc, ref);
		assertNotNull(output);
		System.out.println("output: '" + output + "'");
		assertTrue("Output is missing", output.size() > 0);
		boolean found = false;
		for (String o : output) {
			if (o.contains("tmp")) {
				found = true;
			}
		}
		assertTrue(found);
	}

	public void testExecWithClientReuse() throws ServerFault {
		String cmd = "echo -n";

		INodeClient nc = client();
		for (int i = 0; i < 100; i++) {
			System.out.println("Execution " + i);
			ExitList el = NCUtils.exec(nc, cmd);
			assertEquals(0, el.getExitCode());
		}
	}

	public void testExecFastLoop() throws ServerFault {
		int fail = 0;
		int success = 0;
		for (int i = 0; i < 100; i++) {
			System.out.println("Execution " + i);
			try {
				testExecFast();
				success++;
			} catch (Throwable t) {
				t.printStackTrace();
				fail++;
			}
		}
		System.out.println("Summary, fail: " + fail + " success: " + success);
		assertTrue(fail == 0);
	}

	public void testExecSlow() throws ServerFault {
		String cmd = "sleep 5";

		INodeClient nc = client();
		TaskRef ref = nc.executeCommand(cmd);
		assertNotNull(ref);
		LinkedList<String> output = runTask(nc, ref);
		assertNotNull(output);
		assertTrue(output.size() == 0);
	}

	public void testExecNotExistingRef() throws ServerFault {
		TaskRef ref = TaskRef.create("" + System.nanoTime());
		try {
			TaskStatus status = client().getExecutionStatus(ref);
			System.out.println("status success: " + status.state.succeed + ", complete: " + status.state.ended);
		} catch (Throwable t) {
			t.printStackTrace();
			fail("Checking a non existing task must not throw.");
		}
	}

	private LinkedList<String> runTask(INodeClient nc, TaskRef ref) throws ServerFault {
		TaskStatus ts = null;
		LinkedList<String> outLines = new LinkedList<String>();
		do {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				fail("should not be interrupted");
			}
			ts = nc.getExecutionStatus(ref);
		} while (!ts.state.ended);

		System.out.println("--- OUTPUT START ---");
		System.out.println(ts.lastLogEntry);
		System.out.println("--- OUTPUT END ---");

		if (!ts.state.succeed) {
			fail("Command execution failed");
		}

		return outLines;
	}

	public void testRead() throws ServerFault {
		testRead(client());
	}

	private void testRead(INodeClient nc) throws ServerFault {
		byte[] data = nc.read("/etc/bm/bm.ini");
		assertNotNull(data);
		assertTrue(data.length > 0);

		String content = new String(data);
		assertTrue(content.contains("dbtype"));
	}

	public void testReadNotExistingFile() throws ServerFault {
		INodeClient nc = client();

		byte[] data = nc.read("/does.not.exist." + System.currentTimeMillis());
		assertNotNull(data);
		assertTrue(data.length == 0);
	}

	public void testReadLoop() throws ServerFault {
		INodeClient nc = client();
		int fail = 0;
		int success = 0;
		for (int i = 0; i < 1000; i++) {
			System.out.println("Execution " + i);
			try {
				testRead(nc);
				success++;
			} catch (Throwable t) {
				System.err.println("e: " + t.getMessage());
				fail++;
			}
		}
		System.out.println("Summary, fail: " + fail + " success: " + success);
		assertTrue(fail == 0);
	}

	public void testWriteRead() throws ServerFault {
		testWriteRead(client());
	}

	private void testWriteRead(INodeClient nc) throws ServerFault {

		String path = System.getProperty("user.home") + "/sch" + System.currentTimeMillis() + ".txt";
		String orig = "File content: €uro symbol.\n";
		byte[] toWrite = orig.getBytes();
		nc.writeFile(path, new ByteArrayInputStream(toWrite));
		byte[] readBack = nc.read(path);
		String afterWriteRead = new String(readBack);
		assertEquals(orig, afterWriteRead);
	}

	public void testReadBigFile() throws ServerFault, IOException {
		INodeClient nc = client();

		// This is a 800MB file on my computer.
		String path = "/Users/tom/java_pid21878.hprof";
		File f = new File(path);
		if (!f.exists() || !f.isFile()) {
			fail("You need a _file_ name " + path + " on your computer");
		}
		long size = f.length();
		f = null;
		InputStream data = nc.openStream(path);
		assertNotNull(data);

		byte[] buf = new byte[4096];
		int read = 0;
		int loop = 1;
		try {
			while (true) {
				int remaining = data.read(buf, 0, 4096);
				if (remaining == -1) {
					break;
				}
				read += remaining;
				loop++;
			}
		} catch (Throwable t) {
			System.out.println("size: " + size + ", read: " + read + ", loop: " + loop);
			t.printStackTrace();
			fail();
		} finally {
			data.close();
		}
		System.out.println("size: " + size + ", read: " + read + ", loop: " + loop);
		assertEquals(size, read);
	}

	public void testBigFileOpenCloseLoop() throws ServerFault, IOException {
		INodeClient nc = client();

		// This is a 800MB file on my computer.
		String path = "/Users/tom/java_pid21878.hprof";
		File f = new File(path);
		if (!f.exists() || !f.isFile()) {
			fail("You need a _file_ name " + path + " on your computer");
		}
		f = null;
		InputStream data = nc.openStream(path);
		assertNotNull(data);
		data.close();

		// If your file is as big as mine, this test will tranfer 4GB over
		// nodeClient
		for (int i = 0; i < 5; i++) {
			data = nc.openStream(path);
			data.close();
		}

	}

	public void testSmallFileOpenCloseLoop() throws ServerFault, IOException {
		String path = System.getProperty("user.home") + "/small." + System.currentTimeMillis() + ".txt";
		String orig = "File content: €uro symbol.\n";
		byte[] toWrite = orig.getBytes();

		INodeClient nc = client();
		nc.writeFile(path, new ByteArrayInputStream(toWrite));
		File f = new File(path);
		if (!f.exists() || !f.isFile()) {
			fail("You need a _file_ named " + path + " on your computer");
		}
		f.deleteOnExit();
		InputStream data = nc.openStream(path);
		assertNotNull(data);
		data.close();

		for (int i = 0; i < 100; i++) {
			data = nc.openStream(path);
			ByteStreams.toByteArray(data);
			data.close();
		}
	}

	public void testBackupStyleCopy() throws ServerFault, IOException {
		String ts = "" + System.currentTimeMillis();
		int cnt = 0;
		String home = System.getProperty("user.home");
		String dir = home + "/junit." + ts;
		new File(dir).mkdir();
		String pf = dir + "/small." + ts + ".";
		String path = pf + cnt + ".txt";
		String orig = "File content: €uro symbol.\n";
		byte[] toWrite = orig.getBytes();

		INodeClient nc = client();
		nc.writeFile(path, new ByteArrayInputStream(toWrite));
		File f = new File(path);
		if (!f.exists() || !f.isFile()) {
			fail("You need a _file_ named " + path + " on your computer");
		}
		f.deleteOnExit();
		InputStream data = nc.openStream(path);
		assertNotNull(data);
		data.close();
		data = null;

		for (int i = 0; i < 500; i++) {

			InputStream toTransfer = nc.openStream(pf + cnt + ".txt");
			TaskRef mkdir = nc.executeCommand("mkdir -p " + dir);
			runTask(nc, mkdir);
			cnt++;
			String newFilePath = pf + cnt + ".txt";
			nc.writeFile(newFilePath, toTransfer);
			List<FileDescription> dirs = nc.listFiles(dir);
			System.err.println("Files count: " + dirs.size());
			dirs = null;
			new File(newFilePath).deleteOnExit();
		}
	}

	public void testPostmapStyle() throws ServerFault {
		// INodeClient nc = client("172.16.45.161");
		// String p = "/etc/postfix";
		INodeClient nc = client("127.0.0.1");
		String p = "/Users/tom/postfix";
		String[] maps = { p + "/virtual_mailbox-flat", p + "/transport-flat", p + "/virtual_alias-flat",
				p + "/virtual_domains-flat" };
		for (int i = 0; i < 1000; i++) {
			for (String map : maps) {
				byte[] mbf = nc.read(map);
				nc.writeFile(map, new ByteArrayInputStream(mbf));
				NCUtils.exec(nc, "/usr/sbin/postmap " + map);
				NCUtils.exec(nc, "/bin/mv -f " + map + ".db " + map.replace("-flat", "") + ".db");
			}
		}
	}

	public void testWRLoop() throws ServerFault {
		INodeClient nc = client();
		int fail = 0;
		int success = 0;
		for (int i = 0; i < 100; i++) {
			System.out.println("Execution " + i);
			try {
				testWriteRead(nc);
				success++;
			} catch (Throwable t) {
				fail++;
			}
		}
		File f = new File(System.getProperty("user.home"));
		String[] toDel = f.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				// System.err.println(name);
				return name.startsWith("sch") && name.endsWith(".txt");
			}
		});
		for (String s : toDel) {
			File td = new File(s);
			td.delete();
		}
		System.out.println("Summary, fail: " + fail + " success: " + success);
		assertTrue(fail == 0);
	}

	public void testWriteLoop() throws ServerFault {
		int fail = 0;
		int success = 0;
		String path = System.getProperty("user.home") + "/wloop." + System.currentTimeMillis() + ".txt";
		String orig = "File content: €uro symbol.\n";
		byte[] toWrite = orig.getBytes();
		INodeClient nc = client();
		for (int i = 0; i < 100; i++) {
			if ((i % 100) == 0) {
				System.out.print(".");
			}
			try {
				nc.writeFile(path, new ByteArrayInputStream(toWrite));
				success++;
			} catch (Throwable t) {
				fail++;
			}
		}
		new File(path).delete();
		System.out.println("Summary, fail: " + fail + " success: " + success);
		assertTrue(fail == 0);
	}

	public void testExecParallel() throws ServerFault, IOException, InterruptedException {
		int CNT = 10;
		EventExecutorGroup exec = new DefaultEventExecutorGroup(3);
		final AtomicLong total = new AtomicLong();
		final AtomicLong failed = new AtomicLong();
		final INodeClient nc = client();
		// Random r = new Random();
		Runnable sleepCommand = new Runnable() {

			@Override
			public void run() {
				try {
					long val = total.incrementAndGet();
					ExitList el = NCUtils.exec(nc, "sleep " + (1 + (val % 3)));
					if (el.getExitCode() != 0) {
						failed.incrementAndGet();
					}
				} catch (Throwable e) {
					e.printStackTrace();
					failed.incrementAndGet();
				}
			}
		};
		for (int i = 0; i < CNT; i++) {
			exec.execute(sleepCommand);
			assertEquals(0, failed.get());
		}
		Future<?> down = exec.shutdownGracefully();
		System.out.println("Waiting for threads termination... " + total.get() + "/" + CNT);
		down.await();
		assertEquals(0, failed.get());
	}

	public void testWriteParallel() throws ServerFault, IOException {
		String path = System.getProperty("user.home") + "/wloop." + System.currentTimeMillis() + ".txt";
		String orig = "File content: €uro symbol.\n";

		byte[] toWrite = orig.getBytes();
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		for (int i = 0; i < 1000; i++) {
			bout.write(toWrite);
		}
		bout.close();
		toWrite = bout.toByteArray();
		bout = null;

		System.out.println("File size will be " + toWrite.length);
		INodeClient nc = client();
		ExecutorService pool = new BlockingExecutor(16);
		StatRecorder sr = new StatRecorder();
		WriteOperation wo = new WriteOperation(facto, sr, path, toWrite);
		for (int i = 0; i < 2500; i++) {
			if ((i % 100) == 0) {
				System.out.print(".");
			}
			pool.execute(wo);
		}
		System.out.println("All submitted.");
		pool.shutdown();
		boolean down = false;
		do {
			try {
				down = pool.awaitTermination(2, TimeUnit.SECONDS);
				System.out.print(".");
			} catch (InterruptedException e) {
				System.out.print("X");
			}
		} while (!down);
		System.out.println("Pool terminated.");
		byte[] read = nc.read(path);
		assertEquals(toWrite.length, read.length);
		new File(path).delete();

		System.out.println("Summary:\n" + sr.toString());
		assertFalse(sr.hasFailed());
	}

	public void testListFiles() {
		try {
			INodeClient nc = client();

			System.out.println("Searching for files into directory: /etc/bm");
			List<FileDescription> results = nc.listFiles("/etc/bm");
			assertTrue(results.size() > 0);

			boolean foundBmIni = false;
			for (FileDescription fileDesc : results) {
				System.out.println("Found file path: " + fileDesc.getPath() + ", name: " + fileDesc.getName());
				if (fileDesc.getName().equals("bm.ini")) {
					foundBmIni = true;
				}
			}
			assertTrue(foundBmIni);
			System.out.println("Found file: /etc/bm/bm.ini");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Test thrown an axception");
		}
	}

	public void testListNonExistingPath() {
		try {
			INodeClient nc = client();

			List<FileDescription> results = nc.listFiles("/doesNotExist" + System.currentTimeMillis());
			assertEquals("listFiles on a not existing path must return an empty list", 0, results.size());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Test thrown an axception");
		}
	}

	public void testComplexPath() {
		try {
			INodeClient nc = client();

			File dir = new File("/etc/bm/a^b + c/");
			dir.mkdirs();
			System.out.println("d: " + dir.getAbsolutePath());
			File file = new File(dir, String.valueOf(System.currentTimeMillis()));
			Files.touch(file);
			System.out.println("f: " + file.getAbsolutePath());
			System.out.println("Searching for files in: " + dir.getAbsolutePath());
			List<FileDescription> results = nc.listFiles(dir.getAbsolutePath());
			assertEquals(1, results.size());
			nc.writeFile(file.getAbsolutePath(), new ByteArrayInputStream("X".getBytes()));
			byte[] cnt = nc.read(file.getAbsolutePath());
			assertEquals(1, cnt.length);
			assertEquals("X", new String(cnt));
			file.delete();
			dir.delete();
		} catch (Exception e) {
			e.printStackTrace();
			fail("Test thrown an axception");
		}
	}

	public void testListFile() {
		try {
			INodeClient nc = client();

			System.out.println("Searching for file: /etc/bm/bm.ini");
			List<FileDescription> results = nc.listFiles("/etc/bm/bm.ini");
			assertEquals(1, results.size());

			FileDescription fileDesc = results.get(0);
			assertEquals(fileDesc.getPath(), "/etc/bm/bm.ini");
			System.out.println("Found file: /etc/bm/bm.ini");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Test thrown an axception");
		}
	}

	public void testListFileLoop() {
		try {
			INodeClient nc = client();

			int maxLoop = 100;
			for (int count = 0; count < maxLoop; count++) {
				System.out.println("#" + (count + 1) + " Searching for file: /etc/bm");
				List<FileDescription> results = nc.listFiles("/etc/bm");
				assertTrue(results.size() > 0);
			}

			System.out.println("Loop " + maxLoop + " times with success");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Test thrown an axception");
		}
	}

	public void testListFileFilterExtension() {
		try {
			INodeClient nc = client();

			System.out.println("Searching for file: /etc/bm/*ini");
			List<FileDescription> results = nc.listFiles("/etc/bm", "ini");
			assertEquals(1, results.size());

			FileDescription fileDesc = results.get(0);
			// /private is for osx symlinks on /etc
			assertEquals(fileDesc.getPath().replace("/private", ""), "/etc/bm/bm.ini");
			System.out.println("Found file: " + fileDesc.getPath());

			System.out.println("Searching for file: /etc/bm/*doesnotexist");
			results = nc.listFiles("/etc/bm", "doesnotexist");
			assertEquals(0, results.size());
			System.out.println("File: /etc/bm/*doesnotexist, not found");

			System.out.println("Searching for file: /usr/lib/*.dylib");
			results = nc.listFiles("/usr/lib", "dylib");
			assertTrue(results.size() > 1);
			System.out.println("Found: " + results.size() + " files: /usr/lib/*.dylib");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Test thrown an axception");
		}
	}

	private INodeClient client() throws ServerFault {
		return client("127.0.0.1");
	}

	private INodeClient client(String ip) throws ServerFault {
		return facto.create(ip);
	}

	public void testLongCommand() throws ServerFault {
		INodeClient nc = client();

		TaskRef ref = nc.executeCommandNoOut("find " + System.getProperty("user.home"));
		System.out.println("Waiting for command ending...");
		ExitList data = NCUtils.waitFor(nc, ref);
		System.out.println("Output: " + data.size());
	}
}
