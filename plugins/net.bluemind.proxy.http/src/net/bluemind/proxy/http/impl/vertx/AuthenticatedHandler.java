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
package net.bluemind.proxy.http.impl.vertx;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;

import com.netflix.spectator.api.Registry;

import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.proxy.http.IDecorableRequest;
import net.bluemind.proxy.http.InvalidSession;
import net.bluemind.proxy.http.config.ForwardedLocation;
import net.bluemind.proxy.http.impl.CachedTemplate;
import net.bluemind.proxy.http.impl.Templates;

/**
 * Proxies the request to the target server and serves it back chunked.
 */
public final class AuthenticatedHandler implements Handler<UserReq> {
	private static final Logger logger = LoggerFactory.getLogger(AuthenticatedHandler.class);
	private final HttpClient client;
	private final Registry registry;
	private final IdFactory idFactory;
	private final ForwardedLocation fl;

	public AuthenticatedHandler(Vertx vertx, ForwardedLocation fl, Registry registry, IdFactory idFactory) {
		this.client = createClient(vertx, fl);
		this.fl = fl;
		this.idFactory = idFactory;
		this.registry = registry;
	}

	private HttpClient createClient(Vertx vertx, ForwardedLocation fl) {
		HttpClient client = vertx.createHttpClient().setHost(fl.getHost()).setPort(fl.getPort());
		client.setTCPNoDelay(true);
		client.setUsePooledBuffers(true);
		client.setKeepAlive(true);
		client.setMaxPoolSize(200);
		client.exceptionHandler(new Handler<Throwable>() {

			@Override
			public void handle(Throwable event) {
				logger.error(event.getMessage(), event);
			}
		});

		return new WrappedClient(client);
	}

	public void handle(final UserReq userReq) {
		long nanos = System.nanoTime();
		try {
			handleImpl(userReq);
		} catch (Exception t) {
			logger.error("error handling request {}", userReq, t);
			String message = t.getMessage();
			if (message == null) {
				message = "null";
			}
			userReq.fromClient.response().setStatusCode(504).setStatusMessage(message).end();
		} finally {
			if (logger.isDebugEnabled()) {
				nanos = System.nanoTime() - nanos;
				HttpServerRequest req = userReq.fromClient;
				logger.debug("{} {} handled in {}ms.", req.method(), req.uri(), TimeUnit.NANOSECONDS.toMillis(nanos));
			}
		}
	}

	private void handleImpl(final UserReq userReq) {
		registry.counter(idFactory.name("upstreamRequestsCount", "path", fl.getPathPrefix())).increment();
		final long time = System.nanoTime();
		final HttpServerRequest clientReq = userReq.fromClient;

		String uri = clientReq.uri();
		if (uri.startsWith("/templates/")) {
			templatesUri(clientReq);
			return;
		}
		if (uri.endsWith("/ping")) {
			pingSession(userReq);
			return;
		}
		final HttpServerResponse clientResp = clientReq.response();
		final HttpClientRequest upstreamReq = client.request(clientReq.method(), clientReq.uri(),
				new Handler<HttpClientResponse>() {
					public void handle(final HttpClientResponse upstreamResp) {
						MultiMap upstreamHeaders = upstreamResp.headers();

						addAndSecureUpstreamHeaders(clientResp, upstreamHeaders);

						upstreamResp.exceptionHandler(h -> {
							logger.error("upstream error forwarding {} {} error : {}", clientReq.method(),
									clientReq.uri(), h.getMessage(), h);
							String message = h.getMessage();
							if (message == null) {
								message = "Internal Server Error";
							}
							clientResp.setStatusCode(500).setStatusMessage(message).end();
						});

						clientResp.setStatusCode(upstreamResp.statusCode());
						final AtomicLong writtenToClient = new AtomicLong();
						upstreamResp.dataHandler(new Handler<Buffer>() {
							public void handle(Buffer data) {
								writtenToClient.addAndGet(data.length());
								clientResp.write(data);
								if (clientResp.writeQueueFull()) {
									upstreamResp.pause();
									clientResp.drainHandler(event -> {
										upstreamResp.resume();
									});
								}
							}
						});
						upstreamResp.endHandler(new VoidHandler() {
							public void handle() {
								clientResp.end();
								long nanos = System.nanoTime() - time;
								logger.debug("U: [{}][{}bytes] {} {} took {}ms.", upstreamResp.statusCode(),
										writtenToClient.get(), clientReq.method(), clientReq.uri(),
										TimeUnit.NANOSECONDS.toMillis(nanos));
								registry.distributionSummary(
										idFactory.name("upstreamRequestSize", "path", fl.getPathPrefix()))
										.record(writtenToClient.get());
								registry.timer(idFactory.name("upstreamRequestTime", "path", fl.getPathPrefix()))
										.record(nanos, TimeUnit.NANOSECONDS);
							}
						});
					}
				});
		final MultiMap cHeaders = clientReq.headers();
		if (userReq.provider != null) {
			try {
				userReq.provider.decorate(userReq.sessionId, new IDecorableRequest() {

					@Override
					public void addHeader(String n, String v) {
						cHeaders.add(n, v);
					}
				});
			} catch (InvalidSession e) {
				userReq.fromClient.response().setStatusCode(302);
				userReq.fromClient.response().headers().add("Location", "/bluemind_sso_logout");
				userReq.fromClient.response().end();
				return;
			}
		}
		upstreamReq.setTimeout(30000);
		upstreamReq.exceptionHandler(new Handler<Throwable>() {

			@Override
			public void handle(Throwable event) {
				logger.error(clientReq.method() + " " + clientReq.uri() + " error: " + event.getMessage(), event);
				String message = event.getMessage();
				if (message == null) {
					message = "Internal Server Error";
				}
				clientResp.setStatusCode(500).setStatusMessage(message).end();

			}
		});
		cHeaders.remove("Connection");

		upstreamReq.headers().set(cHeaders);
		final AtomicLong writtenToUpstream = new AtomicLong();
		clientReq.exceptionHandler(new Handler<Throwable>() {

			@Override
			public void handle(Throwable event) {
				logger.error("Client req error: " + event.getMessage(), event);
				clientResp.setStatusCode(500).setStatusMessage("Internal Server Error").end();
			}
		});
		clientReq.dataHandler(new Handler<Buffer>() {
			public void handle(Buffer data) {
				writtenToUpstream.addAndGet(data.length());
				upstreamReq.write(data);
				if (upstreamReq.writeQueueFull()) {
					clientReq.pause();
					upstreamReq.drainHandler(event -> {
						clientReq.resume();
					});
				}
			}
		});

		clientReq.endHandler(new VoidHandler() {
			public void handle() {
				upstreamReq.end();
				long nanos = System.nanoTime() - time;
				logger.debug("C: {} {} {}b forwarded in {}ms.", clientReq.method(), clientReq.uri(),
						writtenToUpstream.get(), TimeUnit.NANOSECONDS.toMillis(nanos));
			}
		});
	}

	protected void addAndSecureUpstreamHeaders(HttpServerResponse clientResp, MultiMap upstreamHeaders) {
		upstreamHeaders.iterator().forEachRemaining(h -> {
			if (!"Set-Cookie".equals(h.getKey())) {
				clientResp.headers().add(h.getKey(), h.getValue());
			} else {
				Cookie c = ClientCookieDecoder.LAX.decode(h.getValue());
				c.setHttpOnly(true);
				c.setSecure(true);
				clientResp.headers().add("Set-Cookie", ServerCookieEncoder.LAX.encode(c));
			}
		});
	}

	private void pingSession(final UserReq userReq) {
		userReq.provider.ping(userReq.sessionId, new AsyncHandler<Boolean>() {

			@Override
			public void success(Boolean value) {
				if (value) {
					userReq.fromClient.response().headers().add("BMAuth", "OK");
					userReq.fromClient.response().setStatusCode(200);
					userReq.fromClient.response().end("OK");
				} else {
					userReq.fromClient.response().headers().set("Location", "/");
					userReq.fromClient.response().setStatusCode(302);
					userReq.fromClient.response().end();
				}
			}

			@Override
			public void failure(Throwable e) {
				logger.error("error during ping of session {} ", userReq.sessionId, e);
				userReq.fromClient.response().headers().set("Location", "/");
				userReq.fromClient.response().setStatusCode(302);
				userReq.fromClient.response().end();
			}
		});
	}

	private void templatesUri(final HttpServerRequest req) {
		req.endHandler(new Handler<Void>() {

			@Override
			public void handle(Void event) {
				try {
					CachedTemplate tpl = Templates.getCached(req.uri());
					HttpServerResponse resp = req.response();
					resp.setStatusCode(200);
					resp.headers().set("Content-Type", tpl.getContentType());
					resp.end(new Buffer(tpl.getContent()));
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
					req.response().setStatusCode(503).setStatusMessage(e.getMessage() != null ? e.getMessage() : "null")
							.end();
				}
			}
		});
	}
}
