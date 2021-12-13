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
package net.bluemind.core.rest.http.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.WebSocket;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.rest.base.IRestCallHandler;
import net.bluemind.core.rest.base.RestRequest;
import net.bluemind.core.rest.base.RestResponse;
import net.bluemind.core.utils.UIDGenerator;

public class VertxSockJsCallHandler implements IRestCallHandler {

	private static final Logger logger = LoggerFactory.getLogger(VertxHttpCallHandler.class);

	private SockJsProvider sockJsProvider;

	public VertxSockJsCallHandler(SockJsProvider provider) {
		this.sockJsProvider = provider;

	}

	@Override
	public void call(RestRequest request, final AsyncHandler<RestResponse> response) {
		sockJsProvider.ws(ws -> {
			if (ws.writeQueueFull()) {
				logger.warn("WS {} queue full", ws);
				ws.drainHandler(v -> doCall(ws, request, response));
			} else {
				doCall(ws, request, response);
			}
		});
	}

	private void doCall(WebSocket ws, RestRequest request, AsyncHandler<RestResponse> response) {
		JsonObject jsRequest = buildRequest(request);
		String id = jsRequest.getString("requestId");
		logger.debug("do call with id {} : {}", id, jsRequest);
		sockJsProvider.registerResponseHandler(id, data -> {
			handleResponse(response, data);
		});
		ws.write(Buffer.buffer(jsRequest.encode()));
	}

	private JsonObject buildRequest(RestRequest request) {
		String rId = UIDGenerator.uid();
		JsonObject msg = new JsonObject();
		msg.put("requestId", rId);
		msg.put("method", request.method.name());
		msg.put("path", request.path);

		JsonObject headers = new JsonObject();
		request.headers.forEach(entry -> {
			headers.put(entry.getKey(), entry.getValue());
		});

		JsonObject params = new JsonObject();
		request.params.forEach(entry -> {
			params.put(entry.getKey(), entry.getValue());
		});
		msg.put("headers", headers);
		msg.put("params", params);

		if (request.body != null) {
			msg.put("body", request.body.getBytes());
		}
		return msg;
	}

	private void handleResponse(AsyncHandler<RestResponse> response, JsonObject data) {
		try {
			sockJsProvider.unregisterHandler(null, data.getString("requestId"));

			RestResponse resp = parseResponse(data);
			response.success(resp);
		} catch (Exception e) {
			response.failure(e);
		}
	}

	private RestResponse parseResponse(JsonObject msg) {

		String requestId = msg.getString("requestId");
		if (requestId == null) {
			throw new IllegalArgumentException("requestId is null");
		}

		MultiMap headers = MultiMap.caseInsensitiveMultiMap();
		headers.addAll(asMap(msg.getJsonObject("headers")));

		Buffer body = null;
		String bodyb = msg.getString("body");
		if (bodyb != null) {
			body = Buffer.buffer(bodyb);
		}

		return RestResponse.ok(headers, msg.getInteger("statusCode"), body);
	}

	private Map<String, String> asMap(JsonObject object) {

		return Optional.of(object).map((o) -> {
			Map<String, String> v = o.getMap().entrySet().stream().collect(Collectors.toMap(a -> {
				return a.getKey();
			}, b -> {
				return (String) b.getValue();
			}));
			return v;
		}).orElse(new HashMap<>());
	}

}
