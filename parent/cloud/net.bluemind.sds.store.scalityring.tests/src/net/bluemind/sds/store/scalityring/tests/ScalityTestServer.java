/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.sds.store.scalityring.tests;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.core.rest.base.GenericStream.AccumulatorStream;
import net.bluemind.core.rest.vertx.BufferReadStream;
import net.bluemind.lib.vertx.RouteMatcher;
import net.bluemind.lib.vertx.VertxPlatform;

public class ScalityTestServer extends AbstractVerticle {
	public static class PreparedResponse {
		Buffer data;
		Map<String, String> headers;
		int code;

		public PreparedResponse(int code) {
			this(code, null);
		}

		public PreparedResponse(int code, Buffer data) {
			this(code, data, Collections.emptyMap());
		}

		public PreparedResponse(int code, Buffer data, Map<String, String> headers) {
			this.headers = headers;
			this.code = code;
			this.data = data;
		}
	}

	private PreparedResponse nextResponse;
	private final ConcurrentHashMap<String, Buffer> memoryFs = new ConcurrentHashMap<>();
	private final HttpServer server;
	private final int port = 4552;
	private final String prefix = "sproxy";

	public ScalityTestServer() {
		Vertx vertx = VertxPlatform.getVertx();
		this.server = vertx.createHttpServer();
		RouteMatcher router = new RouteMatcher(vertx);
		router.noMatch(req -> {
			req.response().setStatusCode(404).end();
		});
		router.head("/" + prefix + "/:name", this::head);
		router.put("/" + prefix + "/:name", this::put);
		router.get("/" + prefix + "/:name", this::get);
		router.delete("/" + prefix + "/:name", this::delete);
		server.requestHandler(router);
	}

	@Override
	public void start(Promise<Void> p) throws Exception {
		server.listen(port, ar -> {
			if (ar.succeeded()) {
				p.complete();
			} else {
				p.fail(ar.cause());
			}
		});
	}

	@Override
	public void stop(Promise<Void> stopPromise) throws Exception {
		server.close(ar -> {
			if (ar.succeeded()) {
				stopPromise.complete();
			} else {
				stopPromise.fail(ar.cause());
			}
		});
	}

	public void setNextResponse(PreparedResponse response) {
		this.nextResponse = response;
	}

	private boolean sendNextResponse(HttpServerResponse resp) {
		if (nextResponse == null) {
			return false;
		}
		resp.setStatusCode(nextResponse.code);
		for (Entry<String, String> hdr : nextResponse.headers.entrySet()) {
			resp.putHeader(hdr.getKey(), hdr.getValue());
		}
		if (nextResponse.data != null) {
			sendData(resp, nextResponse.data);
		} else {
			resp.end();
		}
		return true;
	}

	private void sendData(HttpServerResponse resp, Buffer data) {
		resp.setStatusCode(200);
		resp.setChunked(true);
		BufferReadStream bufstream = new BufferReadStream(data);
		bufstream.pipeTo(resp);
	}

	private void head(final HttpServerRequest event) {
		HttpServerResponse resp = event.response();
		if (!sendNextResponse(resp)) {
			Buffer data = memoryFs.get(event.getParam("name"));
			if (data == null) {
				resp.setStatusCode(404);
			} else {
				resp.setStatusCode(200);
			}
			resp.end();
		}
	}

	private void get(final HttpServerRequest event) {
		HttpServerResponse resp = event.response();
		if (!sendNextResponse(resp)) {
			Buffer data = memoryFs.get(event.getParam("name"));
			if (data == null) {
				resp.setStatusCode(404);
				resp.end();
			} else {
				sendData(resp, data);
			}
		}
	}

	private void put(final HttpServerRequest event) {
		HttpServerResponse resp = event.response();
		if (!sendNextResponse(resp)) {
			AccumulatorStream writer = new AccumulatorStream();
			event.pipeTo(writer, ar -> {
				if (ar.succeeded()) {
					Buffer data = writer.buffer().copy();
					System.err.println("PUT " + event.getParam("name") + " " + data.length() + " bytes.");

					memoryFs.put(event.getParam("name"), data);
					resp.setStatusCode(200);
					resp.end();
				} else {
					resp.setStatusMessage("failed to upload:" + ar.cause());
					resp.setStatusCode(500);
					resp.end();
				}
			});
		}
	}

	private void delete(final HttpServerRequest event) {
		HttpServerResponse resp = event.response();
		if (!sendNextResponse(resp)) {
			Buffer data = memoryFs.get(event.getParam("name"));
			if (data == null) {
				resp.setStatusCode(404);
			} else {
				memoryFs.remove(event.getParam("name"));
				resp.setStatusCode(200);
			}
			resp.end();
		}
	}
}
