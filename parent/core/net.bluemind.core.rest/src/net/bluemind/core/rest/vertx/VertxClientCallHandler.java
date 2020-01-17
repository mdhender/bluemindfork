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
package net.bluemind.core.rest.vertx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.rest.base.IRestCallHandler;
import net.bluemind.core.rest.base.RestRequest;
import net.bluemind.core.rest.base.RestResponse;

public class VertxClientCallHandler implements IRestCallHandler {

	static final Logger logger = LoggerFactory.getLogger(VertxClientCallHandler.class);
	private Vertx vertx;

	public VertxClientCallHandler(Vertx vertx) {
		this.vertx = vertx;
	}

	@Override
	public void call(final RestRequest request, final AsyncHandler<RestResponse> responseHandler) {
		VertxRestRequest vrr = VertxRestRequest.create(request);

		if (vrr.bodyStreamAdr != null) {
			VertxStreamProducer.stream(vertx, vrr.bodyStreamAdr, request.bodyStream);

		} else {
			logger.debug("no body stream");
		}

		vertx.eventBus().request("bm-core", vrr, new Handler<AsyncResult<Message<VertxRestResponse>>>() {

			@Override
			public void handle(AsyncResult<Message<VertxRestResponse>> msg) {
				if (msg.succeeded()) {
					VertxRestResponse resp = msg.result().body();
					if (resp.responseStreamAdr != null) {
						responseHandler
								.success(RestResponse.stream(new VertxStreamConsumer(vertx, resp.responseStreamAdr)));
					} else {
						responseHandler.success(resp.asResponse());
					}
				} else {
					responseHandler.failure(msg.cause());
				}
			}
		});
	}

}
