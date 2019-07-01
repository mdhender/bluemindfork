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
package net.bluemind.ysnp.impl;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.unixsocket.SocketClosedException;
import net.bluemind.unixsocket.UnixDomainSocketChannel;
import net.bluemind.unixsocket.UnixServerSocket;

public class MainLoop extends Thread {

	private static final Logger logger = LoggerFactory.getLogger(MainLoop.class);

	private UnixServerSocket socket;
	private IConnectionListener listener;

	public MainLoop(UnixServerSocket socket, IConnectionListener listener) {
		this.socket = socket;
		this.listener = listener;
	}

	@Override
	public void run() {
		boolean stopped = false;
		while (!stopped) {
			try {
				UnixDomainSocketChannel channel = socket.accept();
				listener.connectionAccepted(channel);
			} catch (SocketClosedException sce) {
				logger.info("gracefull shutdown initiated.");
			} catch (IOException ioe) {
				logger.error("problem accepting connections, leaving main loop", ioe);
				return;
			}
		}
	}

	public void shutdown() throws IOException {
		socket.close();
	}

}
