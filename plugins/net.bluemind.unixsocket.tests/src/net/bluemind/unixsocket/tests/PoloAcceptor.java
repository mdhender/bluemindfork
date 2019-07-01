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

import java.io.IOException;
import java.nio.ByteBuffer;

import net.bluemind.unixsocket.SocketClosedException;
import net.bluemind.unixsocket.UnixDomainSocketChannel;
import net.bluemind.unixsocket.UnixServerSocket;

public class PoloAcceptor implements Runnable {

	private UnixServerSocket uss;
	private int received;
	private int iterations;

	public PoloAcceptor(UnixServerSocket uss) {
		this.uss = uss;
		this.received = 0;
		this.iterations = 0;
	}

	@Override
	public void run() {
		try {

			ByteBuffer rb = ByteBuffer.allocateDirect(5);
			UnixDomainSocketChannel channel = uss.accept();
			ByteBuffer wb = ByteBuffer.allocateDirect(4);
			wb.put("polo".getBytes());
			wb.flip();
			do {
				int read = channel.read(rb);
				if (read == 0) {
					System.out.println("P: nothing read");
					break;
				}
				received += read;
				rb.rewind();
				channel.write(wb);
				wb.rewind();
				iterations++;
			} while (true);
			channel.close();
		} catch (SocketClosedException sce) {
			sce.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		System.out.println("Leaving thread.");
	}

	public int getReceived() {
		return received;
	}

	public int getIterations() {
		return iterations;
	}

}
