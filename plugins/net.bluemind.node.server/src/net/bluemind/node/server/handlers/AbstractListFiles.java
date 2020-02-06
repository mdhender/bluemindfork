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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public abstract class AbstractListFiles implements Handler<HttpServerRequest> {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(AbstractListFiles.class);

	public AbstractListFiles() {

	}

	protected void handle(String path, String extension, final HttpServerRequest req) {
		// this does not list content if path is a directory..
		JsonObject jso = new JsonObject();
		JsonArray descs = new JsonArray();
		jso.put("descriptions", descs);
		try {
			File asked = new File(path);
			if (asked.exists()) {
				if (asked.isDirectory()) {
					String[] content = asked.list();
					for (String p : content) {
						if (extension == null || p.endsWith(extension)) {
							descs.add(json(new File(asked, p)));
						}
					}
				} else {
					descs.add(json(asked));
				}
			}
			req.response().end(jso.encode());
		} catch (Exception t) {
			req.response().setStatusCode(404).end();
		}
	}

	private JsonObject json(File fp) {
		JsonObject fdo = new JsonObject();
		fdo.put("path", fp.getAbsolutePath());
		fdo.put("dir", fp.isDirectory());
		if (!fp.isDirectory()) {
			fdo.put("size", fp.length());
		}
		return fdo;
	}
}
