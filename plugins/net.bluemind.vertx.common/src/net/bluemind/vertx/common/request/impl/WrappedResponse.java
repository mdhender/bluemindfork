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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerResponse;

import com.google.common.base.Optional;
import com.netflix.spectator.api.DistributionSummary;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Timer;

import net.bluemind.metrics.registry.IdFactory;

final class WrappedResponse implements HttpServerResponse {

	private static final Logger logger = LoggerFactory.getLogger(WrappedResponse.class);
	private final HttpServerResponse impl;
	private final long reqStart;
	private final Map<String, String> logAttributes;
	private final Registry registry;
	private final IdFactory idFactory;
	private final LongAdder respSize;

	WrappedResponse(Registry registry, IdFactory idfactory, HttpServerResponse impl) {
		this.respSize = new LongAdder();
		this.registry = registry;
		this.idFactory = idfactory;
		this.impl = impl;
		this.reqStart = registry.clock().monotonicTime();
		this.logAttributes = new LinkedHashMap<>();
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
		for (String s : logAttributes.keySet()) {
			String v = logAttributes.get(s);
			tags.append(first ? "" : ", ").append(s).append(": ").append(v);
			first = false;
		}
		tags.append(']');
		logger.info("{} completed in {}ms.", tags.toString(), TimeUnit.NANOSECONDS.toMillis(spent));
	}

	public void putLogAttribute(String k, String v) {
		logAttributes.put(k, v);
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

	public HttpServerResponse write(Buffer chunk) {
		respSize.add(chunk.length());
		impl.write(chunk);
		return this;
	}

	public HttpServerResponse write(String chunk, String enc) {
		respSize.add(chunk.getBytes(Charset.forName(enc)).length);
		impl.write(chunk, enc);
		return this;
	}

	public HttpServerResponse write(String chunk) {
		respSize.add(chunk.getBytes().length);
		impl.write(chunk);
		return this;
	}

	public void end(String chunk) {
		respSize.add(chunk.getBytes().length);
		endImpl();
		impl.end(chunk);
	}

	public void end(String chunk, String enc) {
		respSize.add(chunk.getBytes(Charset.forName(enc)).length);
		endImpl();
		impl.end(chunk, enc);
	}

	public void end(Buffer chunk) {
		respSize.add(chunk.length());
		endImpl();
		impl.end(chunk);
	}

	public void end() {
		endImpl();
		impl.end();
	}

	public HttpServerResponse sendFile(String filename) {
		File toSend = new File(filename);
		if (toSend.exists()) {
			respSize.add(toSend.length());
		}
		impl.sendFile(filename);
		return this;
	}

	public HttpServerResponse sendFile(String filename, String notFoundFile) {
		File toSend = new File(filename);
		if (toSend.exists()) {
			respSize.add(toSend.length());
		} else {
			toSend = new File(notFoundFile);
			if (toSend.exists()) {
				respSize.add(toSend.length());
			}
		}
		impl.sendFile(filename, notFoundFile);
		return this;
	}

	public HttpServerResponse sendFile(String filename, Handler<AsyncResult<Void>> resultHandler) {
		File toSend = new File(filename);
		if (toSend.exists()) {
			respSize.add(toSend.length());
		}
		impl.sendFile(filename, resultHandler);
		return this;
	}

	public HttpServerResponse sendFile(String filename, String notFoundFile, Handler<AsyncResult<Void>> resultHandler) {
		impl.sendFile(filename, notFoundFile, resultHandler);
		return this;
	}

	public void close() {
		impl.close();
	}

	public String getLogAttribute(String k) {
		return logAttributes.get(k);
	}

}
