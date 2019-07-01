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
package net.bluemind.todolist.service.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.ReadStream;

public abstract class GenericObjectStream<T> implements ReadStream<GenericObjectStream<T>> {

	private static Logger logger = LoggerFactory.getLogger(GenericObjectStream.class);

	private Handler<Buffer> dataHandler;
	private boolean paused;
	private Handler<Throwable> exceptionHandler;
	private Handler<Void> endHandler;
	private boolean ended;

	@Override
	public GenericObjectStream<T> dataHandler(Handler<Buffer> dataHandler) {
		this.dataHandler = dataHandler;
		read();
		return this;
	}

	@Override
	public GenericObjectStream<T> pause() {
		this.paused = true;
		return this;
	}

	@Override
	public GenericObjectStream<T> resume() {
		if (this.paused) {
			paused = false;
			read();
		}
		return this;
	}

	@Override
	public GenericObjectStream<T> exceptionHandler(Handler<Throwable> exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
		return this;
	}

	@Override
	public GenericObjectStream<T> endHandler(Handler<Void> endHandler) {
		this.endHandler = endHandler;
		return this;
	}

	private void read() {
		if (paused) {
			return;
		}

		while (!ended && !paused) {
			T n;
			try {
				n = next();

				if (n == null) {
					ended = true;
					if (endHandler != null) {
						endHandler.handle(null);
					}
				} else {
					dataHandler.handle(serialize(n));
				}

			} catch (Exception e) {
				error(e);
			}

		}

	}

	protected abstract Buffer serialize(T n) throws Exception;

	protected abstract T next() throws Exception;

	private void error(Exception e) {
		if (exceptionHandler != null) {
			exceptionHandler.handle(e);
		} else {
			logger.error("error reading backup stream", e);
		}
	}

}
