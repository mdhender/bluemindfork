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

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.rest.base.RestRequest;
import net.bluemind.core.rest.base.RestResponse;
import net.bluemind.core.rest.base.RestRootHandler;

public class RestVertxRootHandler implements Handler<Message<VertxRestRequest>> {
	private static final Logger logger = LoggerFactory.getLogger(RestVertxRootHandler.class);
	private Vertx vertx;
	private RestRootHandler rootHandler;

	public RestVertxRootHandler(Vertx vertx, RestRootHandler rootHandler) {
		this.vertx = vertx;
		this.rootHandler = rootHandler;
	}

	@Override
	public void handle(final Message<VertxRestRequest> message) {
		try {
			doCall(message);
		} catch (Exception e) {
			logger.error("error during call", e);
			message.reply(VertxRestResponse.create(RestResponse.fault(e)));
		}
	}

	protected void doCall(final Message<VertxRestRequest> message) {
		VertxRestRequest request = message.body();
		VertxStreamConsumer bodyStream = null;
		if (request.bodyStreamAdr != null) {
			bodyStream = new VertxStreamConsumer(vertx, request.bodyStreamAdr);
		}
		final RestRequest r = request.asRestRequest(bodyStream);

		RestVertxRootHandler.this.rootHandler.call(r, new AsyncHandler<RestResponse>() {

			@Override
			public void success(RestResponse value) {
				VertxRestResponse vr = VertxRestResponse.create(value);
				if (vr.responseStreamAdr != null) {
					VertxStreamProducer.stream(vertx, vr.responseStreamAdr, value.responseStream);
				}
				message.reply(vr);
			}

			@Override
			public void failure(Throwable e) {
				if (r.bodyStream != null) {
					((VertxStreamConsumer) r.bodyStream).fail(e);
				}
				message.reply(VertxRestResponse.create(RestResponse.fault(e)));
			}

		});

	}

}
