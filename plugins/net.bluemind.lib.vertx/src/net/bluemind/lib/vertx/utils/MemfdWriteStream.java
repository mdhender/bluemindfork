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
package net.bluemind.lib.vertx.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import net.bluemind.jna.utils.MemfdSupport;
import net.bluemind.jna.utils.OffHeapTemporaryFile;

public class MemfdWriteStream implements WriteStream<Buffer> {

	private static final AtomicLong ALLOC = new AtomicLong();
	private final CompletableFuture<OffHeapTemporaryFile> future;
	private final OffHeapTemporaryFile fd;
	private final OutputStream output;
	private Handler<Throwable> excep;

	public MemfdWriteStream() {
		this.fd = MemfdSupport.newOffHeapTemporaryFile("memfd-stream-" + ALLOC.incrementAndGet());
		this.future = new CompletableFuture<>();
		try {
			this.output = fd.openForWriting();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public CompletableFuture<OffHeapTemporaryFile> result() {
		return future;
	}

	@Override
	public WriteStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
		this.excep = handler;
		return this;
	}

	@Override
	public Future<Void> write(Buffer data) {
		try {
			output.write(data.getBytes());
			return Future.succeededFuture();
		} catch (Exception e) {
			fail(e);
			return Future.failedFuture(e);
		}
	}

	@Override
	public void write(Buffer data, Handler<AsyncResult<Void>> handler) {
		write(data).andThen(handler);
	}

	private void fail(Exception e) {
		if (excep != null) {
			excep.handle(e);
		}
		future.completeExceptionally(e);
		fd.close();
	}

	@Override
	public Future<Void> end() {
		try {
			output.flush();
			output.close();
			future.complete(fd);
			return Future.succeededFuture();
		} catch (IOException e) {
			fail(e);
			return Future.failedFuture(e);
		}
	}

	@Override
	public void end(Handler<AsyncResult<Void>> handler) {
		end().andThen(handler);
	}

	@Override
	public WriteStream<Buffer> setWriteQueueMaxSize(int maxSize) {
		return this;
	}

	@Override
	public boolean writeQueueFull() {
		return false;
	}

	@Override
	public WriteStream<Buffer> drainHandler(Handler<Void> handler) {
		return this;
	}

}
