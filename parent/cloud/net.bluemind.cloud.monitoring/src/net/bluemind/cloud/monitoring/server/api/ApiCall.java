/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.cloud.monitoring.server.api;

import java.util.concurrent.CompletableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

public abstract class ApiCall<T> implements Handler<HttpServerRequest> {

	protected abstract String toJson(T data);

	protected void response(HttpServerRequest request, CompletableFuture<T> data) {
		data.thenAccept(payload -> {
			request.response().setStatusCode(200);
			request.response().headers().add("Content-Type", "application/json");
			request.response().end(toJson(payload));
		});
	}

	protected void error(HttpServerRequest request, AsyncResult<?> ret) {
		request.response().write(ret.cause().toString());
		request.response().setStatusCode(500);
		request.response().end();
	}

}
