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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpServerRequest;

public class ListMatches extends AbstractListFiles {

	private static final Logger logger = LoggerFactory.getLogger(ListMatches.class);

	public ListMatches() {
	}

	@Override
	public void handle(final HttpServerRequest req) {
		String extension = UrlPath.dec(req.params().get("param0"));
		String path = UrlPath.dec(req.params().get("param1"));
		logger.info("MATCH extension {} in {}", extension, path);
		super.handle(path, extension, req);
	}

}
