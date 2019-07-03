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
package net.bluemind.lmtp.proxy.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.lmtp.impl.CoreStateListener;
import net.bluemind.lmtp.testhelper.model.MailboxesModel;
import net.bluemind.lmtp.testhelper.model.MockServerStats;
import net.bluemind.lmtp.testhelper.server.MockServer;
import net.bluemind.lmtp.testhelper.server.ProxyServer;
import net.bluemind.system.api.SystemState;

public class ProxyToMockSetupTests {

	@Before
	public void before() throws Exception {
		MockServerStats.get().reset();
		MailboxesModel.get().reset();
		MockServer.start();
		ProxyServer.start();
		CoreStateListener.state = SystemState.CORE_STATE_RUNNING;
	}

	@After
	public void after() throws Exception {
		ProxyServer.stop();
		MockServer.stop();
	}

	@Test
	public void proxyConnectsToMock()
			throws UnknownHostException, IOException, InterruptedException, ExecutionException, TimeoutException {
		Socket sock = new Socket("127.0.0.1", 2400);
		assertTrue(sock.isConnected());
		sock.setSoTimeout(500);
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(sock.getInputStream(), StandardCharsets.US_ASCII));
		String line = reader.readLine();
		System.out.println(line);
		assertEquals(MockServer.BANNER, line);
		CompletableFuture<Void> cf = MockServer.expectClose();
		sock.close();
		cf.thenAccept(v -> {
			System.out.println("Mock socket close event.");
			assertEquals(1, MockServerStats.get().openConnections());
			assertEquals(1, MockServerStats.get().closedConnections());
		}).get(1, TimeUnit.SECONDS);
	}

}
