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

public class DeleteFile implements Handler<HttpServerRequest> {

	private static final Logger logger = LoggerFactory.getLogger(DeleteFile.class);

	@Override
	public void handle(HttpServerRequest req) {
		final String path = UrlPath.dec(req.params().get("param0"));
		logger.info("DELETE {}...", path);

		File file = new File(path);
		if (!file.exists() || !file.isFile()) {
			logger.warn("Can not delete file {}. File is not present or not a file", path);
		}
		req.response().setStatusCode(file.delete() ? 200 : 500).end();
	}

}
