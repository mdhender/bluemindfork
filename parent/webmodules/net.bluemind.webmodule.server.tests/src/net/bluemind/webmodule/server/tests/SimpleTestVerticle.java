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
package net.bluemind.webmodule.server.tests;

import java.io.IOException;

import org.eclipse.core.runtime.FileLocator;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;

public class SimpleTestVerticle extends AbstractVerticle {

	@Override
	public void start() {

		String path = null;
		try {
			path = FileLocator.resolve(Activator.context.getBundle().getResource("web-resources/statics/test.html"))
					.getFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		String p = path;
		System.out.println(p);

		HttpServer httpServer = vertx.createHttpServer();
		httpServer.requestHandler(new Handler<HttpServerRequest>() {

			@Override
			public void handle(HttpServerRequest r) {
				r.response().sendFile(p, res -> {
					if (res.failed()) {
						r.response().setStatusCode(404).end();
					}
				});

			}
		}).listen(8081);
	}
}
