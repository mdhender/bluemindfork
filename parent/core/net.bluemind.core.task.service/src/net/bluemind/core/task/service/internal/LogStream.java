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
package net.bluemind.core.task.service.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import net.bluemind.core.api.Stream;

public class LogStream implements ReadStream<Buffer>, Stream {

	private static final Logger logger = LoggerFactory.getLogger(LogStream.class);
	private Handler<Buffer> handler;

	private volatile boolean paused;

	private boolean ended;
	private Handler<Void> endHandler;
	private Handler<Throwable> exceptionHandler;

	private final ISubscriber sub;

	public LogStream(ISubscriber subscriber) {
		this.sub = subscriber;
	}

	@Override
	public LogStream handler(Handler<Buffer> handler) {

		this.handler = handler;
		read();
		return this;
	}

	private synchronized void read() {
		if (paused) {
			return;
		}
		fetchPending();

		maybeEnd();
	}

	private void maybeEnd() {
		try {
			if (ended) {
				ended();
			}
		} catch (Throwable e) {// NOSONAR
			if (exceptionHandler != null) {
				exceptionHandler.handle(e);
			} else {
				logger.error(e.getMessage(), e);
			}
		}
	}

	private void fetchPending() {
		if (handler == null) {
			return;
		}
		try {
			JsonObject data = null;
			while ((data = sub.fetchOne()) != null) {
				Buffer buf = Buffer.buffer(data.encode());
				handler.handle(buf);
				if (paused) { // NOSONAR: set from another thread
					break;
				}
				checkStreamEnd(data);
			}
		} catch (Throwable e) {// NOSONAR
			if (exceptionHandler != null) {
				exceptionHandler.handle(e);
			} else {
				logger.error(e.getMessage(), e);
			}
		}
	}

	private void checkStreamEnd(JsonObject data) {
		if (!ended && Boolean.TRUE.equals(data.getBoolean("end", false))) {
			ended = true;
		}
	}

	private void ended() {
		if (endHandler != null) {
			endHandler.handle(null);
			endHandler = null;
		}
	}

	@Override
	public LogStream pause() {
		paused = true;
		return this;
	}

	@Override
	public LogStream resume() {
		paused = false;
		read();
		return this;
	}

	@Override
	public LogStream exceptionHandler(Handler<Throwable> excHandler) {
		exceptionHandler = excHandler;
		return this;
	}

	@Override
	public LogStream endHandler(Handler<Void> endHandler) {
		this.endHandler = endHandler;
		read();
		return this;
	}

	public void wakeUp() {
		if (!paused && handler != null) {
			read();
		}
	}

	public void end() {
		ended = true;
		ended();
	}

	@Override
	public ReadStream<Buffer> fetch(long amount) {
		return this;
	}
}