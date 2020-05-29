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
package net.bluemind.proxy.http;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.lib.vertx.VertxPlatform;

public class HttpProxyServer {

	private static final Logger logger = LoggerFactory.getLogger(HttpProxyServer.class);
	private int port;

	public HttpProxyServer() {
		this.port = 8079;
	}

	public void run() {

		final CountDownLatch cdl = new CountDownLatch(1);
		Handler<AsyncResult<Void>> doneHandler = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				if (event.succeeded()) {
					logger.info("Deployement done. {}", this);
				} else {
					logger.error("Deployement failed.", event.cause());
				}
				cdl.countDown();
			}
		};
		try {
			VertxPlatform.spawnVerticles(doneHandler);
			cdl.await();
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void stop() {
		logger.info("Proxy stopped.");
	}

}
