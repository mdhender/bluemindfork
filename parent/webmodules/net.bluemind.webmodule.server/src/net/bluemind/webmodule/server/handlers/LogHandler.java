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
package net.bluemind.webmodule.server.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.json.JsonObject;

import com.netflix.spectator.api.Registry;

import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

public class LogHandler implements Handler<HttpServerRequest> {
	private static final Logger logger = LoggerFactory.getLogger(LogHandler.class);

	private final Registry registry = MetricsRegistry.get();
	private final IdFactory idFactory = new IdFactory("jsErrors", registry, LogHandler.class);

	@Override
	public void handle(final HttpServerRequest request) {
		request.expectMultiPart(true);
		request.endHandler(new Handler<Void>() {

			@Override
			public void handle(Void buff) {

				HttpServerResponse resp = request.response();
				try {
					handleLog(request);
				} catch (Exception t) {
					logger.error(t.getMessage(), t);
				}
				resp.setStatusCode(200);
				resp.end();

			}
		});
	}

	protected void handleLog(HttpServerRequest request) {
		MultiMap formAttributes = request.formAttributes();

		String login = request.headers().get("BMUserLATD");

		String msg = formAttributes.get("message");

		String name = formAttributes.get("name");

		logger.error("[" + login + "] [" + name + "] " + msg);
		registry.counter(idFactory.name("logReceived")).increment();

		String exception = formAttributes.get("exception");
		if (exception != null && !"undefined".equals(exception) && !"".equals(exception)) {
			JsonObject js = new JsonObject(exception);

			logger.error("[" + login + "] [" + name + "] " + js.getString("name") + ": " + js.getString("message")
					+ " (" + js.getString("filename") + ":" + js.getValue("line") + ")");
			logger.error("[" + login + "] [" + name + "] " + js.getString("stack"));

		}
	}
}
