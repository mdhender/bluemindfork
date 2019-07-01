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
package net.bluemind.dav.server.proto.move;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;

import net.bluemind.dav.server.proto.IDavProtocol;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.vertx.common.Body;

public class MoveProtocol implements IDavProtocol<MoveQuery, MoveResponse> {

	private static final Logger logger = LoggerFactory.getLogger(MoveProtocol.class);

	@Override
	public void parse(HttpServerRequest r, final DavResource davRes, final Handler<MoveQuery> handler) {
		Body.handle(r, new Handler<Buffer>() {

			@Override
			public void handle(Buffer event) {
				logReq(r, event);
				handler.handle(new MoveQuery(davRes));
			}
		});

	}

	@Override
	public void execute(LoggedCore lc, MoveQuery query, Handler<MoveResponse> handler) {
		handler.handle(new MoveResponse());
	}

	@Override
	public void write(MoveResponse response, HttpServerResponse sr) {
		logger.error("Not implemented");
		sr.setStatusCode(403).setStatusMessage("Not implemented").end();
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

}
