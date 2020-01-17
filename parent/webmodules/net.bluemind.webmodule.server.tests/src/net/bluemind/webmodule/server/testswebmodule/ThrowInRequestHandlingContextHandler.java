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
package net.bluemind.webmodule.server.testswebmodule;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.webmodule.server.NeedVertx;

public class ThrowInRequestHandlingContextHandler implements Handler<HttpServerRequest>, NeedVertx {

	@Override
	public void handle(HttpServerRequest event) {
		event.bodyHandler(new Handler<Buffer>() {

			@Override
			public void handle(Buffer event) {

				throw new RuntimeException("check that");
			}
		});

	}

	@Override
	public void setVertx(Vertx vertx) {
	}

}
