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
package net.bluemind.core.rest.tests.services;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import net.bluemind.lib.vertx.Result;

public class AccumulatorStream implements WriteStream<Buffer> {

	// private static final Logger logger =
	// LoggerFactory.getLogger(AccumulatorStream.class);
	private final ByteBuf buffer = Unpooled.buffer();
	public int call;

	@Override
	public AccumulatorStream exceptionHandler(Handler<Throwable> handler) {
		return this;
	}

	@Override
	public AccumulatorStream setWriteQueueMaxSize(int maxSize) {
		return this;
	}

	@Override
	public boolean writeQueueFull() {
		return false;
	}

	@Override
	public AccumulatorStream drainHandler(Handler<Void> handler) {
		return this;
	}

	@Override
	public synchronized AccumulatorStream write(Buffer data) {
		if (data != null) {
			call++;
			buffer.writeBytes(data.getByteBuf());
		}
		return this;

	}

	public Buffer buffer() {
		return Buffer.buffer(buffer);
	}

	@Override
	public WriteStream<Buffer> write(Buffer data, Handler<AsyncResult<Void>> handler) {
		write(data);
		handler.handle(Result.success());
		return this;
	}

	@Override
	public void end() {
	}

	@Override
	public void end(Handler<AsyncResult<Void>> handler) {
		handler.handle(Result.success());
	}
}
