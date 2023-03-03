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
package net.bluemind.webmodule.maintenancefilter;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.webmodule.maintenancefilter.internal.CoreState;
import net.bluemind.webmodule.server.IWebFilter;
import net.bluemind.webmodule.server.WebserverConfiguration;

public class MaintenanceFilter implements IWebFilter {

	private static final Logger logger = LoggerFactory.getLogger(MaintenanceFilter.class);

	@Override
	public CompletableFuture<HttpServerRequest> filter(HttpServerRequest request, WebserverConfiguration conf) {

		if (request.path().startsWith("/setup/")) {
			return CompletableFuture.completedFuture(request);
		} else if (CoreState.notInstalled() || CoreState.needUpgrade()) {
			return setupWizard(request);
		} else if (CoreState.notRunning()) {
			return error(request);
		} else if (CoreState.ok()) {
			return CompletableFuture.completedFuture(request);
		} else {
			return maintenance(request);
		}
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
		resp.headers().add(HttpHeaders.LOCATION, "/login/index.html?maintenance=true");
		resp.setStatusCode(302);
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
