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

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
		Map h = body.getJsonObject("headers").getMap();
		Map p = body.getJsonObject("params").getMap();
		MultiMap params = MultiMap.caseInsensitiveMultiMap();
		params.addAll(p);

		MultiMap headers = MultiMap.caseInsensitiveMultiMap();
		headers.addAll(h);
		byte[] br = body.getBinary("body");
		Buffer b = null;
		if (br != null) {
			b = Buffer.buffer(br);
		}
		RestRequest r = RestRequest.create(null, HttpMethod.valueOf(body.getString("verb")), headers,
				body.getString("path"), params, b, null);

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
			ret.put("data", value.data.getBytes());
		}

		JsonObject headers = new JsonObject();
		for (Entry<String, String> entry : value.headers.entries()) {
			JsonArray values = headers.getJsonArray(entry.getKey());
			if (values == null) {
				values = new JsonArray();
				headers.put(entry.getKey(), values);
			}
			values.add(entry.getValue());
		}

		ret.put("headers", headers);
		ret.put("statusCode", value.statusCode);
		return ret;
	}

}
