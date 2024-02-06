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

import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.streams.Pipe;
import io.vertx.core.streams.ReadStream;
import net.bluemind.central.reverse.proxy.vertx.Body;
import net.bluemind.central.reverse.proxy.vertx.ProxyRequest;
import net.bluemind.central.reverse.proxy.vertx.ProxyResponse;

class ProxyResponseImpl implements ProxyResponse {

	private final ProxyRequestImpl request;
	private final HttpServerResponse outboundResponse;
	private int statusCode;
	private String statusMessage;
	private Body body;
	private final MultiMap headers;
	private HttpClientResponse inboundResponse;
	private long maxAge;
	private String etag;
	private boolean publicCacheControl;
	private Function<ReadStream<Buffer>, ReadStream<Buffer>> bodyFilter = Function.identity();
	private String target;

	private static final Logger logger = LoggerFactory.getLogger(ProxyResponseImpl.class);

	ProxyResponseImpl(ProxyRequestImpl request, HttpServerResponse outboundResponse) {
		this.inboundResponse = null;
		this.statusCode = 200;
		this.headers = MultiMap.caseInsensitiveMultiMap();
		this.request = request;
		this.outboundResponse = outboundResponse;
	}

	ProxyResponseImpl(ProxyRequestImpl request, HttpServerResponse outboundResponse,
			HttpClientResponse inboundResponse) {

		// Determine content length
		long contentLength = -1L;
		String contentLengthHeader = inboundResponse.getHeader(HttpHeaders.CONTENT_LENGTH);
		if (contentLengthHeader != null) {
			try {
				contentLength = Long.parseLong(contentLengthHeader);
			} catch (NumberFormatException e) {
				// Ignore ???
			}
		}
		this.target = inboundResponse.netSocket().remoteAddress().hostAddress();

		this.request = request;
		this.inboundResponse = inboundResponse;
		this.outboundResponse = outboundResponse;
		this.statusCode = inboundResponse.statusCode();
		this.statusMessage = inboundResponse.statusMessage();
		this.body = Body.body(inboundResponse, contentLength);

		long maxAge = -1;
		boolean publicCacheControl = false;
		String cacheControlHeader = inboundResponse.getHeader(HttpHeaders.CACHE_CONTROL);
		if (cacheControlHeader != null) {
			CacheControl cacheControl = new CacheControl().parse(cacheControlHeader);
			if (cacheControl.isPublic()) {
				publicCacheControl = true;
				if (cacheControl.maxAge() > 0) {
					maxAge = (long) cacheControl.maxAge() * 1000;
				} else {
					String dateHeader = inboundResponse.getHeader(HttpHeaders.DATE);
					String expiresHeader = inboundResponse.getHeader(HttpHeaders.EXPIRES);
					if (dateHeader != null && expiresHeader != null) {
						maxAge = ParseUtils.parseHeaderDate(expiresHeader).getTime()
								- ParseUtils.parseHeaderDate(dateHeader).getTime();
					}
				}
			}
		}
		this.maxAge = maxAge;
		this.publicCacheControl = publicCacheControl;
		this.etag = inboundResponse.getHeader(HttpHeaders.ETAG);
		this.headers = MultiMap.caseInsensitiveMultiMap().addAll(inboundResponse.headers());
	}

	String targetAddress() {
		return target;
	}

	@Override
	public ProxyRequest request() {
		return request;
	}

	@Override
	public int getStatusCode() {
		return statusCode;
	}

	@Override
	public ProxyResponse setStatusCode(int sc) {
		statusCode = sc;
		return this;
	}

	@Override
	public String getStatusMessage() {
		return statusMessage;
	}

	@Override
	public ProxyResponse setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
		return this;
	}

	@Override
	public Body getBody() {
		return body;
	}

	@Override
	public ProxyResponse setBody(Body body) {
		this.body = body;
		return this;
	}

	@Override
	public boolean publicCacheControl() {
		return publicCacheControl;
	}

	@Override
	public long maxAge() {
		return maxAge;
	}

	@Override
	public String etag() {
		return etag;
	}

	@Override
	public MultiMap headers() {
		return headers;
	}

	@Override
	public ProxyResponse putHeader(CharSequence name, CharSequence value) {
		headers.set(name, value);
		return this;
	}

	@Override
	public ProxyResponse bodyFilter(Function<ReadStream<Buffer>, ReadStream<Buffer>> filter) {
		bodyFilter = filter;
		return this;
	}

	@Override
	public Future<Void> send() {
		Promise<Void> promise = request.context.promise();
		send(promise);
		return promise.future();
	}

	public void send(Handler<AsyncResult<Void>> completionHandler) {
		// Set stuff
		outboundResponse.setStatusCode(statusCode);

		if (statusMessage != null) {
			outboundResponse.setStatusMessage(statusMessage);
		}

		// Date header
		Date date = HttpUtils.dateHeader(headers);
		if (date == null) {
			date = new Date();
		}
		try {
			outboundResponse.putHeader("date", ParseUtils.formatHttpDate(date));
		} catch (Exception e) {
			logger.error("send error: {}", e.getMessage());
		}

		// Warning header
		List<String> warningHeaders = headers.getAll("warning");
		if (warningHeaders.size() > 0) {
			warningHeaders = new ArrayList<>(warningHeaders);
			String dateHeader = headers.get("date");
			Date dateInstant = dateHeader != null ? ParseUtils.parseHeaderDate(dateHeader) : null;
			Iterator<String> i = warningHeaders.iterator();
			// Suppress incorrect warning header
			while (i.hasNext()) {
				String warningHeader = i.next();
				Date warningInstant = ParseUtils.parseWarningHeaderDate(warningHeader);
				if (warningInstant != null && dateInstant != null && !warningInstant.equals(dateInstant)) {
					i.remove();
				}
			}
		}
		outboundResponse.putHeader("warning", warningHeaders);

		// Handle other headers
		headers.forEach(header -> {
			String name = header.getKey();
			String value = header.getValue();
			if (name.equalsIgnoreCase("date") || name.equalsIgnoreCase("warning")
					|| name.equalsIgnoreCase("transfer-encoding")) {
				// Skip
			} else {
				outboundResponse.headers().add(name, value);
			}
		});

		//
		if (body == null) {
			outboundResponse.end();
			return;
		}

		long len = body.length();
		if (len >= 0) {
			outboundResponse.putHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(len));
		} else {
			if (request.outboundRequest().version() == HttpVersion.HTTP_1_0) {
				// Special handling for HTTP 1.0 clients that cannot handle chunked encoding
				// we need to buffer the content
				BufferingWriteStream buffer = new BufferingWriteStream();
				body.stream().pipeTo(buffer, ar -> {
					if (ar.succeeded()) {
						Buffer content = buffer.content();
						outboundResponse.end(content, completionHandler);
					} else {
						System.out.println("Not implemented");
					}
				});
				return;
			}
			outboundResponse.setChunked(true);
		}
		ReadStream<Buffer> bodyStream = bodyFilter.apply(body.stream());
		sendResponse(bodyStream, completionHandler);
	}

	@Override
	public ProxyResponse release() {
		if (inboundResponse != null) {
			inboundResponse.resume();
			inboundResponse = null;
			body = null;
			headers.clear();
		}
		return this;
	}

	private void sendResponse(ReadStream<Buffer> body, Handler<AsyncResult<Void>> completionHandler) {
		Pipe<Buffer> pipe = body.pipe();
		pipe.endOnSuccess(true);
		pipe.endOnFailure(false);
		pipe.to(outboundResponse, ar -> {
			if (ar.failed()) {
				if (!(ar.cause() instanceof ClosedChannelException)) {
					logger.error("Failed piping outbound: {}", ar.cause().getMessage(), ar.cause());
				}
				request.inboundRequest.reset();
				outboundResponse.reset();
			}
			completionHandler.handle(ar);
		});
	}
}
