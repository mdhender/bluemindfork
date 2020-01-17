/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.vertx.testhelper;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

public class WriteStreamForTests implements WriteStream<Buffer> {

	public final Queue<Buffer> received = new LinkedList<>();
	public final CompletableFuture<Void> responseFuture = new CompletableFuture<>();

	@Override
	public WriteStreamForTests exceptionHandler(Handler<Throwable> handler) {
		return this;
	}

	@Override
	public WriteStreamForTests setWriteQueueMaxSize(int maxSize) {
		return this;
	}

	@Override
	public boolean writeQueueFull() {
		return false;
	}

	@Override
	public WriteStreamForTests drainHandler(Handler<Void> handler) {
		return this;
	}

	@Override
	public WriteStreamForTests write(Buffer data) {
		received.add(data);
		if (!responseFuture.isDone()) {
			responseFuture.complete(null);
		}
		return this;
	}

	@Override
	public WriteStream<Buffer> write(Buffer data, Handler<AsyncResult<Void>> handler) {
		write(data);
		handler.handle(FakeResult.ok(null));
		return this;
	}

	@Override
	public void end() {
		// yeah
	}

	@Override
	public void end(Handler<AsyncResult<Void>> handler) {
		handler.handle(FakeResult.ok(null));
	}

}
