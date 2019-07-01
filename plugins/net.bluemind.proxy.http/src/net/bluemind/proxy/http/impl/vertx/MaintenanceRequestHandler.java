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
package net.bluemind.proxy.http.impl.vertx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;

import com.netflix.spectator.api.Registry;

import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.proxy.http.HttpProxyServer;

public class MaintenanceRequestHandler implements Handler<HttpServerRequest> {

	private static Logger logger = LoggerFactory.getLogger(MaintenanceRequestHandler.class);
	private static final Registry registry = MetricsRegistry.get();
	private final IdFactory idFactory;
	private CoreState coreState;

	public MaintenanceRequestHandler(CoreState coreState) {
		idFactory = new IdFactory(registry, HttpProxyServer.class);

		this.coreState = coreState;
	}

	@Override
	public void handle(HttpServerRequest req) {
		registry.counter(idFactory.name("requestsCount", "kind", "maintenance")).increment();
		logger.info("current core state on maintenance {}", coreState.state());
		if (coreState.notInstalled()) {
			HttpServerResponse resp = req.response();
			resp.headers().add("Location", "/setup/index.html");
			resp.setStatusCode(302);
			resp.end();
		} else if (coreState.notRunning()) {
			// default error page
			HttpServerResponse resp = req.response();
			resp.setStatusCode(502);
			resp.end();
		} else if (coreState.ok()) {
			HttpServerResponse resp = req.response();
			resp.headers().add("Location", "/");
			resp.setStatusCode(302);
			resp.end();
		} else if (coreState.needUpgrade()) {
			HttpServerResponse resp = req.response();
			resp.headers().add("Location", "/setup/index.html");
			resp.setStatusCode(302);
			resp.end();
		} else {
			HttpServerResponse resp = req.response();
			resp.headers().add("Location", "/login/index.html?maintenance=true");
			resp.setStatusCode(302);
			resp.end();
		}
	}

}
