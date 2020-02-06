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
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.vertx.common.Body;

public class MissingProtocol implements IDavProtocol<UnknownQuery, UnknownResponse> {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private static final UnknownQuery uq = new UnknownQuery();
	private static final UnknownResponse ur = new UnknownResponse();

	private final int errorCode;

	public MissingProtocol() {
		this(501);
	}

	public MissingProtocol(int errorCode) {
		this.errorCode = errorCode;
	}

	@Override
	public void parse(final HttpServerRequest r, DavResource davRes, final Handler<UnknownQuery> handler) {
		Body.handle(r, new Handler<Buffer>() {
			@Override
			public void handle(Buffer b) {
				logReq(r, b);
				handler.handle(uq);
			}
		});
	}

	@Override
	public void execute(LoggedCore lc, UnknownQuery query, Handler<UnknownResponse> handler) {
		handler.handle(ur);
	}

	@Override
	public void write(UnknownResponse response, HttpServerResponse sr) {
		logger.error("Sending error code {}", errorCode);
		sr.setStatusCode(errorCode).setStatusMessage("Not implemented").end();
	}

	private void logReq(HttpServerRequest r, Buffer body) {
		logger.error("{} {}", r.method(), r.path());
		for (String hn : r.headers().names()) {
			logger.error("{}: {}", hn, r.headers().get(hn));
		}
		if (body != null) {
			logger.error("parse '{}'\n{}", r.path(), body.toString());
		} else {
			logger.error("parse '{}' q:'{}'", r.path(), r.query());
		}
	}
}
