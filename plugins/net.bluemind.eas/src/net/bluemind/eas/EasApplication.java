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
package net.bluemind.eas;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.systemd.notify.Startup;

public class EasApplication implements IApplication {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public Object start(IApplicationContext context) throws Exception {
		logger.info("EAS Server starting...");
		ListeningExecutorService executor = MoreExecutors.newDirectExecutorService();
		Callable<Void> starter = new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				start();
				return null;
			}
		};
		ListenableFuture<Void> future = executor.submit(starter);
		return future;
	}

	private void start() throws InterruptedException {
		final CountDownLatch cdl = new CountDownLatch(1);
		Handler<AsyncResult<Void>> doneHandler = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				logger.info("EAS vertx deployement complete.");
				cdl.countDown();
				Startup.notifyReady();
			}
		};
		VertxPlatform.spawnVerticles(doneHandler);
		cdl.await();
	}

	@Override
	public void stop() {
	}

}
