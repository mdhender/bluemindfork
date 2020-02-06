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
package net.bluemind.core.rest.tests.services;

import java.util.concurrent.ConcurrentLinkedDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;

public class QueueReadStream implements ReadStream<Buffer> {

	private Logger logger = LoggerFactory.getLogger(QueueReadStream.class);
	private ConcurrentLinkedDeque<Buffer> queue = new ConcurrentLinkedDeque<>();
	private Handler<Buffer> dataHandler;
	private Handler<Void> endHandler;
	private boolean paused;
	private boolean ended;

	@Override
	public QueueReadStream handler(Handler<Buffer> handler) {
		this.dataHandler = handler;
		read();
		return this;
	}

	public void queue(Buffer buffer) {
		queue.add(buffer);
		read();
	}

	public void end() {
		ended = true;
		read();
	}

	private synchronized void read() {
		if (dataHandler == null) {
			return;
		}
		if (paused) {
			return;
		}

		Buffer data = null;
		while (!paused && (data = queue.poll()) != null) {
			dataHandler.handle(data);
		}

		if (!paused && ended) {
			endHandler.handle(null);
		}
	}

	@Override
	public QueueReadStream pause() {
		logger.debug(" pause " + Thread.currentThread());
		this.paused = true;
		return this;
	}

	@Override
	public synchronized QueueReadStream resume() {
		if (paused) {
			logger.debug(" resume {}", Thread.currentThread());
			this.paused = false;
			read();
		} else {
			logger.warn("was already resumed!");
		}
		return this;
	}

	@Override
	public QueueReadStream exceptionHandler(Handler<Throwable> handler) {
		return this;
	}

	@Override
	public QueueReadStream endHandler(Handler<Void> endHandler) {
		this.endHandler = endHandler;
		return this;
	}

	@Override
	public ReadStream<Buffer> fetch(long amount) {
		return this;
	}

}
