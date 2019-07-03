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
package net.bluemind.ysnp;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.systemd.notify.Startup;
import net.bluemind.unixsocket.UnixServerSocket;
import net.bluemind.ysnp.impl.AuthChainBuilder;

/**
 * "You Shall Not Pass" saslauthd daemon
 * 
 * 
 */
public class YSNPDaemon implements IApplication {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private AuthChainBuilder at;

	@Override
	public Object start(IApplicationContext context) throws Exception {
		logger.info("YSNP daemon starting");

		CountDownLatch cdl = new CountDownLatch(1);
		Handler<AsyncResult<Void>> doneHandler = (AsyncResult<Void> event) -> {
			logger.info("Deployement done");
			cdl.countDown();
		};

		VertxPlatform.spawnVerticles(doneHandler);

		YSNPConfiguration conf = new YSNPConfiguration();

		logger.info("UNIX socket will be created on " + conf.getSocketPath());

		try {
			// let's create the unix socket
			UnixServerSocket socket = new UnixServerSocket(conf.getSocketPath());
			// and handle incoming authenfication requests
			at = new AuthChainBuilder(conf, socket);
			at.start();
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						at.shutdown();
						logger.info("Shutdown complete.");
					} catch (IOException e) {
						logger.error("Problem shutdown auth director", e);
					}
				}
			});
		} catch (IOException ioe) {
			logger.error("could not start daemon: " + ioe.getMessage());
		}
		cdl.await(1, TimeUnit.MINUTES);
		Startup.notifyReady();

		return EXIT_OK;
	}

	@Override
	public void stop() {
		logger.info("YSNP daemon shutdown.");
	}

}
