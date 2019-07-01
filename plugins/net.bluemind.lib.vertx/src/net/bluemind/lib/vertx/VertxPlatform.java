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
package net.bluemind.lib.vertx;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.PlatformManager;

import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import net.bluemind.lib.vertx.internal.BMPlatformManagerFactory;
import net.bluemind.lib.vertx.internal.Result;

public final class VertxPlatform implements BundleActivator {

	private static BundleContext context;

	private static CompletableFuture<Void> future;
	private static String deploymentId;

	private static PlatformManager pm;
	private static Vertx vertx;
	private static final Logger logger = LoggerFactory.getLogger(VertxPlatform.class);

	static {

	}

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext )
	 */
	public void start(BundleContext bundleContext) throws Exception {
		if (pm != null) {
			return;
		}
		logger.info("Starting vertx platform");

		InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);

		System.setProperty("org.vertx.logger-delegate-factory-class-name",
				"org.vertx.java.core.logging.impl.SLF4JLogDelegateFactory");
		pm = new BMPlatformManagerFactory().createPlatformManager();
		vertx = pm.vertx();
		VertxPlatform.context = bundleContext;
	}

	public static PlatformManager getPlatformManager() {
		return pm;
	}

	public synchronized static void spawnVerticles(final Handler<AsyncResult<Void>> complete) {
		if (future != null) {
			logger.info("============ VERTICLES ALREADY SPAWNED =========");
			if (future.isDone()) {
				complete.handle(new Result<Void>());
			} else {
				future.thenAccept(v -> complete.handle(new Result<Void>()));
			}
			return;
		}

		future = new CompletableFuture<>();
		JsonObject config = new JsonObject();
		pm.deployModuleFromClasspath("net.bluemind~app~3.0.0", config, 1, new URL[0],
				new Handler<AsyncResult<String>>() {

					@Override
					public void handle(AsyncResult<String> event) {
						logger.info("Module deployed, success: {}", event.succeeded());
						if (event.succeeded()) {
							logger.info("Deployement id is {}", event.result());
							deploymentId = event.result();
							complete.handle(new Result<Void>());
							future.complete(null);
						} else {
							logger.error(event.cause().getMessage(), event.cause());
							complete.handle(new Result<Void>(event.cause()));
							future.completeExceptionally(event.cause());
						}
					}
				});
	}

	public static void undeployVerticles(final Handler<AsyncResult<Void>> complete) {
		if (deploymentId == null) {
			complete.handle(new Result<>(new RuntimeException("No deploymentId, you need to spawn verticles first")));
		}
		// pm.undeploy("net.bluemind~app~3.0.0", complete);
		pm.undeploy(deploymentId, complete);
	}

	public static Vertx getVertx() {
		return vertx;
	}

	public static EventBus eventBus() {
		return vertx.eventBus();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		VertxPlatform.context = null;
	}

}
