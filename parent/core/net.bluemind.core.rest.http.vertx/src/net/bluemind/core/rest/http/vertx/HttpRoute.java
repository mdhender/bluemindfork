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
package net.bluemind.core.rest.http.vertx;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.lib.vertx.RouteMatcher;

public class HttpRoute {

	private static final Logger logger = LoggerFactory.getLogger(HttpRoute.class);
	private Set<String> verbs;
	private String path;
	private Handler<HttpServerRequest> handler;

	public HttpRoute(Handler<HttpServerRequest> handler, String path, Set<String> verbs) {
		this.handler = handler;
		this.path = path;
		this.verbs = verbs;
	}

	public void bind(RouteMatcher routeMatcher) {
		for (String verb : verbs) {
			if (verb.equals("GET")) {
				routeMatcher.get(path, handler);
			} else if (verb.equals("POST")) {
				routeMatcher.post(path, handler);
			} else if (verb.equals("DELETE")) {
				routeMatcher.delete(path, handler);
			} else if (verb.equals("PUT")) {
				routeMatcher.put(path, handler);
			} else {
				logger.warn("unknow verb {}", verb);
			}
		}
	}
}
