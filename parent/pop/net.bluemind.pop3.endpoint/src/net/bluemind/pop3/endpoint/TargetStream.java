/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.pop3.endpoint;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import net.bluemind.lib.vertx.Result;

public class TargetStream implements WriteStream<Buffer> {

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
	public Future<Void> write(Buffer data) {
		out.writeBytes(data.getBytes());
		return Future.succeededFuture();
	}

	@Override
	public void write(Buffer data, Handler<AsyncResult<Void>> handler) {
		write(data);
		handler.handle(Result.success());
	}

	@Override
	public Future<Void> end() {
		return Future.succeededFuture();
	}

	@Override
	public void end(Handler<AsyncResult<Void>> handler) {
		handler.handle(Result.success());
	}

}
