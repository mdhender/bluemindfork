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
package net.bluemind.core.rest.http.internal;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.asynchttpclient.request.body.Body;
import org.asynchttpclient.request.body.generator.BodyChunk;
import org.asynchttpclient.request.body.generator.BodyGenerator;
import org.asynchttpclient.request.body.generator.QueueBasedFeedableBodyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.Unpooled;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.Pump;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import net.bluemind.lib.vertx.Result;

public class BodyGeneratorStream extends QueueBasedFeedableBodyGenerator<ConcurrentLinkedQueue<BodyChunk>>
		implements BodyGenerator, WriteStream<Buffer> {

	private static final Logger logger = LoggerFactory.getLogger(BodyGeneratorStream.class);
	private ReadStream<Buffer> bodyStream;
	private Handler<Void> drainHandler;
	private final static byte[] ZERO = "".getBytes(US_ASCII);

	public BodyGeneratorStream(ReadStream<Buffer> bodyStream) {
		super(new ConcurrentLinkedQueue<>());
		this.bodyStream = bodyStream;
	}

	@Override
	public Body createBody() {
		bodyStream.endHandler((Void event) -> {
			logger.debug("send end of stream {}", event);
			try {
				feed(Unpooled.EMPTY_BUFFER, true);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		});
		Pump.pump(bodyStream, this).start();
		return super.createBody();
	}

	@Override
	protected boolean offer(BodyChunk chunk) {
		return queue.offer(chunk);
	}

	@Override
	public BodyGeneratorStream exceptionHandler(Handler<Throwable> handler) {
		return this;
	}

	@Override
	public BodyGeneratorStream setWriteQueueMaxSize(int maxSize) {
		return this;
	}

	@Override
	public boolean writeQueueFull() {
		return false;
	}

	@Override
	public BodyGeneratorStream drainHandler(Handler<Void> handler) {
		this.drainHandler = handler;
		return this;
	}

	@Override
	public BodyGeneratorStream write(Buffer data) {
		logger.debug("send chunck of data {}", data);
		try {
			feed(Unpooled.wrappedBuffer(data.getBytes()), false);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return this;
	}

	@Override
	public WriteStream<Buffer> write(Buffer data, Handler<AsyncResult<Void>> handler) {
		write(data);
		handler.handle(Result.success());
		return this;
	}

	@Override
	public void end() {
		// that's ok
	}

	@Override
	public void end(Handler<AsyncResult<Void>> handler) {
		handler.handle(Result.success());
	}

}
