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
package net.bluemind.vertx.common.request.impl;

import java.io.File;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.netflix.spectator.api.DistributionSummary;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Timer;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpFrame;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.StreamPriority;
import io.vertx.core.net.HostAndPort;
import net.bluemind.core.commons.logs.MdcLogUser;
import net.bluemind.metrics.registry.IdFactory;

final class WrappedResponse implements HttpServerResponse {

	private static final Logger logger = LoggerFactory.getLogger(WrappedResponse.class);
	private final HttpServerResponse impl;
	private final long reqStart;
	private final Map<String, String> logAttributes;
	private final Registry registry;
	private final IdFactory idFactory;
	private final LongAdder respSize;
	private final Map<String, String> dataAttributes;

	WrappedResponse(Registry registry, IdFactory idfactory, HttpServerResponse impl) {
		this.respSize = new LongAdder();
		this.registry = registry;
		this.idFactory = idfactory;
		this.impl = impl;
		this.reqStart = registry.clock().monotonicTime();
		this.logAttributes = new LinkedHashMap<>();
		this.dataAttributes = new LinkedHashMap<>();
	}

	private void endImpl() {
		long end = registry.clock().monotonicTime();
		long spent = end - reqStart;
		DistributionSummary sizeSummary = registry.distributionSummary(idFactory.name("responseSize"));
		sizeSummary.record(respSize.sumThenReset());
		if (!logAttributes.containsKey("async")) {
			Timer timer = registry.timer(idFactory.name("executionTime"));
			timer.record(spent, TimeUnit.NANOSECONDS);
		}
		logAttributes.put("http.out", Integer.toString(impl.getStatusCode()));
		StringBuilder tags = new StringBuilder();
		tags.append('[');
		boolean first = true;
		for (Entry<String, String> kv : logAttributes.entrySet()) {
			tags.append(first ? "" : ", ").append(kv.getKey()).append(": ").append(kv.getValue());
			first = false;
		}
		tags.append(']');

		getUserLogin().ifPresentOrElse(
				u -> MdcLogUser.logInfoAsUser(u, logger, "{} completed in {}ms.", tags,
						TimeUnit.NANOSECONDS.toMillis(spent)),
				() -> logger.info("{} completed in {}ms.", tags, TimeUnit.NANOSECONDS.toMillis(spent)));
	}

	private java.util.Optional<String> getUserLogin() {
		return java.util.Optional.ofNullable(dataAttributes.getOrDefault("user-login", null));
	}

	public void putLogAttribute(String k, String v) {
		logAttributes.put(k, v);
	}

	public void putDataAttribute(String k, String v) {
		dataAttributes.put(k, v);
	}

	public HttpServerResponse exceptionHandler(Handler<Throwable> handler) {
		impl.exceptionHandler(handler);
		return this;
	}

	public HttpServerResponse setWriteQueueMaxSize(int maxSize) {
		impl.setWriteQueueMaxSize(maxSize);
		return this;
	}

	public boolean writeQueueFull() {
		return impl.writeQueueFull();
	}

	public HttpServerResponse drainHandler(Handler<Void> handler) {
		impl.drainHandler(handler);
		return this;
	}

	public int getStatusCode() {
		return impl.getStatusCode();
	}

	public HttpServerResponse setStatusCode(int statusCode) {
		impl.setStatusCode(statusCode);
		return this;
	}

	public String getStatusMessage() {
		return impl.getStatusMessage();
	}

	public HttpServerResponse setStatusMessage(String statusMessage) {
		impl.setStatusMessage(Optional.fromNullable(statusMessage).or("null message"));
		return this;
	}

	public HttpServerResponse setChunked(boolean chunked) {
		impl.setChunked(chunked);
		return this;
	}

	public boolean isChunked() {
		return impl.isChunked();
	}

	public MultiMap headers() {
		return impl.headers();
	}

	public HttpServerResponse putHeader(String name, String value) {
		impl.putHeader(name, value);
		return this;
	}

	public HttpServerResponse putHeader(CharSequence name, CharSequence value) {
		impl.putHeader(name, value);
		return this;
	}

	public HttpServerResponse putHeader(String name, Iterable<String> values) {
		impl.putHeader(name, values);
		return this;
	}

	public HttpServerResponse putHeader(CharSequence name, Iterable<CharSequence> values) {
		impl.putHeader(name, values);
		return this;
	}

	public MultiMap trailers() {
		return impl.trailers();
	}

	public HttpServerResponse putTrailer(String name, String value) {
		impl.putTrailer(name, value);
		return this;
	}

	public HttpServerResponse putTrailer(CharSequence name, CharSequence value) {
		impl.putTrailer(name, value);
		return this;
	}

	public HttpServerResponse putTrailer(String name, Iterable<String> values) {
		impl.putTrailer(name, values);
		return this;
	}

	public HttpServerResponse putTrailer(CharSequence name, Iterable<CharSequence> value) {
		impl.putTrailer(name, value);
		return this;
	}

	public HttpServerResponse closeHandler(Handler<Void> handler) {
		impl.closeHandler(handler);
		return this;
	}

	@Override
	public Future<Void> write(Buffer chunk) {
		respSize.add(chunk.length());
		return impl.write(chunk);
	}

	@Override
	public Future<Void> write(String chunk, String enc) {
		respSize.add(chunk.getBytes(Charset.forName(enc)).length);
		return impl.write(chunk, enc);
	}

	@Override
	public Future<Void> write(String chunk) {
		respSize.add(chunk.getBytes().length);
		return impl.write(chunk);
	}

	@Override
	public Future<Void> end(String chunk) {
		respSize.add(chunk.getBytes().length);
		endImpl();
		return impl.end(chunk);
	}

	@Override
	public Future<Void> end(String chunk, String enc) {
		respSize.add(chunk.getBytes(Charset.forName(enc)).length);
		endImpl();
		return impl.end(chunk, enc);
	}

	@Override
	public Future<Void> end(Buffer chunk) {
		respSize.add(chunk.length());
		endImpl();
		return impl.end(chunk);
	}

	@Override
	public Future<Void> end() {
		endImpl();
		return impl.end();
	}

	@Override
	public Future<Void> sendFile(String filename) {
		File toSend = new File(filename);
		if (toSend.exists()) {
			respSize.add(toSend.length());
		}
		endImpl();
		return impl.sendFile(filename);
	}

	/*
	 * Vertx asks for request.connection().close(); intead
	 */
	@Deprecated
	public void close() {
		impl.close();
	}

	public String getLogAttribute(String k) {
		return logAttributes.get(k);
	}

	public void write(Buffer data, Handler<AsyncResult<Void>> handler) {
		impl.write(data, handler);
	}

	@Override
	public void write(String chunk, String enc, Handler<AsyncResult<Void>> handler) {
		impl.write(chunk, enc, handler);
	}

	@Override
	public void write(String chunk, Handler<AsyncResult<Void>> handler) {
		impl.write(chunk, handler);
	}

	public void end(Handler<AsyncResult<Void>> handler) {
		impl.end(handler);
	}

	public HttpServerResponse endHandler(Handler<Void> handler) {
		return impl.endHandler(handler);
	}

	public HttpServerResponse writeContinue() {
		return impl.writeContinue();
	}

	public void end(String chunk, Handler<AsyncResult<Void>> handler) {
		impl.end(chunk, handler);
	}

	public void end(String chunk, String enc, Handler<AsyncResult<Void>> handler) {
		impl.end(chunk, enc, handler);
	}

	public void end(Buffer chunk, Handler<AsyncResult<Void>> handler) {
		impl.end(chunk, handler);
	}

	@Override
	public Future<Void> sendFile(String filename, long offset) {
		endImpl();
		return impl.sendFile(filename, offset);
	}

	@Override
	public Future<Void> sendFile(String filename, long offset, long length) {
		endImpl();
		return impl.sendFile(filename, offset, length);
	}

	@Override
	public HttpServerResponse sendFile(String filename, Handler<AsyncResult<Void>> resultHandler) {
		return impl.sendFile(filename, res -> {
			endImpl();
			resultHandler.handle(res);
		});
	}

	@Override
	public HttpServerResponse sendFile(String filename, long offset, Handler<AsyncResult<Void>> resultHandler) {
		return impl.sendFile(filename, offset, res -> {
			endImpl();
			resultHandler.handle(res);
		});
	}

	public HttpServerResponse sendFile(String filename, long offset, long length,
			Handler<AsyncResult<Void>> resultHandler) {
		return impl.sendFile(filename, offset, length, res -> {
			endImpl();
			resultHandler.handle(res);
		});
	}

	public boolean ended() {
		return impl.ended();
	}

	public boolean closed() {
		return impl.closed();
	}

	public boolean headWritten() {
		return impl.headWritten();
	}

	public HttpServerResponse headersEndHandler(Handler<Void> handler) {
		return impl.headersEndHandler(handler);
	}

	public HttpServerResponse bodyEndHandler(Handler<Void> handler) {
		return impl.bodyEndHandler(handler);
	}

	public long bytesWritten() {
		return impl.bytesWritten();
	}

	public int streamId() {
		return impl.streamId();
	}

	@Override
	public HttpServerResponse push(HttpMethod method, String host, String path,
			Handler<AsyncResult<HttpServerResponse>> handler) {
		return impl.push(method, host, path, handler);
	}

	@Override
	public HttpServerResponse push(HttpMethod method, String path, MultiMap headers,
			Handler<AsyncResult<HttpServerResponse>> handler) {
		return impl.push(method, path, headers, handler);
	}

	@Override
	public HttpServerResponse push(HttpMethod method, String path, Handler<AsyncResult<HttpServerResponse>> handler) {
		return impl.push(method, path, handler);
	}

	@Override
	public HttpServerResponse push(HttpMethod method, String host, String path, MultiMap headers,
			Handler<AsyncResult<HttpServerResponse>> handler) {
		return impl.push(method, host, path, headers, handler);
	}

	@Override
	public HttpServerResponse writeCustomFrame(int type, int flags, Buffer payload) {
		return impl.writeCustomFrame(type, flags, payload);
	}

	@Override
	public HttpServerResponse writeCustomFrame(HttpFrame frame) {
		return impl.writeCustomFrame(frame);
	}

	@Override
	public HttpServerResponse setStreamPriority(StreamPriority streamPriority) {
		return impl.setStreamPriority(streamPriority);
	}

	@Override
	public HttpServerResponse addCookie(Cookie cookie) {
		return impl.addCookie(cookie);
	}

	@Override
	public Cookie removeCookie(String name) {
		return impl.removeCookie(name);
	}

	@Override
	public Cookie removeCookie(String name, boolean invalidate) {
		return impl.removeCookie(name, invalidate);
	}

	@Override
	@Deprecated
	public Future<HttpServerResponse> push(HttpMethod method, String host, String path, MultiMap headers) {
		return impl.push(method, host, path, headers);
	}

	@Override
	public Future<HttpServerResponse> push(HttpMethod method, HostAndPort authority, String path, MultiMap headers) {
		return impl.push(method, authority, path, headers);
	}

	@Override
	public boolean reset(long code) {
		return impl.reset(code);
	}

	@Override
	public Set<Cookie> removeCookies(String name, boolean invalidate) {
		return impl.removeCookies(name, invalidate);
	}

	@Override
	public Cookie removeCookie(String name, String domain, String path, boolean invalidate) {
		return impl.removeCookie(name, domain, path, invalidate);
	}

	@Override
	public Future<Void> writeEarlyHints(MultiMap headers) {
		return impl.writeEarlyHints(headers);
	}

	@Override
	public void writeEarlyHints(MultiMap headers, Handler<AsyncResult<Void>> handler) {
		impl.writeEarlyHints(headers, handler);
	}
}
