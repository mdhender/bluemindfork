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

import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.CaseInsensitiveMultiMap;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.rest.base.RestRequest;
import net.bluemind.core.rest.base.RestResponse;
import net.bluemind.core.rest.base.RestRootHandler;

public class RestJsonVertxRootHandler implements Handler<Message<JsonObject>> {

	private static final Logger logger = LoggerFactory.getLogger(RestJsonVertxRootHandler.class);
	private Vertx vertx;
	private RestRootHandler rootHandler;

	public RestJsonVertxRootHandler(Vertx vertx, RestRootHandler rootHandler) {
		this.vertx = vertx;
		this.rootHandler = rootHandler;
	}

	@Override
	public void handle(Message<JsonObject> request) {
		JsonObject body = request.body();
		Map h = body.getObject("headers").toMap();
		Map p = body.getObject("params").toMap();
		CaseInsensitiveMultiMap params = new CaseInsensitiveMultiMap();
		params.add(p);

		CaseInsensitiveMultiMap headers = new CaseInsensitiveMultiMap();
		headers.add(h);
		byte[] br = body.getBinary("body");
		Buffer b = null;
		if (br != null) {
			b = new Buffer(br);
		}
		RestRequest r = RestRequest.create(null, body.getString("verb"), headers, body.getString("path"), params, b,
				null);

		rootHandler.call(r, new AsyncHandler<RestResponse>() {

			@Override
			public void success(RestResponse value) {
				logger.debug("reply {}", value);
				request.reply(buildResponse(value));

			}

			@Override
			public void failure(Throwable e) {
				logger.debug("reply error ", e);
				request.reply(buildFailure(e));
			}

		});
	}

	protected JsonObject buildFailure(Throwable e) {
		return new JsonObject();
	}

	protected JsonObject buildResponse(RestResponse value) {
		JsonObject ret = new JsonObject();
		if (value.data != null) {
			ret.putBinary("data", value.data.getBytes());
		}

		JsonObject headers = new JsonObject();
		for (Entry<String, String> entry : value.headers.entries()) {
			JsonArray values = headers.getArray(entry.getKey());
			if (values == null) {
				values = new JsonArray();
				headers.putArray(entry.getKey(), values);
			}
			values.add(entry.getValue());
		}

		ret.putObject("headers", headers);
		ret.putNumber("statusCode", value.statusCode);
		return ret;
	}

}
