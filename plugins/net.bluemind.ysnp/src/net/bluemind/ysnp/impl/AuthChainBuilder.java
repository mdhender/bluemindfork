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

import net.bluemind.unixsocket.UnixServerSocket;
import net.bluemind.ysnp.YSNPConfiguration;

/**
 * Wires all the daemon components together
 * 
 * 
 */
public class AuthChainBuilder {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private UnixServerSocket socket;
	private MainLoop mainLoop;
	private ConnectionHandler handler;
	private YSNPConfiguration conf;
	private ValidationPolicy vp;

	public AuthChainBuilder(YSNPConfiguration conf, UnixServerSocket socket) {
		this.socket = socket;
		this.conf = conf;
		this.vp = new ValidationPolicy(conf);
	}

	public void start() {
		handler = new ConnectionHandler(vp, conf);
		mainLoop = new MainLoop(socket, handler);
		logger.info("Starting main loop");
		mainLoop.start();
	}

	public void shutdown() throws IOException {
		mainLoop.shutdown();
		handler.shutdown();
	}

}
