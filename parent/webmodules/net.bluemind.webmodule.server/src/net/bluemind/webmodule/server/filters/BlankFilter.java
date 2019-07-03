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
package net.bluemind.webmodule.server.filters;

import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;

import net.bluemind.webmodule.server.IWebFilter;

public class BlankFilter implements IWebFilter {

	@Override
	public HttpServerRequest filter(HttpServerRequest request) {
		if (request.path().endsWith("blank.html")) {
			HttpServerResponse resp = request.response();
			resp.headers().set("Content-Type", "text/html; charset=UTF-8");
			resp.headers().set("Cache-Control", "no-cache");
			resp.end("<html><body></body></html>");
			return null;
		} else {
			return request;
		}
	}

}
