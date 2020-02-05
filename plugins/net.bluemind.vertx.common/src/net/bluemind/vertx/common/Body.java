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
package net.bluemind.vertx.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;

public final class Body {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(Body.class);

	public static final void handle(HttpServerRequest r, final Handler<Buffer> bh) {
		final Buffer body = Buffer.buffer(1024);

		r.handler(new Handler<Buffer>() {

			@Override
			public void handle(Buffer event) {
				body.appendBuffer(event);
			}

		});
		r.endHandler(new Handler<Void>() {

			@Override
			public void handle(Void event) {
				bh.handle(body);
			}

		});
	}
}
