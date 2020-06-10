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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBus;
import net.bluemind.lib.vertx.internal.BMModule;
import net.bluemind.lib.vertx.internal.Result;

public final class VertxPlatform implements BundleActivator {

	private static BundleContext context;

	private static CompletableFuture<Void> future;
	private static String deploymentId;

	private static Vertx vertx;
	private static final Logger logger = LoggerFactory.getLogger(VertxPlatform.class);

	static BundleContext getContext() {
		return context;
	}

	static {
		InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
		System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext )
	 */
	public void start(BundleContext bundleContext) throws Exception {
		if (vertx != null) {
			return;
		}
		logger.info("Starting vertx platform");

		vertx = Vertx.vertx(new VertxOptions().setPreferNativeTransport(true));
		VertxPlatform.context = bundleContext;
	}

	public static synchronized void spawnVerticles(final Handler<AsyncResult<Void>> complete) {
		if (future != null) {
			logger.info("============ VERTICLES ALREADY SPAWNED ({}) =========", deploymentId);
			if (future.isDone()) {
				complete.handle(new Result<Void>());
			} else {
				future.thenAccept(v -> complete.handle(new Result<Void>()));
			}
			return;
		}

		future = new CompletableFuture<>();
		Supplier<Verticle> bmModule = BMModule::new;

		vertx.deployVerticle(bmModule, new DeploymentOptions().setInstances(1), (AsyncResult<String> event) -> {
			logger.info("BMModule deployed, success: {}", event.succeeded());
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
		});
	}

	@SuppressWarnings("serial")
	private static class SpawnException extends RuntimeException {

		public SpawnException(Exception e) {
			super(e);
		}

	}

	public static void spawnBlocking(long t, TimeUnit u) {
		CompletableFuture<Void> v = new CompletableFuture<>();
		spawnVerticles(r -> {
			if (r.succeeded()) {
				v.complete(null);
			} else {
				v.completeExceptionally(r.cause());
			}
		});
		try {
			v.get(t, u);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			Thread.currentThread().interrupt();
			throw new SpawnException(e);
		}
	}

	public static void undeployVerticles(final Handler<AsyncResult<Void>> complete) {
		if (deploymentId == null) {
			complete.handle(new Result<>(new RuntimeException("No deploymentId, you need to spawn verticles first")));
		}
		vertx.undeploy(deploymentId, complete);
	}

	public static Vertx getVertx() {
		return vertx;
	}

	public static long executeBlockingPeriodic(long delay, Handler<Long> handler) {
		return getVertx().setPeriodic(delay, id -> vertx.<Void>executeBlocking(prom -> {
			try {
				handler.handle(id);
			} finally {
				prom.complete(null);
			}
		}, false, res -> {
		}));
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
