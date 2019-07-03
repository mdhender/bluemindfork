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
package net.bluemind.backend.mail.replica.service.internal;

import java.util.concurrent.CompletableFuture;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.Pump;
import org.vertx.java.core.streams.ReadStream;
import org.vertx.java.core.streams.WriteStream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.bluemind.core.api.Stream;
import net.bluemind.core.rest.vertx.VertxStream;

public class SyncStreamDownload {

	private SyncStreamDownload() {
	}

	private static class TargetStream implements WriteStream<TargetStream> {

		public final ByteBuf out = Unpooled.buffer();

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
			out.writeBytes(data.getBytes());
			return this;
		}

	}

	public static CompletableFuture<ByteBuf> read(Stream s) {
		CompletableFuture<ByteBuf> ret = new CompletableFuture<>();
		TargetStream out = new TargetStream();
		ReadStream<?> toRead = VertxStream.read(s);
		toRead.exceptionHandler(t -> ret.completeExceptionally(t));
		toRead.endHandler(v -> ret.complete(out.out));
		Pump pump = Pump.createPump(toRead, out);
		pump.start();
		toRead.resume();
		return ret;
	}

}
