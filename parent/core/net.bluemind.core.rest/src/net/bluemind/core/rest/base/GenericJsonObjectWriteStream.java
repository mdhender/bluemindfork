/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.core.rest.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.lib.vertx.Result;

public abstract class GenericJsonObjectWriteStream<T> implements WriteStream<Buffer> {

	private static Logger logger = LoggerFactory.getLogger(GenericJsonObjectWriteStream.class);

	private Handler<Throwable> exceptionHandler;

	private Class<T> type;

	public GenericJsonObjectWriteStream(Class<T> type) {
		this.type = type;
	}

	@Override
	public GenericJsonObjectWriteStream<T> drainHandler(Handler<Void> drainHandler) {
		return this;
	}

	@Override
	public GenericJsonObjectWriteStream<T> setWriteQueueMaxSize(int arg0) {
		return this;
	}

	@Override
	public boolean writeQueueFull() {
		return false;
	}

	@Override
	public Future<Void> write(Buffer buffer) {
		try {
			T value = JsonUtils.read(buffer.toString(), type);

			next(value);
		} catch (Exception e) {
			error(e);
		}
		return Future.succeededFuture();
	}

	@Override
	public void write(Buffer buffer, Handler<AsyncResult<Void>> res) {
		write(buffer);
		res.handle(Result.success());
	}

	@Override
	public void end(Handler<AsyncResult<Void>> res) {
		res.handle(Result.success());
	}

	@Override
	public Future<Void> end() {
		return Future.succeededFuture();
	}

	protected abstract void next(T value) throws Exception;

	public GenericJsonObjectWriteStream<T> exceptionHandler(Handler<Throwable> exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
		return this;
	}

	private void error(Exception e) {
		if (exceptionHandler != null) {
			exceptionHandler.handle(e);
		} else {
			logger.error("error reading backup stream", e);
		}
	}
}
