/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.node.server;

import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.lib.vertx.RouteMatcher;

public class BlueMindUnsecureNode extends BlueMindNode {

	@Override
	protected int getPort() {
		return 8021;
	}

	@Override
	protected void options(HttpServerOptions options) {
		logger.info("Unsecure mode on 8021, node can be claimed");
	}

	@Override
	protected void router(RouteMatcher rm) {
		rm.options("/ping", (HttpServerRequest event) -> {
			if (BlueMindSSLNode.canSSL()) {
				logger.info("Certs are here, time to secure and restart...");
				vertx.setTimer(100, tid -> {
					vertx.eventBus().send("bluemind.node.ssl", null);
				});
				event.response().setStatusCode(201).end();
			} else {
				logger.warn("Ping on unsecure BUT certs are not there yet");
				event.response().setStatusCode(200).end();
			}
		});

	}

}
