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
package net.bluemind.locator;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;

import com.google.common.base.Joiner;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.impl.Config;

import net.bluemind.lib.vertx.BlockingCode;
import net.bluemind.locator.impl.LocatorDbHelper;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

/**
 * Locates BM host IP addresses with a service, service_property, login@domain.
 * This call url shoud be /location/host/bm/core/login@domain
 * 
 * 
 */
public class HostLocationHandler implements Handler<HttpServerRequest> {

	private static final Logger logger = LoggerFactory.getLogger(HostLocationHandler.class);
	private final Registry registry;
	private final IdFactory idFactory;
	private final Vertx vertx;
	private final ExecutorService blockingPool;

	public HostLocationHandler(Vertx v, ExecutorService blockingPool) {
		this.vertx = v;
		this.blockingPool = blockingPool;
		ClassLoader configCl = Config.class.getClassLoader();
		ClassLoader contextCl = Thread.currentThread().getContextClassLoader();
		logger.info("************ configCl: {}, contextCl: {}", configCl, contextCl);
		registry = MetricsRegistry.get();
		idFactory = new IdFactory(registry, HostLocationHandler.class);
	}

	@Override
	public void handle(HttpServerRequest req) {
		final long start = registry.clock().monotonicTime();

		MultiMap params = req.params();
		String service = params.get("kind");
		String property = params.get("tag");
		String loginAtDomain = params.get("latd");
		String origin = Optional.ofNullable(req.headers().get("X-Bm-Origin")).orElse("unknown");

		BlockingCode.forVertx(vertx).withExecutor(blockingPool)
				.run(() -> LocatorDbHelper.findUserAssignedHosts(loginAtDomain, service + "/" + property))
				.exceptionally(t -> {
					registry.counter(idFactory.name("requestsCount", "statusCode", "500", "origin", origin))
							.increment();
					req.response().setStatusCode(500);
					req.response().setStatusMessage(t.getMessage() != null ? t.getMessage() : "null");
					req.response().end();
					final long end = registry.clock().monotonicTime();
					registry.timer(idFactory.name("executionTime")).record(end - start, TimeUnit.NANOSECONDS);
					return null;
				}).thenAccept(ips -> {
					final long end = registry.clock().monotonicTime();
					registry.timer(idFactory.name("executionTime")).record(end - start, TimeUnit.NANOSECONDS);
					if (ips == null) {
						// exceptionnaly triggered
					} else if (!ips.isEmpty()) {
						registry.counter(idFactory.name("requestsCount", "statusCode", "200", "origin", origin))
								.increment();
						if (logger.isDebugEnabled()) {
							logger.debug("{} => {}", req.path(), ips);
						}
						req.response().end(Joiner.on('\n').join(ips));
					} else {
						registry.counter(idFactory.name("requestsCount", "statusCode", "404", "origin", origin))
								.increment();
						String error = "Could not find " + service + "/" + property + " for " + loginAtDomain;
						logger.error(error);
						req.response().setStatusCode(404);
						req.response().setStatusMessage(error);
						req.response().end();
					}
				});
	}

}
