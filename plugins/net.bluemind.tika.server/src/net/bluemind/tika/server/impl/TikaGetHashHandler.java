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

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

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
			AsyncFile asyncFile = vertx.fileSystem().openBlocking(f.getAbsolutePath(), new OpenOptions());
			asyncFile.pipeTo(resp);
		} else {
			resp.setStatusCode(404).end();
		}
	}

}
