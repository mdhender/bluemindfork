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
package net.bluemind.node.server.handlers;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.file.AsyncFile;
import org.vertx.java.core.file.FileSystem;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.streams.Pump;

import net.bluemind.lib.vertx.VertxPlatform;

public class WriteFile implements Handler<HttpServerRequest> {

	private static final Logger logger = LoggerFactory.getLogger(WriteFile.class);

	@Override
	public void handle(final HttpServerRequest req) {
		final String path = UrlPath.dec(req.params().get("param0"));
		logger.debug("PUT {}...", path);
		req.pause();
		final FileSystem fs = VertxPlatform.getVertx().fileSystem();
		fs.truncate(path, 0, new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				writeFile(fs, req, path);
			}
		});
	}

	private void writeFile(final FileSystem fs, final HttpServerRequest req, final String path) {
		// we don't really care about truncate result.
		new File(path).getParentFile().mkdirs();
		fs.open(path, ar -> {
			if (!ar.succeeded()) {
				logger.error("[" + path + "]: " + ar.cause().getMessage(), ar.cause());
				req.resume();
				req.response().setStatusCode(500).end();
			} else {
				final AsyncFile af = ar.result();
				final Pump pump = Pump.createPump(req, af);
				req.endHandler(v -> {
					af.flush(fh -> {
						if (!fh.succeeded()) {
							logger.error("Failed to write file {}", path);
							req.response().setStatusCode(500).end();
						} else {
							af.close(event -> {
								if (!event.succeeded()) {
									logger.error("Failed to write file {}", path);
									req.response().setStatusCode(500).end();
								} else {
									logger.info("PUT {} completed, wrote {}bytes.", path, pump.bytesPumped());
									req.response().end();
								}
							});
						}
					});
				});
				req.resume();
				pump.start();
			}
		});
	}
}
