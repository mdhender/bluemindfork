/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.node.server.handlers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

public class MoveFile implements Handler<HttpServerRequest> {
	private static final Logger logger = LoggerFactory.getLogger(MoveFile.class);

	@Override
	public void handle(final HttpServerRequest req) {
		req.exceptionHandler(t -> do500(t, req));
		req.endHandler(v -> {
			if (!req.response().ended()) {
				req.response().end();
			}
		});
		req.bodyHandler((Buffer body) -> {
			JsonObject jso = new JsonObject(body.toString());
			if (logger.isDebugEnabled()) {
				logger.debug("move request {}", jso.encodePrettily());
			}
			Path src = Paths.get(jso.getString("src"));
			Path dst = Paths.get(jso.getString("dst"));
			Path parentDir = dst.getParent();
			if (!Files.exists(parentDir)) {
				logger.info("mv {} -> {}: parent directory {} does not exists, creating", src, dst, parentDir);
				try {
					Files.createDirectories(parentDir);
				} catch (IOException e) {
					do500(e, req);
					return;
				}
			}
			logger.info("mv {} {}", src, dst);
			try {
				Files.move(src, dst);
			} catch (IOException e) {
				do500(e, req);
			}
		});
	}

	private void do500(Throwable t, HttpServerRequest req) {
		logger.error(t.getMessage(), t);
		req.response().setStatusCode(500).end();
	}
}