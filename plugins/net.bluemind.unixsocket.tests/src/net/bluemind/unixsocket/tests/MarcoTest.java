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
import java.nio.ByteBuffer;

import junit.framework.TestCase;
import net.bluemind.unixsocket.UnixClientSocket;
import net.bluemind.unixsocket.UnixDomainSocketChannel;
import net.bluemind.unixsocket.UnixServerSocket;

public class MarcoTest extends TestCase {

	protected String path = System.getProperty("user.home") + File.separatorChar + "junit_unix.sock";

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testMarcoPolo() throws IOException, InterruptedException {
		final int COUNT = 1000000;

		final UnixServerSocket uss = new UnixServerSocket(path);

		PoloAcceptor pa = new PoloAcceptor(uss);
		Thread t = new Thread(pa);
		t.start();

		Thread.sleep(1000);

		UnixClientSocket ucs = new UnixClientSocket(path);
		UnixDomainSocketChannel channel = ucs.connect();

		ByteBuffer marco = ByteBuffer.wrap("marco".getBytes());
		ByteBuffer polo = ByteBuffer.allocateDirect(4);
		long time = System.currentTimeMillis();
		int received = 0;
		for (int i = 0; i < COUNT; i++) {
			channel.write(marco);
			marco.rewind();
			received += channel.read(polo);
			polo.rewind();
		}
		time = System.currentTimeMillis() - time;
		System.out.println("time: " + time + "ms. (marco.received: " + received + " polo.received: " + pa.getReceived()
				+ " with " + pa.getIterations() + " iterations)");

		ucs.close();
	}
}
