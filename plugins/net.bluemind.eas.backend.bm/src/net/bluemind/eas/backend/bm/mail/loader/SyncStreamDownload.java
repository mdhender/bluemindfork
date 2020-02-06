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

import java.util.concurrent.CompletableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.Pump;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import net.bluemind.core.api.Stream;
import net.bluemind.core.rest.vertx.VertxStream;

public class SyncStreamDownload {

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

	public static CompletableFuture<Buffer> read(Stream s) {
		CompletableFuture<Buffer> ret = new CompletableFuture<Buffer>();
		TargetStream out = new TargetStream();
		ReadStream<Buffer> toRead = VertxStream.read(s);
		toRead.endHandler(v -> {
			ret.complete(out.out);
		});
		Pump pump = Pump.pump(toRead, out);
		pump.start();
		toRead.resume();
		return ret;
	}

}
