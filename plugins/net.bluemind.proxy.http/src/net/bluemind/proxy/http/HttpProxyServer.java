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
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.proxy.http.config.ConfigBuilder;
import net.bluemind.proxy.http.config.HPSConfiguration;
import net.bluemind.proxy.http.reload.ReloadListener;

public class HttpProxyServer {

	private HPSConfiguration conf;
	private static final Logger logger = LoggerFactory.getLogger(HttpProxyServer.class);

	public HttpProxyServer() {
		this.conf = ConfigBuilder.build();
	}

	public void run() {
		final CountDownLatch cdl = new CountDownLatch(1);
		Handler<AsyncResult<Void>> doneHandler = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				logger.info("Deployement done.");
				cdl.countDown();
			}
		};
		try {
			VertxPlatform.spawnVerticles(doneHandler);
			cdl.await();
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}

		ReloadListener rl = new ReloadListener();
		rl.start(VertxPlatform.eventBus());
	}

	public int getPort() {
		return conf.getPort();
	}

	public void setPort(int port) {
		conf.setPort(port);
	}

	public void stop() {
		logger.info("Proxy stopped.");
	}

}
