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
package net.bluemind.dav.server.proto.post;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;

import net.bluemind.dav.server.proto.DavHeaders;
import net.bluemind.dav.server.proto.IDavProtocol;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;

public class PushProtocol implements IDavProtocol<PushQuery, PushResponse> {

	private static final Logger logger = LoggerFactory.getLogger(PushProtocol.class);

	@Override
	public void parse(final HttpServerRequest r, final DavResource davRes, final Handler<PushQuery> handler) {
		r.endHandler(new Handler<Void>() {

			@Override
			public void handle(Void v) {
				logReq(r, null);
				PushQuery pq = new PushQuery(davRes);
				DavHeaders.parse(pq, r.headers());
				handler.handle(pq);
			}
		});
	}

	private void logReq(final HttpServerRequest r, Buffer body) {
		for (String hn : r.headers().names()) {
			logger.info("{}: {}", hn, r.headers().get(hn));
		}
		if (body != null) {
			logger.info("parse '{}'\n{}", r.path(), body.toString());
		} else {
			logger.info("parse '{}' q:'{}'", r.path(), r.query());
		}
	}

	@Override
	public void execute(LoggedCore lc, PushQuery query, Handler<PushResponse> handler) {
		PushResponse pr = new PushResponse();
		handler.handle(pr);
	}

	@Override
	public void write(PushResponse response, HttpServerResponse sr) {
		logger.error("Not implemented, but 200");
		sr.setStatusCode(200).end();
	}
}
