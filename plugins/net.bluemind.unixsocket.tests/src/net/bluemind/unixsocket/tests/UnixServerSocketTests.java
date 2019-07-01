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
package net.bluemind.unixsocket.tests;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import junit.framework.TestCase;
import net.bluemind.unixsocket.SocketClosedException;
import net.bluemind.unixsocket.UnixDomainSocketChannel;
import net.bluemind.unixsocket.UnixServerSocket;

public class UnixServerSocketTests extends TestCase {

	protected String path = System.getProperty("user.home") + File.separatorChar + "junit_unix.sock";

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testContructor() throws IOException {
		UnixServerSocket uss = new UnixServerSocket(path);
		System.out.println("contructor has returned");
		assertNotNull(uss);
		assertTrue(new File(path).exists());
		uss.close();
		System.out.println("unit test complete");
	}

	public void testContructorError() {
		try {
			UnixServerSocket uss = new UnixServerSocket("/forbidden.sock");
			uss.close();
			fail("Call should not have succeeded");
		} catch (IOException e) {
			System.out.println("Got an error, but this is what we were testing, it's ok");
		}
	}

	public void testConstructorLoop() throws IOException {
		int COUNT = 100000;

		long time = System.currentTimeMillis();
		for (int i = 0; i < COUNT; i++) {
			UnixServerSocket uss = new UnixServerSocket(path);
			uss.close();
		}
		time = System.currentTimeMillis() - time;
		System.out.println("Performed " + COUNT + " create/delete of UNIX sockets in " + time + "ms.");
	}

	/**
	 * Will open a socket, wait for connections, then try to close it 5sec
	 * later.
	 * 
	 * @throws IOException
	 */
	public void testAccept() throws IOException {
		long oldTime = System.currentTimeMillis();
		final UnixServerSocket uss = new UnixServerSocket(path);
		long testDelay = 100;

		// accept() blocks, so we need to send the close() from another thread.
		Timer t = new Timer();
		t.schedule(new TimerTask() {
			public void run() {
				try {
					uss.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}, testDelay);

		try {
			UnixDomainSocketChannel channel = uss.accept();
			System.out.println("returned from accept. channel: " + channel);
			fail("We expected a socket closed exception, but got a connection on the socket");
		} catch (SocketClosedException sce) {
			// that's what we expect
		} catch (IOException e) {
			fail("should get a socketclosedexception, not a plain ioe");
		}
		assertTrue(System.currentTimeMillis() - oldTime >= testDelay);
	}

	public void testAcceptLoop() throws IOException {
		int COUNT = 50;
		for (int i = 0; i < 50; i++) {
			testAccept();
		}
		System.out.println("performed " + COUNT + " socket/bind/listen/accept/close correctly.");
	}
}
