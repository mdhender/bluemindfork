/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.eas.backend.bm.mail.loader;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import net.bluemind.core.api.Stream;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.lib.vertx.Result;

public class SyncStreamDownload {

	private static final Logger logger = LoggerFactory.getLogger(SyncStreamDownload.class);

	private SyncStreamDownload() {

	}

	private static class TargetStream implements WriteStream<Buffer> {

		public final Buffer out = Buffer.buffer();

		@Override
		public TargetStream exceptionHandler(Handler<Throwable> handler) {
			return this;
		}

		@Override
		public TargetStream setWriteQueueMaxSize(int maxSize) {
			return this;
		}

		@Override
		public boolean writeQueueFull() {
			return false;
		}

		@Override
		public TargetStream drainHandler(Handler<Void> handler) {
			return this;
		}

		@Override
		public TargetStream write(Buffer data) {
			out.appendBuffer(data);
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
			// ok
		}

		@Override
		public void end(Handler<AsyncResult<Void>> handler) {
			handler.handle(Result.success());
		}

	}

	private static class OIOTargetStream implements WriteStream<Buffer> {

		private final OutputStream out;

		public OIOTargetStream(OutputStream out) {
			this.out = out;
		}

		@Override
		public OIOTargetStream exceptionHandler(Handler<Throwable> handler) {
			return this;
		}

		@Override
		public OIOTargetStream setWriteQueueMaxSize(int maxSize) {
			return this;
		}

		@Override
		public boolean writeQueueFull() {
			return false;
		}

		@Override
		public OIOTargetStream drainHandler(Handler<Void> handler) {
			return this;
		}

		@Override
		public OIOTargetStream write(Buffer data) {
			try {
				out.write(data.getBytes());
			} catch (IOException e) {
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
			try {
				out.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}

		@Override
		public void end(Handler<AsyncResult<Void>> handler) {
			end();
			handler.handle(Result.success());
		}

	}

	public static CompletableFuture<Buffer> read(Stream s) {
		CompletableFuture<Buffer> ret = new CompletableFuture<>();
		TargetStream out = new TargetStream();
		ReadStream<Buffer> toRead = VertxStream.read(s);
		toRead.pipeTo(out, ar -> {
			if (ar.failed()) {
				ret.completeExceptionally(ar.cause());
			} else {
				ret.complete(null);
			}
		});
		toRead.resume();
		return ret;
	}

	public static CompletableFuture<Void> read(Stream s, OutputStream target) {
		CompletableFuture<Void> ret = new CompletableFuture<>();
		OIOTargetStream out = new OIOTargetStream(target);
		ReadStream<Buffer> toRead = VertxStream.read(s);
		toRead.pipeTo(out, ar -> {
			if (ar.failed()) {
				ret.completeExceptionally(ar.cause());
			} else {
				ret.complete(null);
			}
		});
		toRead.resume();
		return ret;
	}

}
