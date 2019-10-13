/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.forest.cloud.launcher;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import net.bluemind.core.rest.ServerSideServiceFactories;
import net.bluemind.forest.cloud.hazelcast.HzStarter;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.systemd.notify.SystemD;

public class ForestCloudApplication implements IApplication {

	static {
		// HOLLOW uses JUL...
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();

		// change the port for RestNetVerticle
		System.setProperty("bm.rest.net.verticle.port", "8089");
	}

	private static final Logger logger = LoggerFactory.getLogger(ForestCloudApplication.class);

	@Override
	public Object start(IApplicationContext context) throws Exception {
		HzStarter st = new HzStarter("forest-node", "127.0.1.1");
		CompletableFuture<Object> vxStarted = st.startFuture().thenCompose(hz -> {
			logger.info("HZ is running {}", hz);
			CompletableFuture<Object> start = new CompletableFuture<>();
			VertxPlatform.spawnVerticles(res -> {
				if (res.succeeded()) {
					start.complete(IApplication.EXIT_OK);
				} else {
					start.completeExceptionally(res.cause());
				}
			});
			return start;
		});

		Object ret = vxStarted.get(85, TimeUnit.SECONDS);
		ServerSideServiceFactories factories = ServerSideServiceFactories.getInstance();
		logger.info("Start with {} service(s)", factories.getFactories().size());
		if (SystemD.isAvailable()) {
			SystemD.get().notifyReady();
		}
		return ret;
	}

	@Override
	public void stop() {
		logger.info("stop {}", this);
	}
}
