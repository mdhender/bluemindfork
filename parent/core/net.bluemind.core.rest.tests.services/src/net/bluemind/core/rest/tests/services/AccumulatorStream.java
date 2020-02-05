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

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

public class AccumulatorStream implements WriteStream<Buffer> {

	private Buffer buffer = Buffer.buffer();

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
	public AccumulatorStream write(Buffer data) {
		buffer.appendBuffer(data);
		return this;

	}

	public Buffer buffer() {
		return buffer;
	}

	@Override
	public WriteStream<Buffer> write(Buffer data, Handler<AsyncResult<Void>> handler) {
		write(data);
		handler.handle(null);
		return this;
	}

	@Override
	public void end() {
	}

	@Override
	public void end(Handler<AsyncResult<Void>> handler) {
		handler.handle(null);
	}
}
