/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.lib.vertx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;
import io.vertx.ext.web.handler.sockjs.SockJSSocket;

public class RouteMatcher implements Handler<HttpServerRequest> {
	private static final Logger logger = LoggerFactory.getLogger(RouteMatcher.class);

	private final Router router;
	private final Vertx vertx;

	public RouteMatcher(Vertx vx) {
		this.vertx = vx;
		this.router = Router.router(vx);
	}

	private static HttpServerRequest unwrapVertxWeb(HttpServerRequest req) {
		// we known the request is not paused and is an
		// io.vertx.ext.web.impl.HttpServerRequestWrapper
		// the pause method has a side effect that unwraps the delegate :)
		return req.pause().resume();
	}

	public void regex(HttpMethod m, String re, Handler<HttpServerRequest> h) {
		router.routeWithRegex(m, re).handler(rc -> h.handle(unwrapVertxWeb(rc.request())));
	}

	public void regex(String re, Handler<HttpServerRequest> h) {
		router.routeWithRegex(re).handler(rc -> h.handle(unwrapVertxWeb(rc.request())));
	}

	public void post(String path, Handler<HttpServerRequest> h) {
		router.post(path).handler(rc -> h.handle(unwrapVertxWeb(rc.request()).pause().resume()));
	}

	public void get(String path, Handler<HttpServerRequest> h) {
		router.get(path).handler(rc -> h.handle(unwrapVertxWeb(rc.request())));
	}

	public void allWithRegEx(String regex, Handler<HttpServerRequest> h) {
		router.routeWithRegex(regex).handler(rc -> h.handle(unwrapVertxWeb(rc.request())));
	}

	@Override
	public void handle(HttpServerRequest fRequest) {
		router.handle(fRequest);
	}

	public void noMatch(Handler<HttpServerRequest> h) {
		router.route().order(Integer.MAX_VALUE).handler(rc -> h.handle(unwrapVertxWeb(rc.request())));
	}

	public SockJSHandler websocket(String prefix, SockJSHandlerOptions hOpts, Handler<SockJSSocket> sock) {
		// add a default body handler needed for XHR POST requests
		BodyHandler defaultBodyHandler = BodyHandler.create();
		router.post(prefix + "/*").handler(ctx -> {
			defaultBodyHandler.handle(ctx);
			logger.info("XHR POST endpoint:{} headers:{}", ctx.request().absoluteURI(), ctx.request().headers());
		});
		SockJSHandler handler = SockJSHandler.create(vertx, hOpts);
		router.route(prefix + "/*").subRouter(handler.socketHandler(sock));
		return handler;
	}

	public void delete(String path, Handler<HttpServerRequest> handler) {
		router.delete(path).handler(rc -> handler.handle(unwrapVertxWeb(rc.request())));
	}

	public void put(String path, Handler<HttpServerRequest> handler) {
		router.put(path).handler(rc -> handler.handle(unwrapVertxWeb(rc.request())));
	}

	public void head(String path, Handler<HttpServerRequest> handler) {
		router.head(path).handler(rc -> handler.handle(unwrapVertxWeb(rc.request())));
	}

	public void options(String path, Handler<HttpServerRequest> handler) {
		router.options(path).handler(rc -> handler.handle(unwrapVertxWeb(rc.request())));
	}

}
