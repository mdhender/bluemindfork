/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.imap.endpoint.tests;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.IMAPHeaders;
import net.bluemind.imap.ListResult;
import net.bluemind.imap.StoreClient;
import net.bluemind.imap.TaggedResult;
import net.bluemind.imap.endpoint.EndpointConfig;
import net.bluemind.imap.endpoint.tests.driver.MockModel;
import net.bluemind.lib.vertx.VertxPlatform;

public class ClientBasedTests {
	private int port;
	private MockModel mdl;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		this.port = EndpointConfig.get().getInt("imap.port");

		mdl = MockModel.INSTANCE;

		mdl.registerFolder(UUID.randomUUID(), "INBOX");
		mdl.registerFolder(UUID.randomUUID(), "Sent");
		mdl.registerFolder(UUID.randomUUID(), "Draft");
		mdl.registerFolder(UUID.randomUUID(), "Trash");
		mdl.registerFolder(UUID.randomUUID(), "Junk");
		mdl.registerFolder(UUID.randomUUID(), "Outbox");
		mdl.registerFolder(UUID.randomUUID(), "Templates");
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testLoginCapabilitySelect() {
		try (StoreClient sc = new StoreClient("127.0.0.1", port, "tom@devenv.blue", "tom")) {
			assertTrue(sc.login());
			try {
				Set<String> capas = sc.capabilities();
				assertTrue(capas.contains("BM-ROCKS"));
				sc.select("INBOX");
				assertTrue(sc.tagged("SELECT INBOX (QRESYNC 42)").isOk());
				sc.tagged("LOGOUT");
			} catch (IMAPException e) {
				fail(e.getMessage());
			}
		}
	}

	private InputStream eml() {
		return new ByteArrayInputStream("From: gg@gmail.com\r\n".getBytes());
	}

	@Test
	public void testAppendCommands() {
		try (StoreClient sc = new StoreClient("127.0.0.1", port, "tom@devenv.blue", "tom")) {
			assertTrue(sc.login());
			int uid = sc.append("INBOX", eml(), new FlagsList());
			System.err.println("uid: " + uid);

			FlagsList seen = new FlagsList();
			seen.add(Flag.SEEN);
			uid = sc.append("Sent", eml(), seen);
			System.err.println("uid: " + uid);

			uid = sc.append("Trash", eml(), seen, new Date());
			System.err.println("uid: " + uid);
		}
	}

	@Test
	public void testUidStore() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", port, "tom@devenv.blue", "tom")) {
			assertTrue(sc.login());

			sc.select("INBOX");

			FlagsList seen = new FlagsList();
			seen.add(Flag.SEEN);
			sc.uidStore(Arrays.asList(1, 2, 3), seen, true);

			sc.uidStore(Arrays.asList(1, 2, 3), seen, false);
		}
	}

	@Test
	public void testUidCopy() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", port, "tom@devenv.blue", "tom")) {
			assertTrue(sc.login());

			sc.select("INBOX");

			Map<Integer, Integer> copied = sc.uidCopy("12", "Trash");
			assertEquals(42, ((Integer) copied.get(12)).intValue());
		}
	}

	@Test
	public void testUidFetchFlags() {
		try (StoreClient sc = new StoreClient("127.0.0.1", port, "tom@devenv.blue", "tom")) {
			assertTrue(sc.login());
			try {
				sc.select("INBOX");
				sc.uidFetchFlags("1:*");
			} catch (IMAPException e) {
				fail(e.getMessage());
			}
		}
	}

	@Test
	public void testUidFetchHeaders() {
		try (StoreClient sc = new StoreClient("127.0.0.1", port, "tom@devenv.blue", "tom")) {
			assertTrue(sc.login());
			try {
				sc.select("INBOX");
				Collection<IMAPHeaders> headers = sc.uidFetchHeaders(Collections.singleton(27),
						new String[] { "Subject", "Date", "Message-ID" });
				for (IMAPHeaders h : headers) {
					System.err.println("h: " + h.getRawHeaders());
				}
			} catch (IMAPException e) {
				fail(e.getMessage());
			}
		}
	}

	@Test
	public void testListXlist() {
		try (StoreClient sc = new StoreClient("127.0.0.1", port, "tom@devenv.blue", "tom")) {
			assertTrue(sc.login());
			ListResult result = sc.listAll();
			assertTrue(result.size() > 5);
			result = sc.listSubscribed();
			assertTrue(result.size() > 5);

			TaggedResult rcStyle = sc.tagged("list \"\" Drafts");
			assertTrue(rcStyle.isOk());

			TaggedResult anotherStyle = sc.tagged("lsub \"\" \"%\"");
			assertTrue(anotherStyle.isOk());

		}
	}

	@Test
	public void testThunderbirdStyleLogin() throws IOException {
		try (SocketClient sockc = new SocketClient()) {
			sockc.write("A0 LOGIN \"tom@f8de2c4a.internal\" \"tom\"\r\n");
			sockc.waitFor("User logged");
		}
	}

	@Test
	public void testNginxStyleLogin() throws IOException {
		try (SocketClient sockc = new SocketClient()) {
			sockc.write("A0 LOGIN {21+}\r\ntom@f8de2c4a.internal {3+}\r\ntom\r\n");
			sockc.waitFor("User logged");
		}
	}

	@Test
	public void testLiteralStyleLogin() throws IOException {
		try (SocketClient sockc = new SocketClient()) {
			sockc.write("A0 LOGIN {21}\r\ntom@f8de2c4a.internal {3}\r\ntom\r\n");
			sockc.waitFor("+ OK");
			sockc.waitFor("User logged");
		}
	}

	@Test
	public void testCreateSimple() throws IOException {
		try (SocketClient sockc = new SocketClient()) {
			sockc.write("A0 LOGIN {21+}\r\ntom@f8de2c4a.internal {3+}\r\ntom\r\n");
			sockc.waitFor("User logged");
			sockc.clearQueue();
			sockc.write("A1 CREATE Bonjour toi\r\n");
			sockc.waitFor("create completed");
			assertNotNull("folder not found", mdl.byName("Bonjour toi"));
		}
	}

	@Test
	public void testCreateQuoted() throws IOException {
		try (SocketClient sockc = new SocketClient()) {
			sockc.write("A0 LOGIN {21+}\r\ntom@f8de2c4a.internal {3+}\r\ntom\r\n");
			sockc.waitFor("User logged");
			sockc.clearQueue();
			sockc.write("A1 CREATE \"Bonjour toi\"\r\n");
			sockc.waitFor("create completed");
			assertNotNull("folder not found", mdl.byName("Bonjour toi"));
		}
	}

	@Test
	public void testCreateLitteral() throws IOException {
		try (SocketClient sockc = new SocketClient()) {
			sockc.write("A0 LOGIN {21+}\r\ntom@f8de2c4a.internal {3+}\r\ntom\r\n");
			sockc.waitFor("User logged");
			sockc.clearQueue();
			sockc.write("A1 CREATE {11}\r\nBonjour toi/et moi\r\n");
			sockc.waitFor("create completed");
			assertNotNull("folder not found folders: " + mdl, mdl.byName("Bonjour toi/et moi"));
		}
	}

	@SuppressWarnings("unused")
	private static class SocketClient implements AutoCloseable {
		private final ConcurrentLinkedDeque<String> readqueue;
		private final Socket sock;
		private final OutputStream out;

		public SocketClient() throws IOException {
			sock = new Socket();
			sock.connect(new InetSocketAddress("127.0.0.1", EndpointConfig.get().getInt("imap.port")));
			readqueue = new ConcurrentLinkedDeque<>();
			Thread t = new Thread(() -> {
				try {
					InputStream in = sock.getInputStream();
					byte[] buf = new byte[1024];
					while (true) {
						int read = in.read(buf, 0, 1024);
						if (read == -1) {
							break;
						}
						String resp = new String(buf, 0, read);
						System.err.println("S: " + resp);
						readqueue.offer(resp);
					}
				} catch (Exception e) {
					// ok
				}
			});
			t.start();
			out = sock.getOutputStream();
		}

		public ConcurrentLinkedDeque<String> queue() {
			return readqueue;
		}

		@Override
		public void close() {
			try {
				sock.close();
			} catch (Exception e) {
			}
		}

		public void write(String s) throws IOException {
			out.write(s.getBytes());
			out.flush();
		}

		public void write(byte[] b) throws IOException {
			out.write(b);
			out.flush();
		}

		public void waitFor(String s) {
			waitFor(readstring -> readstring.contains(s));
		}

		public void waitFor(Predicate<String> filter) {
			await().atMost(1, TimeUnit.SECONDS).until(() -> (readqueue.stream().filter(filter).count() > 0));
		}

		public void clearQueue() {
			readqueue.clear();
		}
	}

}
