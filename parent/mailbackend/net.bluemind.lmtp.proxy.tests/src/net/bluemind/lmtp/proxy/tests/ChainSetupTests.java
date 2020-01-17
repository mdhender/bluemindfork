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

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.Vertx;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.lmtp.testhelper.client.VertxLmtpClient;
import net.bluemind.lmtp.testhelper.model.MailboxesModel;
import net.bluemind.lmtp.testhelper.model.MockServerStats;
import net.bluemind.lmtp.testhelper.server.MockServer;
import net.bluemind.lmtp.testhelper.server.ProxyServer;

public class ChainSetupTests {

	@Before
	public void before() throws Exception {
		MockServerStats.get().reset();
		MailboxesModel.get().reset();
		MockServer.start();
		ProxyServer.start();
		System.err.println("***** BEFORE FINISHED *****");
	}

	@After
	public void after() throws Exception {
		ProxyServer.stop();
		MockServer.stop();
		System.err.println("********** AFTER TEST *********");
	}

	@Test
	public void proxyConnectsToMock()
			throws UnknownHostException, IOException, InterruptedException, ExecutionException, TimeoutException {
		Vertx vertx = VertxPlatform.getVertx();
		VertxLmtpClient client = new VertxLmtpClient(vertx, "127.0.0.1", 2400);
		CompletableFuture<Void> cf = MockServer.expectClose();
		client.connect().thenCompose(banner -> {
			System.out.println("Connected, banner: " + banner);
			return client.close();
		}).thenCompose(v -> {
			System.out.println("Client close.");
			return cf;
		}).thenAccept(v -> {
			System.out.println("Mock socket close event.");
			assertEquals(1, MockServerStats.get().openConnections());
			assertEquals(1, MockServerStats.get().closedConnections());
		}).get(1, TimeUnit.SECONDS);
	}

}
