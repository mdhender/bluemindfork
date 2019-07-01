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
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.platform.Verticle;

public class SimpleTestVerticle extends Verticle {

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
		httpServer.setTrafficClass(3);
		httpServer.requestHandler(new Handler<HttpServerRequest>() {

			@Override
			public void handle(HttpServerRequest r) {
				r.response().sendFile(p);

			}
		}).listen(8081);
	}
}
