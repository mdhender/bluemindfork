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
package net.bluemind.dav.server.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.dav.server.DavActivator;
import net.bluemind.dav.server.store.LoggedCore;

public class ErrorHandler implements Handler<Throwable> {

	private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);
	private HttpServerRequest r;
	private LoggedCore lc;

	public ErrorHandler(LoggedCore lc, HttpServerRequest r) {
		this.r = r;
		this.lc = lc;
	}

	@Override
	public void handle(Throwable event) {
		logger.error(event.getMessage(), event);
		r.response().setStatusCode(403).end();
		if (DavActivator.devMode) {
			System.exit(1);
		}
	}

}
