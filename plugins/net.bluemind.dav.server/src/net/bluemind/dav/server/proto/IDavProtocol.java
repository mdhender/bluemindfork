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
package net.bluemind.dav.server.proto;

import org.slf4j.Logger;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;

public interface IDavProtocol<Q, R> {

	void parse(HttpServerRequest r, DavResource davRes, Handler<Q> handler);

	void execute(LoggedCore lc, Q query, Handler<R> handler);

	void write(R response, HttpServerResponse sr);

	default void logReq(Logger logger, final HttpServerRequest r, Buffer body) {
		for (String hn : r.headers().names()) {
			logger.info("{}: {}", hn, r.headers().get(hn));
		}
		if (body != null) {
			logger.info("parse '{}'\n{}", r.path(), body.toString());
		} else {
			logger.info("parse '{}' q:'{}'", r.path(), r.query());
		}
	}
}
