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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.WebSocket;
import org.vertx.java.core.json.JsonObject;

public class SockJsProvider {
	private static final Logger logger = LoggerFactory.getLogger(SockJsProvider.class);
	private HttpClient client;
	private List<Handler<WebSocket>> waiters = new ArrayList<>(10);
	private WebSocket ws;
	private volatile boolean connecting;
	private ConcurrentHashMap<String, Handler<JsonObject>> responseHandlers = new ConcurrentHashMap<>();
	private String uri;

	public SockJsProvider(HttpClient client, String uri) {
		this.client = client;
		this.uri = uri;
	}

	public void ws(Handler<WebSocket> handler) {
		if (ws != null) {
			handler.handle(ws);
		} else {
			waiters.add(handler);
			if (!connecting) {
				connecting = true;

				client.connectWebsocket(uri, retSock -> {
					logger.info("Connected to sockjs server");
					ws = retSock;

					ws.dataHandler(buffer -> {
						handleData(buffer);
					});

					waiters.forEach(w -> {
						w.handle(retSock);
					});

					waiters.clear();
				});
			}
		}
	}

	public void registerResponseHandler(String id, Handler<JsonObject> handler) {
		responseHandlers.put(id, handler);
	}

	public void unregisterResponseHandler(String id) {
		responseHandlers.remove(id);
	}

	public void registerHandler(String credentials, String id, Handler<JsonObject> handler) {
		registerResponseHandler(id, handler);
		if (credentials != null) {
			String regJson = String.format(
					"{\"method\":\"register\", \"headers\":{ \"X-BM-ApiKey\":\"%s\"}, \"params\":{}, \"path\":\"%s\"}",
					credentials, id);
			// FIXME control backpressure
			ws.write(new Buffer(regJson));
		}
	}

	public void unregisterHandler(String credentials, String id) {
		unregisterResponseHandler(id);
		String regJson = String.format(
				"{\"method\":\"unregister\", \"headers\":{ \"X-BM-ApiKey\":\"%s\"}, \"params\":{}, \"path\":\"%s\"}",
				credentials, id);
		// FIXME control backpressure
		ws.write(new Buffer(regJson));
	}

	private void handleData(Buffer data) {
		JsonObject r = new JsonObject(data.toString());
		String reqId = r.getString("requestId");
		Handler<JsonObject> handler = responseHandlers.get(reqId);
		if (handler != null) {
			handler.handle(r);
		} else {
			logger.info("no  handler for {} : {}", reqId, r);
		}
	}
}
