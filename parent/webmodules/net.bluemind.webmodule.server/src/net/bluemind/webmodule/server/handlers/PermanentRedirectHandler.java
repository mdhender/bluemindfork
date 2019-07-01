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
package net.bluemind.webmodule.server.handlers;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

public class PermanentRedirectHandler implements Handler<HttpServerRequest> {

	private String to;

	public PermanentRedirectHandler(String to) {
		this.to = to;

	}

	@Override
	public void handle(HttpServerRequest req) {
		// permanent redirect to index
		req.response().headers().add("Location", to);
		req.response().setStatusCode(301);
		req.response().end();
	}

}
