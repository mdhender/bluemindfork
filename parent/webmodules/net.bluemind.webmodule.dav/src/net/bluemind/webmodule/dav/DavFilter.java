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
package net.bluemind.webmodule.dav;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpServerRequest;

import net.bluemind.dav.server.DavRouter;
import net.bluemind.webmodule.server.IWebFilter;

public class DavFilter implements IWebFilter {

	private static final Logger logger = LoggerFactory.getLogger(DavFilter.class);
	private final DavRouter davRouter;

	public DavFilter() {
		this.davRouter = new DavRouter();
		logger.info("DAV handler created.");
	}

	@Override
	public HttpServerRequest filter(HttpServerRequest request) {
		String path = request.path();

		if (path.equals(DavRouter.CAL_REDIR) || path.equals(DavRouter.CARD_REDIR) || path.startsWith("/dav")) {
			davRouter.handle(request);
			return null;
		} else {
			return request;
		}
	}

}
