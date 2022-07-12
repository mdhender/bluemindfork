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
package net.bluemind.pop3.endpoint.tests;

import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.lib.vertx.VertxPlatform;

public class Pop3Tests {

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		VertxPlatform.spawnBlocking(10, TimeUnit.SECONDS);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testSocketIsWorking() throws IOException {
		try (Socket sock = new Socket()) {
			ConcurrentLinkedDeque<String> queue = rawSocket(sock);

			OutputStream out = sock.getOutputStream();
			out.write("USER toto\r\n".getBytes());
			out.write("PASS titi\r\n".getBytes());
			out.flush();

			await().atMost(4, TimeUnit.SECONDS)
					.until(() -> (queue.stream().filter(s -> s.contains("+OK POP3 ready")).count() > 0));

			await().atMost(4, TimeUnit.SECONDS)
					.until(() -> (queue.stream().filter(s -> s.contains("merci")).count() > 0));

		}
	}

	private ConcurrentLinkedDeque<String> rawSocket(Socket sock) throws IOException {
		sock.connect(new InetSocketAddress("127.0.0.1", 1110));
		ConcurrentLinkedDeque<String> queue = new ConcurrentLinkedDeque<>();
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
					queue.offer(resp);
				}
			} catch (Exception e) {
				// ok
			}
		});
		t.start();
		return queue;
	}

}
