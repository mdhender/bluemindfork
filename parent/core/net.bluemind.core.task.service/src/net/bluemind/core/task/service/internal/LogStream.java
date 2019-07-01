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

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.streams.ReadStream;

import net.bluemind.core.api.Stream;

public class LogStream implements ReadStream<LogStream>, Stream {

	private Handler<Buffer> handler;

	private ConcurrentLinkedDeque<Buffer> queue = new ConcurrentLinkedDeque<>();

	private boolean paused;

	private boolean ended;
	private Handler<Void> endHandler;

	@Override
	public LogStream dataHandler(Handler<Buffer> handler) {

		this.handler = handler;
		read();
		return this;
	}

	private LogStream read() {

		if (paused) {
			return null;
		}
		Buffer data = null;
		while ((data = queue.poll()) != null) {
			handler.handle(data);
			if (paused) { // NOSONAR
				break;
			}
		}

		if (ended) {
			ended();
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
	public LogStream exceptionHandler(Handler<Throwable> handler) {
		// we are so strong that we do not need exceptionHandler
		return this;
	}

	@Override
	public LogStream endHandler(Handler<Void> endHandler) {
		this.endHandler = endHandler;
		return null;
	}

	public void pushData(JsonObject logMessage) {
		queue.add(new Buffer(logMessage.encode()));
		if (!paused && handler != null) {
			read();
		}
	}

	public void end() {
		ended = true;
		ended();
	}
}