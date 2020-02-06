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

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

public class SendFile implements Handler<HttpServerRequest> {

	private static final Logger logger = LoggerFactory.getLogger(SendFile.class);

	public SendFile() {
	}

	@Override
	public void handle(final HttpServerRequest event) {
		String path = UrlPath.dec(event.params().get("param0"));
		HttpServerResponse r = event.response();
		File f = new File(path);
		if (f.exists()) {
			logger.info("GET {} => sendfile.", path);
			r.sendFile(f.getAbsolutePath());
		} else {
			logger.warn("GET {} => does not exist.", path);
			r.setStatusCode(404).end();
		}
	}
}
