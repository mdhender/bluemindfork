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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.imap.vertx.tests;

import java.util.concurrent.ConcurrentLinkedQueue;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import net.bluemind.lib.vertx.Result;
import net.bluemind.lib.vertx.VertxPlatform;

public class SlowSink implements WriteStream<Buffer> {

	ConcurrentLinkedQueue<Buffer> queue = new ConcurrentLinkedQueue<>();
	private Handler<Void> drain;
	private Handler<Throwable> exp;
	private long len;
	private boolean ended;

	@Override
	public WriteStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
		this.exp = handler;
		return this;
	}

	@Override
	public Future<Void> write(Buffer data) {
		queue.add(data);
		len += data.length();
		System.err.println("add " + data.length() + ", total: " + len);
		VertxPlatform.getVertx().setTimer(100, tid -> {
			System.err.println("clear Q " + Thread.currentThread().getName());
			queue.clear();
			if (drain != null) {
				drain.handle(null);
			}
		});
		return Future.succeededFuture();
	}

	public long length() {
		return len;
	}

	public boolean ended() {
		return ended;
	}

	@Override
	public void write(Buffer data, Handler<AsyncResult<Void>> handler) {
		write(data);
		handler.handle(Result.success());
	}

	@Override
	public Future<Void> end() {
		System.err.println("end SlowSink " + Thread.currentThread().getName());
		this.ended = true;
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
		return !queue.isEmpty();
	}

	@Override
	public WriteStream<Buffer> drainHandler(Handler<Void> handler) {
		this.drain = handler;
		if (queue.isEmpty()) {
			handler.handle(null);
		}
		return this;
	}

}
