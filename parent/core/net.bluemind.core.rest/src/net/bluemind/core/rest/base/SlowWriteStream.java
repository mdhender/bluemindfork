/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.core.rest.base;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import net.bluemind.lib.vertx.Result;
import net.bluemind.lib.vertx.VertxPlatform;

public class SlowWriteStream implements WriteStream<Buffer> {

	private AtomicBoolean queueFull = new AtomicBoolean(false);
	private Handler<Void> drain;
	private LongAdder dl = new LongAdder();
	private long report;

	public SlowWriteStream() {
		this.report = VertxPlatform.getVertx().setPeriodic(1000, tid -> {
			System.err.println("GOT " + dl + " byte(s)");
		});
	}

	@Override
	public WriteStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
		return this;
	}

	@Override
	public Future<Void> write(Buffer data) {
		write(data, ar -> {
		});
		return Future.succeededFuture();
	}

	private void markQueueFull() {
		queueFull.set(true);
	}

	@Override
	public void write(Buffer data, Handler<AsyncResult<Void>> handler) {
		markQueueFull();
		dl.add(data.length());
		VertxPlatform.getVertx().setTimer(100, tid -> {
			queueFull.set(false);
			if (drain != null) {
				drain.handle(null);
			}
			handler.handle(Result.success());
		});
	}

	@Override
	public Future<Void> end() {
		queueFull.set(false);
		VertxPlatform.getVertx().cancelTimer(report);
		return Future.succeededFuture();
	}

	@Override
	public void end(Handler<AsyncResult<Void>> handler) {
		end();
		handler.handle(Result.success());
	}

	@Override
	public WriteStream<Buffer> setWriteQueueMaxSize(int maxSize) {
		return this;
	}

	@Override
	public boolean writeQueueFull() {
		return queueFull.get();
	}

	@Override
	public WriteStream<Buffer> drainHandler(Handler<Void> handler) {
		this.drain = handler;
		if (!writeQueueFull()) {
			handler.handle(null);
		}
		return this;
	}

}