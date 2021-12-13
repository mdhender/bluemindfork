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
package net.bluemind.imap.vertx.stream;

import java.util.concurrent.ConcurrentLinkedDeque;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import net.bluemind.lib.vertx.Result;

public class WriteToRead<T> implements WriteStream<T>, ReadStream<T> {

	private final ConcurrentLinkedDeque<T> queue;
	private boolean ended;
	private Handler<Void> drain;
	private boolean pause;
	private Handler<T> dataHandler;
	private Handler<Void> end;

	public WriteToRead(Vertx vertx) {
		queue = new ConcurrentLinkedDeque<>();
	}

	@Override
	public WriteToRead<T> exceptionHandler(Handler<Throwable> handler) {
		return this;
	}

	@Override
	public Future<Void> write(T data) {
		queue.add(data);
		readLoop();
		return Future.succeededFuture();
	}

	@Override
	public void write(T data, Handler<AsyncResult<Void>> handler) {
		write(data);
		handler.handle(Result.success());
	}

	@Override
	public Future<Void> end() {
		this.ended = true;
		if (end != null) {
			final Handler<Void> endRef = end;
			Vertx.currentContext().runOnContext(endRef);
		}
		return Future.succeededFuture();
	}

	@Override
	public void end(Handler<AsyncResult<Void>> handler) {
		end();
		handler.handle(Result.success());
	}

	@Override
	public WriteToRead<T> setWriteQueueMaxSize(int maxSize) {
		return this;
	}

	@Override
	public boolean writeQueueFull() {
		return !queue.isEmpty();
	}

	@Override
	public WriteToRead<T> drainHandler(Handler<Void> handler) {
		this.drain = handler;
		return this;
	}

	@Override
	public WriteToRead<T> handler(Handler<T> handler) {
		this.dataHandler = handler;
		readLoop();
		return this;
	}

	@Override
	public WriteToRead<T> pause() {
		this.pause = true;
		return this;
	}

	@Override
	public WriteToRead<T> resume() {
		this.pause = false;
		readLoop();
		return this;
	}

	private void readLoop() {
		if (dataHandler == null) {
			return;
		}
		while (!pause && !queue.isEmpty()) {
			T data = queue.poll();
			dataHandler.handle(data);
		}
		if (queue.isEmpty() && drain != null) {
			drain.handle(null);
		}
	}

	@Override
	public WriteToRead<T> fetch(long amount) {
		return this;
	}

	@Override
	public WriteToRead<T> endHandler(Handler<Void> endHandler) {
		this.end = endHandler;
		if (ended) {
			endHandler.handle(null);
		}
		return this;
	}

}
