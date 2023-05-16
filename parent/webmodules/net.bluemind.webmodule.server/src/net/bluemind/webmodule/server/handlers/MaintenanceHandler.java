/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.webmodule.server.handlers;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.system.api.SystemState;
import net.bluemind.webmodule.server.handlers.internal.CoreStateListener;

public class MaintenanceHandler {
	private static final Logger logger = LoggerFactory.getLogger(MaintenanceHandler.class);

	public Optional<CompletableFuture<HttpServerRequest>> handle(HttpServerRequest request) {
		if (request.path().startsWith("/setup/")) {
			return Optional.of(CompletableFuture.completedFuture(request));
		} else if (notInstalled()) {
			return Optional.of(setupWizard(request));
		} else if (needUpgrade()) {
			return Optional.of(maintenance(request));
		} else if (notRunning() || !ok()) {
			return Optional.of(error(request));
		}

		return Optional.empty();
	}

	private boolean ok() {
		return CoreStateListener.state == SystemState.CORE_STATE_RUNNING;
	}

	private boolean notRunning() {
		return CoreStateListener.state == SystemState.CORE_STATE_UNKNOWN;
	}

	private boolean needUpgrade() {
		return CoreStateListener.state == SystemState.CORE_STATE_UPGRADE;
	}

	private boolean notInstalled() {
		return CoreStateListener.state == SystemState.CORE_STATE_NOT_INSTALLED;
	}

	private CompletableFuture<HttpServerRequest> setupWizard(HttpServerRequest req) {
		logger.info("Redirect to SetupWizard");
		HttpServerResponse resp = req.response();
		resp.headers().add(HttpHeaders.LOCATION, "/setup/index.html");
		resp.setStatusCode(302);
		resp.end();
		return CompletableFuture.completedFuture(null);

	}

	private CompletableFuture<HttpServerRequest> maintenance(HttpServerRequest req) {
		logger.info("Redirect to maintenance page");
		HttpServerResponse resp = req.response();
		resp.setStatusCode(503);
		resp.end();
		return CompletableFuture.completedFuture(null);
	}

	private CompletableFuture<HttpServerRequest> error(HttpServerRequest req) {
		logger.info("Core is not running");
		HttpServerResponse resp = req.response();
		resp.setStatusCode(502);
		resp.end();
		return CompletableFuture.completedFuture(null);
	}
}
