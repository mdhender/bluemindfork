/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.sds.proxy;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Future;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.sds.proxy.dto.ExistResponse;
import net.bluemind.sds.proxy.dto.JsMapper;
import net.bluemind.sds.proxy.dto.SdsResponse;
import net.bluemind.sds.proxy.events.SdsAddresses;

public class SdsProxyHttpVerticle extends Verticle {

	private static final Logger logger = LoggerFactory.getLogger(SdsProxyHttpVerticle.class);

	public static class SdsProxyHttpFactory implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return false;
		}

		@Override
		public Verticle newInstance() {
			return new SdsProxyHttpVerticle();
		}

	}

	@Override
	public void start(Future<Void> startedResult) {
		HttpServer srv = vertx.createHttpServer();
		RouteMatcher router = new RouteMatcher();
		router.noMatch(req -> {
			logger.warn("Unknown request to {} {}", req.method(), req.absoluteURI());
			req.response().setStatusCode(400).end();
		});
		router.head("/sds", req -> doHead(req));
		router.delete("/sds", req -> doDelete(req));
		router.put("/sds", req -> doPut(req));
		router.get("/sds", req -> doGet(req));

		srv.requestHandler(router).listen(8091, result -> {
			if (result.succeeded()) {
				startedResult.setResult(null);
			} else {
				startedResult.setFailure(result.cause());
			}
		});
	}

	private void doHead(HttpServerRequest req) {
		sendBody(req, SdsAddresses.EXIST, ExistResponse.class,
				resp -> req.response().setStatusCode(resp.exist ? 200 : 404).end());
	}

	private void doDelete(HttpServerRequest req) {
		sendBody(req, SdsAddresses.DELETE, SdsResponse.class, resp -> req.response().setStatusCode(200).end());
	}

	private void doPut(HttpServerRequest req) {
		sendBody(req, SdsAddresses.PUT, SdsResponse.class, resp -> req.response().setStatusCode(200).end());
	}

	private void doGet(HttpServerRequest req) {
		sendBody(req, SdsAddresses.GET, SdsResponse.class, resp -> req.response().setStatusCode(200).end());
	}

	private <T extends SdsResponse> void sendBody(HttpServerRequest req, String address, Class<T> respClass,
			Handler<T> onSuccess) {
		req.bodyHandler(payload -> {
			vertx.eventBus().sendWithTimeout(address, new JsonObject(payload.toString()), 3000,
					(AsyncResult<Message<JsonObject>> res) -> {
						if (res.succeeded()) {
							String jsonString = res.result().body().encode();
							try {
								T objectResp = JsMapper.get().readValue(jsonString, respClass);
								if (objectResp.succeeded()) {
									onSuccess.handle(objectResp);
								} else {
									req.response().setStatusMessage(objectResp.error.message).setStatusCode(500).end();
								}
							} catch (IOException e) {
								logger.error("Error parsing {} response ({})", address, jsonString, e);
								req.response().setStatusCode(500).end();
							}
						} else {
							logger.error("Call over {} failed", address, res.cause());
							req.response().setStatusCode(500).end();
						}
					});
		});

	}

}
