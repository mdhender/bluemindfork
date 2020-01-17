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
package net.bluemind.core.rest.vertx;

import java.util.concurrent.ConcurrentLinkedDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import net.bluemind.core.api.Stream;

public class VertxStreamConsumer implements ReadStream<Buffer>, Stream {

	private static final Logger logger = LoggerFactory.getLogger(VertxStreamConsumer.class);

	private Vertx vertx;
	Handler<Void> endHandler;

	private String controlAddress;

	private String recvAddress;

	Handler<Buffer> dataHandler;

	private Handler<Throwable> exceptionHandler;

	private ConcurrentLinkedDeque<Buffer> dataQueue = new ConcurrentLinkedDeque<>();

	private VertxStreamConsumerControlHandler controlHandler;

	private boolean paused;

	private boolean ended;

	public VertxStreamConsumer(Vertx vertx, String streamAddress) {
		this.vertx = vertx;
		this.controlHandler = new VertxStreamConsumerControlHandler(vertx, this, streamAddress);
		this.controlAddress = streamAddress;
	}

	@Override
	public VertxStreamConsumer handler(Handler<Buffer> handler) {

		this.dataHandler = handler;
		controlHandler.start(true);
		return this;
	}

	@Override
	public VertxStreamConsumer pause() {
		this.paused = true;
		controlHandler.pause();
		return this;
	}

	@Override
	public VertxStreamConsumer resume() {
		this.paused = false;
		controlHandler.resume();
		return this;
	}

	@Override
	public VertxStreamConsumer exceptionHandler(Handler<Throwable> handler) {
		this.exceptionHandler = handler;
		return this;
	}

	@Override
	public VertxStreamConsumer endHandler(Handler<Void> endHandler) {
		this.endHandler = endHandler;
		return this;
	}

	public void pushData(Buffer data) {
		dataQueue.push(data);
		if (!paused) {
			flushQueue();
		}
	}

	private void flushQueue() {
		while (!paused && !dataQueue.isEmpty()) {
			Buffer buffer = dataQueue.poll();
			dataHandler.handle(buffer);
		}

		if (dataQueue.isEmpty() && ended) {
			endHandler.handle(null);
		}
	}

	public void pushEnd() {
		ended = true;
		flushQueue();
	}

	public void fail(Throwable e) {
		controlHandler.sendClose();
	}

	@Override
	public ReadStream<Buffer> fetch(long amount) {
		return this;
	}
}
