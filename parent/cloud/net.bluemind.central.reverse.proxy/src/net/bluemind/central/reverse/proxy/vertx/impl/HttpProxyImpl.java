/*
 * Copyright (c) 2011-2020 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package net.bluemind.central.reverse.proxy.vertx.impl;

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import net.bluemind.central.reverse.proxy.vertx.Body;
import net.bluemind.central.reverse.proxy.vertx.HttpProxy;
import net.bluemind.central.reverse.proxy.vertx.HttpServerRequestContext;
import net.bluemind.central.reverse.proxy.vertx.ProxyOptions;
import net.bluemind.central.reverse.proxy.vertx.ProxyRequest;
import net.bluemind.central.reverse.proxy.vertx.ProxyResponse;

public class HttpProxyImpl implements HttpProxy {

	private final HttpClient client;
	private final boolean supportWebSocket;
	private Function<HttpServerRequestContext, Future<SocketAddress>> selector = req -> Future
			.failedFuture("No origin available");
	private BiConsumer<HttpServerRequestContext, ProxyResponse> responseHook;
	private Logger logger = LoggerFactory.getLogger(HttpProxyImpl.class);

	public HttpProxyImpl(ProxyOptions options, HttpClient client) {
		this.client = client;
		this.supportWebSocket = options.getSupportWebSocket();
	}

	@Override
	public HttpProxy originSelector(Function<HttpServerRequestContext, Future<SocketAddress>> selector) {
		this.selector = selector;
		return this;
	}

	@Override
	public HttpProxy responseHook(BiConsumer<HttpServerRequestContext, ProxyResponse> responseHook) {
		this.responseHook = responseHook;
		return this;
	}

	@Override
	public void handle(HttpServerRequest outboundRequest) {
		ProxyRequest proxyRequest = ProxyRequest.reverseProxy(outboundRequest);

		// Encoding sanity check
		Boolean chunked = HttpUtils.isChunked(outboundRequest.headers());
		if (chunked == null) {
			end(proxyRequest, 400);
			return;
		}

		// WebSocket upgrade tunneling
		if (supportWebSocket && outboundRequest.version() == HttpVersion.HTTP_1_1
				&& outboundRequest.method() == HttpMethod.GET
				&& outboundRequest.headers().contains(HttpHeaders.CONNECTION, HttpHeaders.UPGRADE, true)) {
			handleWebSocketUpgrade(proxyRequest);
			return;
		}

		ProxyContext bh = new Proxy();
		bh.handleProxyRequest(proxyRequest, ar -> {
			if (ar.failed()) {
				logger.error("handle request error: {}", ar.cause().getMessage());
			}
		});
	}

	private void handleWebSocketUpgrade(ProxyRequest proxyRequest) {
		HttpServerRequest outboundRequest = proxyRequest.outboundRequest();
		HttpServerRequestContext context = new HttpServerRequestContextImpl(outboundRequest);
		resolveOrigin(context).onComplete(ar -> {
			if (ar.succeeded()) {
				HttpClientRequest inboundRequest = ar.result();
				inboundRequest.setMethod(HttpMethod.GET);
				inboundRequest.setURI(outboundRequest.uri());
				inboundRequest.headers().addAll(outboundRequest.headers());
				Future<HttpClientResponse> fut2 = inboundRequest.connect();
				outboundRequest.handler(inboundRequest::write);
				outboundRequest.endHandler(v -> inboundRequest.end());
				outboundRequest.resume();
				fut2.onComplete(ar2 -> {
					if (ar2.succeeded()) {
						HttpClientResponse inboundResponse = ar2.result();
						if (inboundResponse.statusCode() == 101) {
							HttpServerResponse outboundResponse = outboundRequest.response();
							outboundResponse.setStatusCode(101);
							outboundResponse.headers().addAll(inboundResponse.headers());
							Future<NetSocket> otherso = outboundRequest.toNetSocket();
							otherso.onSuccess(outboundSocket -> {
								NetSocket inboundSocket = inboundResponse.netSocket();
								outboundSocket.handler(inboundSocket::write);
								inboundSocket.handler(outboundSocket::write);
								outboundSocket.closeHandler(v -> inboundSocket.close());
								inboundSocket.closeHandler(v -> outboundSocket.close());
							}).onFailure(t -> logger.error(
									"unknown error while trying to convert outboundRequest toNetSocket(): {}",
									t.getMessage(), t));
						} else {
							// Rejection
							outboundRequest.resume();
							end(proxyRequest, inboundResponse.statusCode());
						}
					} else {
						outboundRequest.resume();
						end(proxyRequest, 502);
					}
				});
			} else {
				outboundRequest.resume();
				end(proxyRequest, 502);
			}
		});
	}

	private Future<HttpClientRequest> resolveOrigin(HttpServerRequestContext context) {
		return selector.apply(context).flatMap(server -> {
			RequestOptions requestOptions = new RequestOptions();
			requestOptions.setServer(server);
			return client.request(requestOptions);
		});
	}

	private void end(ProxyRequest proxyRequest, int sc) {
		proxyRequest.response().release().setStatusCode(sc).putHeader(HttpHeaders.CONTENT_LENGTH, "0").setBody(null)
				.send();

	}

	private interface ProxyContext {

		void handleProxyRequest(ProxyRequest request, Handler<AsyncResult<Void>> handler);

		void handleProxyResponse(ProxyResponse response, Handler<AsyncResult<Void>> handler);

	}

	private class Proxy implements ProxyContext {

		private ProxyContext context = this;

		@Override
		public void handleProxyRequest(ProxyRequest request, Handler<AsyncResult<Void>> handler) {
			HttpServerRequestContext context = new HttpServerRequestContextImpl(request.outboundRequest());
			sendProxyRequest(context, request, ar -> {
				if (ar.succeeded()) {
					responseHook.accept(context, ar.result());
					sendProxyResponse(ar.result(), ar2 -> {
					});
					handler.handle(Future.succeededFuture());
				} else {
					handler.handle(Future.failedFuture(ar.cause()));
				}
			});
		}

		private void sendProxyRequest(HttpServerRequestContext context, ProxyRequest proxyRequest,
				Handler<AsyncResult<ProxyResponse>> handler) {
			Future<HttpClientRequest> f = resolveOrigin(context);
			f.onComplete(ar -> {
				if (ar.succeeded()) {
					proxyRequest.setBody(Body.body(context.bodyStream()));
					sendProxyRequest(proxyRequest, ar.result(), handler);
				} else {
					HttpServerRequest outboundRequest = proxyRequest.outboundRequest();
					outboundRequest.resume();
					Promise<Void> promise = Promise.promise();
					outboundRequest.exceptionHandler(promise::tryFail);
					outboundRequest.endHandler(promise::tryComplete);
					promise.future().onComplete(ar2 -> {
						end(proxyRequest, 502);
					});
					handler.handle(Future.failedFuture(ar.cause()));
				}
			});
		}

		private void sendProxyRequest(ProxyRequest proxyRequest, HttpClientRequest inboundRequest,
				Handler<AsyncResult<ProxyResponse>> handler) {
			((ProxyRequestImpl) proxyRequest).send(inboundRequest, ar2 -> {
				if (ar2.succeeded()) {
					handler.handle(ar2);
				} else {
					proxyRequest.outboundRequest().response().setStatusCode(502).end();
					handler.handle(Future.failedFuture(ar2.cause()));
				}
			});
		}

		private void sendProxyResponse(ProxyResponse response, Handler<AsyncResult<Void>> handler) {

			// Check validity
			Boolean chunked = HttpUtils.isChunked(response.headers());
			if (chunked == null) {
				// response.request().release(); // Is it needed ???
				end(response.request(), 501);
				handler.handle(Future.succeededFuture()); // should use END future here
				return;
			}

			context.handleProxyResponse(response, handler);
		}

		@Override
		public void handleProxyResponse(ProxyResponse response, Handler<AsyncResult<Void>> handler) {
			((ProxyResponseImpl) response).send(handler);
		}
	}
}
