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
package net.bluemind.dav.server;

import java.io.File;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.lib.vertx.VertxPlatform;

public class DavApplication implements IApplication {

	private static final Logger logger = LoggerFactory.getLogger(DavApplication.class);

	@Override
	public Object start(IApplicationContext context) throws Exception {
		logger.info("Starting DAV Server...");

		if (DavActivator.devMode) {
			ResourceLeakDetector.setLevel(Level.PARANOID);
			logger.info("Setting leak detector to paranoid.");
		}

		File staticCheck = new File(Proxy.staticDataPath);
		if (staticCheck.exists() && staticCheck.isDirectory()) {
			logger.info("static datadir is fine.");
		} else {
			logger.error("Your " + Proxy.staticDataPath + " data directory is missing/incorrect.");
			System.exit(1);
			return IApplication.EXIT_OK;
		}

		Handler<AsyncResult<Void>> doneHandler = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				Results.check(event);
				logger.info("Deployement done");
			}
		};

		VertxPlatform.spawnVerticles(doneHandler);

		return IApplication.EXIT_OK;
	}

	@Override
	public void stop() {
		logger.info("stopped");
	}

}
