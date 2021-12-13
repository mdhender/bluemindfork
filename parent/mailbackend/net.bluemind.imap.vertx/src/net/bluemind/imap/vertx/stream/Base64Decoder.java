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

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.codec.binary.Base64OutputStream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import net.bluemind.lib.vertx.Result;

public class Base64Decoder implements WriteStream<Buffer> {

	private final Base64OutputStream base64;
	private final WriteStream<Buffer> delegate;

	public Base64Decoder(WriteStream<Buffer> delegate) {
		this.delegate = delegate;
		this.base64 = new Base64OutputStream(new OutputStream() {

			@Override
			public void write(int b) throws IOException {
				throw new UnsupportedOperationException("write(int) is not supported");
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				ByteBuf toWrite = Unpooled.copiedBuffer(b, off, len);
				delegate.write(Buffer.buffer(toWrite));
			}

		}, false);

	}

	@Override
	public Base64Decoder exceptionHandler(Handler<Throwable> handler) {
		return this;
	}

	@Override
	public Future<Void> write(Buffer data) {
		try {
			base64.write(data.getBytes());
		} catch (IOException e) {
			// ok
		}
		return Future.succeededFuture();
	}

	@Override
	public void write(Buffer data, Handler<AsyncResult<Void>> handler) {
		write(data);
		handler.handle(Result.success());
	}

	@Override
	public Future<Void> end() {
		try {
			base64.flush();
		} catch (IOException e) {
			// ok
		}
		return delegate.end();
	}

	@Override
	public void end(Handler<AsyncResult<Void>> handler) {
		end();
		handler.handle(Result.success());
	}

	@Override
	public Base64Decoder setWriteQueueMaxSize(int maxSize) {
		return this;
	}

	@Override
	public boolean writeQueueFull() {
		return delegate.writeQueueFull();
	}

	@Override
	public Base64Decoder drainHandler(Handler<Void> handler) {
		delegate.drainHandler(handler);
		return this;
	}

}
