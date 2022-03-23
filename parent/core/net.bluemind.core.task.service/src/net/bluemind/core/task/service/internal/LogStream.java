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

import java.util.concurrent.ConcurrentLinkedDeque;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import net.bluemind.core.api.Stream;

public class LogStream implements ReadStream<Buffer>, Stream {

	private Handler<Buffer> handler;

	private ConcurrentLinkedDeque<Buffer> queue = new ConcurrentLinkedDeque<>();

	private boolean paused;

	private boolean ended;
	private Handler<Void> endHandler;
	private Handler<Throwable> exceptionHandler = null;

	@Override
	public LogStream handler(Handler<Buffer> handler) {

		this.handler = handler;
		read();
		return this;
	}

	private LogStream read() {
		if (paused) {
			return null;
		}
		try {
			Buffer data = null;
			while ((data = queue.poll()) != null) {
				handler.handle(data);
				if (paused) { // NOSONAR: set from another thread
					break;
				}
			}
			if (ended) {
				ended();
			}
		} catch (Exception e) {
			if (exceptionHandler != null) {
				exceptionHandler.handle(e);
			}
		}
		return this;
	}

	private void ended() {
		if (endHandler != null) {
			endHandler.handle(null);
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
		return null;
	}

	public void pushData(JsonObject logMessage) {
		queue.add(Buffer.buffer(logMessage.encode()));
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