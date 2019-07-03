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
package net.bluemind.tika.server.impl;

import java.io.File;

import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.file.AsyncFile;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.streams.Pump;

public class TikaGetHashHandler implements Handler<HttpServerRequest> {

	private Vertx vertx;

	public TikaGetHashHandler(Vertx vertx) {
		this.vertx = vertx;
	}

	@Override
	public void handle(HttpServerRequest event) {
		String hash = event.params().get("hash");
		final HttpServerResponse resp = event.response();

		File f = HashCache.getIfPresent(hash);
		if (f != null && f.exists()) {
			MultiMap headers = resp.headers();
			headers.add("Content-Type", "text/plain; charset=utf-8");
			headers.add("X-BM-TikaHash", hash);
			resp.setChunked(true);
			AsyncFile asyncFile = vertx.fileSystem().openSync(f.getAbsolutePath());
			Pump pump = Pump.createPump(asyncFile, resp);
			pump.start();
			asyncFile.endHandler(new Handler<Void>() {

				@Override
				public void handle(Void event) {
					resp.end();
				}
			});
		} else {
			resp.setStatusCode(404).end();
		}
	}

}
