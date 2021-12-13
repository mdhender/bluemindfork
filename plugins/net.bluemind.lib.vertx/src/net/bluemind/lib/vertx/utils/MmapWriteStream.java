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
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import net.bluemind.lib.vertx.internal.Result;

public class MmapWriteStream implements WriteStream<Buffer> {

	private final MappedByteBuffer targetBuffer;
	private final ByteBuf wrapped;
	private final CompletableFuture<ByteBuf> future;

	public MmapWriteStream(Path baseDir, long capacity) throws IOException {
		Path backingFile = Files.createTempFile(baseDir, "write-stream", ".mmap");
		try (RandomAccessFile raf = new RandomAccessFile(backingFile.toFile(), "rw")) {
			raf.setLength(capacity);
			this.targetBuffer = raf.getChannel().map(MapMode.READ_WRITE, 0, capacity);
			this.wrapped = Unpooled.wrappedBuffer(targetBuffer);
			this.wrapped.writerIndex(0).readerIndex(0);
		}
		Files.deleteIfExists(backingFile);
		this.future = new CompletableFuture<>();
	}

	public CompletableFuture<ByteBuf> mmap() {
		return future;
	}

	@Override
	public WriteStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
		return this;
	}

	@Override
	public Future<Void> write(Buffer data) {
		wrapped.writeBytes(data.getByteBuf());
		return Future.succeededFuture();
	}

	@Override
	public void write(Buffer data, Handler<AsyncResult<Void>> handler) {
		write(data);
		handler.handle(new Result<>());
	}

	@Override
	public Future<Void> end() {
		future.complete(wrapped);
		return Future.succeededFuture();
	}

	@Override
	public void end(Handler<AsyncResult<Void>> handler) {
		end();
		handler.handle(new net.bluemind.lib.vertx.internal.Result<>());
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
