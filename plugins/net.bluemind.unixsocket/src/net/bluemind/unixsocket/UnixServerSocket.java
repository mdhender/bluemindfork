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
package net.bluemind.unixsocket;

import java.io.IOException;

import net.bluemind.unixsocket.impl.SocketAPI;

public class UnixServerSocket {

	private static final int ACCEPT_COUNT = 5;

	private int fd;
	private SocketAPI impl;
	private boolean listening;
	private boolean accept;
	private boolean closing;
	private String path;

	public UnixServerSocket(String path) throws IOException {
		this.path = path;
		this.impl = new SocketAPI();
		this.listening = false;
		this.closing = false;
		this.accept = false;
		try {
			fd = impl.allocateSocket();
			impl.bindToPath(fd, path);
		} catch (IOException e) {
			if (fd > 0) {
				close();
				throw e;
			}
		}
	}

	/**
	 * This method will block until a connection is done on the socket
	 * 
	 * @return a socket channel when someones connect
	 * @throws IOException
	 */
	public UnixDomainSocketChannel accept() throws IOException {
		if (!listening) {
			impl.listen(fd, ACCEPT_COUNT);
			listening = true;
		}
		accept = true;
		UnixDomainSocketChannel channel = null;
		try {
			channel = impl.accept(fd);
		} finally {
			accept = false;
		}
		if (closing) {
			if (channel != null) {
				channel.close();
			}
			throw new SocketClosedException("socket was closed (by user demand)");
		}
		return channel;
	}

	/**
	 * This will close the socket. If accept() is running, it will make a dummy
	 * connection to trigger a wake up, before closing everything.
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		closing = true;
		if (accept) {
			// make dummy connect to make accept() wake up
			UnixClientSocket waker = new UnixClientSocket(path);
			waker.connect();
			waker.close();
		}
		impl.close(fd);
	}

}
