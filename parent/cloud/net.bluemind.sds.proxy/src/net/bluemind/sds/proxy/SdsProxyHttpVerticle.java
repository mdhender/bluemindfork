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
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Registry;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.lib.vertx.RouteMatcher;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.sds.proxy.dto.ConfigureResponse;
import net.bluemind.sds.proxy.dto.ExistResponse;
import net.bluemind.sds.proxy.dto.JsMapper;
import net.bluemind.sds.proxy.dto.SdsResponse;
import net.bluemind.sds.proxy.events.SdsAddresses;
import net.bluemind.vertx.common.request.Requests;

public class SdsProxyHttpVerticle extends AbstractVerticle {

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

	private static final Registry registry = MetricsRegistry.get();
	private static final IdFactory idFactory = new IdFactory("http", MetricsRegistry.get(), SdsProxyHttpVerticle.class);

	@Override
	public void start(Promise<Void> startedResult) {

		HttpServer srv = vertx.createHttpServer();
		RouteMatcher router = new RouteMatcher(vertx);
		router.noMatch(req -> {
			logger.warn("Unknown request to {} {}", req.method(), req.absoluteURI());
			req.response().setStatusCode(400).end();
		});
		router.head("/sds", this::exist);
		router.delete("/sds", this::delete);
		router.put("/sds", this::put);
		router.get("/sds", this::get);
		router.post("/configuration", this::configure);
		router.head("/mailbox", this::validateMailbox);

		srv.requestHandler(router).listen(8091, result -> {
			if (result.succeeded()) {
				startedResult.complete(null);
			} else {
				startedResult.fail(result.cause());
			}
		});
	}

	private void configure(HttpServerRequest request) {
		sendBody(request, SdsAddresses.CONFIG, ConfigureResponse.class, (resp, http) -> http.setStatusCode(200).end());
	}

	private void exist(HttpServerRequest request) {
		sendBody(request, SdsAddresses.EXIST, ExistResponse.class,
				(resp, http) -> http.setStatusCode(resp.exists ? 200 : 404).end());
	}

	private void delete(HttpServerRequest req) {
		sendBody(req, SdsAddresses.DELETE, SdsResponse.class, (resp, http) -> http.setStatusCode(200).end());
	}

	private void put(HttpServerRequest req) {
		sendBody(req, SdsAddresses.PUT, SdsResponse.class, (resp, http) -> http.setStatusCode(200).end());
	}

	private void get(HttpServerRequest req) {
		sendBody(req, SdsAddresses.GET, SdsResponse.class, (resp, http) -> http.setStatusCode(200).end());
	}

	private void validateMailbox(HttpServerRequest request) {
		HttpServerRequest req = Requests.wrap(request);
		req.bodyHandler(payload -> {

			vertx.eventBus().request(SdsAddresses.VALIDATION, payload, new DeliveryOptions().setSendTimeout(3000),
					(AsyncResult<Message<Boolean>> result) -> {
						if (result.failed()) {
							logger.info("Unable to get a result ({}), accept by default", result.cause().getMessage());
							req.response().setStatusCode(200).end();
						} else {
							boolean isAccepted = result.result().body();
							if (isAccepted) {
								req.response().setStatusCode(200).end();
							} else {
								logger.warn("Refusing for {}", payload);
								req.response().setStatusCode(403).end();
							}
						}

					});
		});
	}

	private <T extends SdsResponse> void sendBody(HttpServerRequest httpReq, String address, Class<T> respClass,
			BiConsumer<T, HttpServerResponse> onSuccess) {
		long start = registry.clock().monotonicTime();
		HttpServerRequest req = Requests.wrap(httpReq);

		req.bodyHandler(payload -> {
			JsonObject json = new JsonObject(payload.toString().trim().isEmpty() ? "{}" : payload.toString());
			vertx.eventBus().request(address, json, new DeliveryOptions().setSendTimeout(3000),
					(AsyncResult<Message<JsonObject>> res) -> {
						Id timerId = idFactory.name("requestTime")//
								.withTag("method", address)//
								.withTag("status", res.succeeded() ? "OK" : "FAILED");
						registry.timer(timerId).record(registry.clock().monotonicTime() - start, TimeUnit.NANOSECONDS);

						if (res.succeeded()) {
							String jsonString = res.result().body().encode();
							try {
								T objectResp = JsMapper.get().readValue(jsonString, respClass);
								if (objectResp.succeeded()) {
									Requests.tag(req, "method", address);
									onSuccess.accept(objectResp, req.response());
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
