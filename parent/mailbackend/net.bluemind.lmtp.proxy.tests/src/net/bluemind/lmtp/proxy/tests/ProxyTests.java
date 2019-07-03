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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.lmtp.testhelper.server.MockServer;
import net.bluemind.lmtp.testhelper.server.ProxyServer;

public class ProxyTests {

	@Before
	public void before() throws Exception {
	}

	@After
	public void after() throws Exception {
	}

	@Test
	public void mockServerStarted() throws UnknownHostException, IOException {
		MockServer.start();
		System.err.println("****** AFTER MOCK START **** ");
		Socket sock = new Socket("127.0.0.1", 2424);
		assertTrue(sock.isConnected());
		sock.close();
		MockServer.stop();
	}

	@Test
	public void proxyServerStarted() throws UnknownHostException, IOException {
		MockServer.start();
		ProxyServer.start();
		System.err.println("****** AFTER PROXY START **** ");
		Socket sock = new Socket("127.0.0.1", 2400);
		assertTrue(sock.isConnected());
		sock.close();
		ProxyServer.stop();
		MockServer.stop();
	}

}
