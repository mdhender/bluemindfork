/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.webmodule.forwardfilter;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.RequestOptions;
import net.bluemind.webmodule.server.IWebFilter;
import net.bluemind.webmodule.server.NeedVertx;
import net.bluemind.webmodule.server.SecurityConfig;
import net.bluemind.webmodule.server.WebserverConfiguration;
import net.bluemind.webmodule.server.forward.ForwardedLocation;
import net.bluemind.webmodule.server.forward.ForwardedLocation.ResolvedLoc;

public class ForwardFilter implements IWebFilter, NeedVertx {
	private static final Logger logger = LoggerFactory.getLogger(ForwardFilter.class);

	private Vertx vertx;
	private static Optional<HttpClient> httpClient = Optional.empty();

	@Override
	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}

	@Override
	public CompletableFuture<HttpServerRequest> filter(HttpServerRequest request, WebserverConfiguration conf) {
		Optional<ForwardedLocation> forwardedLocation = conf.getForwardedLocations().stream()
				.filter(fl -> request.path().startsWith(fl.getPathPrefix())).findFirst();

		if (forwardedLocation.isPresent()) {
			Optional<ResolvedLoc> resolved = forwardedLocation.get().resolve();
			if (resolved.isPresent()) {
				return proxify(request, resolved.get());
			}
		}

		return CompletableFuture.completedFuture(request);
	}

	private CompletableFuture<HttpServerRequest> proxify(HttpServerRequest req, ResolvedLoc resolved) {
		final HttpServerResponse resp = req.response();
		req.pause();

		if (logger.isDebugEnabled()) {
			logger.debug("Proxify request URI {} to http://{}:{}{}", req.absoluteURI(), resolved.host, resolved.port,
					req.uri());
		}

		RequestOptions reqOpts = new RequestOptions();
		reqOpts.setHost(resolved.host).setPort(resolved.port);
		reqOpts.setURI(req.uri());
		reqOpts.setMethod(req.method());

		httpClient(vertx).request(reqOpts).onSuccess(upstreamReq -> proxyRequestHandler(req, resp, upstreamReq))
				.onFailure(event -> {
					if (resp.ended()) {
						logger.warn("{} Skipping response ({})", req.uri(), event.getMessage());
						return;
					}
					logger.error("{} {} error: {}", req.method(), req.uri(), event.getMessage(), event);
					String message = Strings.isNullOrEmpty(event.getMessage()) ? "Internal Server Error"
							: event.getMessage();
					resp.setStatusCode(500).setStatusMessage(message).end();
				});

		return CompletableFuture.completedFuture(null);
	}

	private void proxyRequestHandler(HttpServerRequest req, final HttpServerResponse clientResp,
			HttpClientRequest upstreamReq) {
		final AtomicLong writtenToUpstream = new AtomicLong();
		final MultiMap cHeaders = req.headers();

		upstreamReq.setTimeout(30000);
		upstreamReq.headers().setAll(cHeaders.remove("Connection"));

		upstreamReq.response().onSuccess(upstreamResp -> {
			final AtomicLong writtenToClient = new AtomicLong();
			MultiMap upstreamHeaders = upstreamResp.headers();
			addAndSecureUpstreamHeaders(clientResp, upstreamHeaders);
			clientResp.setStatusCode(upstreamResp.statusCode());
			upstreamResp.handler((Buffer data) -> {
				writtenToClient.addAndGet(data.length());
				clientResp.write(data);
				if (clientResp.writeQueueFull()) {
					upstreamResp.pause();
					clientResp.drainHandler(event -> upstreamResp.resume());
				}
			});
			upstreamResp.endHandler(v -> clientResp.end());
		}).onFailure(t -> {
			logger.error("upstream error forwarding {} {} error : {}", req.method(), req.uri(), t.getMessage(), t);
			String message = Strings.isNullOrEmpty(t.getMessage()) ? "Internal Server Error" : t.getMessage();
			clientResp.setStatusCode(500).setStatusMessage(message).end();
		});

		req.handler((Buffer data) -> {
			writtenToUpstream.addAndGet(data.length());
			upstreamReq.write(data);
			if (upstreamReq.writeQueueFull()) {
				req.pause();
				upstreamReq.drainHandler(event -> req.resume());
			}
		});
		req.endHandler(v -> upstreamReq.end(result -> {
			if (result.failed()) {
				logger.error("Forward failure", result.cause());
			}
		}));
		req.exceptionHandler((Throwable event) -> {
			if (clientResp.ended()) {
				logger.warn("Skipping resp for {}", event.getMessage());
				return;
			}
			logger.error("Client req error: {}", event.getMessage(), event);
			clientResp.setStatusCode(500).setStatusMessage("Internal Server Error").end();
		});
		req.resume();
	}

	protected void addAndSecureUpstreamHeaders(HttpServerResponse clientResp, MultiMap upstreamHeaders) {
		upstreamHeaders.iterator().forEachRemaining(h -> {
			if (!"Set-Cookie".equals(h.getKey())) {
				clientResp.headers().add(h.getKey(), h.getValue());
			} else {
				Cookie c = ClientCookieDecoder.LAX.decode(h.getValue());
				c.setHttpOnly(true);
				c.setSecure(SecurityConfig.secureCookies);
				clientResp.headers().add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(c));
			}
		});
	}

	private static HttpClient httpClient(Vertx vertx) {
		if (httpClient.isEmpty()) {
			HttpClientOptions opts = new HttpClientOptions();
			opts.setTcpNoDelay(true);
			opts.setKeepAlive(true);
			opts.setMaxPoolSize(200);
			opts.setMaxWebSockets(200);
			httpClient = Optional.of(vertx.createHttpClient(opts));
		}
		return httpClient.get();
	}

}
