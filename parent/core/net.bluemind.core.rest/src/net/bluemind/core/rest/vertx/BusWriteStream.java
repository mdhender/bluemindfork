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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.streams.WriteStream;

public class BusWriteStream implements WriteStream<BusWriteStream> {
	private static final Logger logger = LoggerFactory.getLogger(BusWriteStream.class);
	protected String streamAddress;
	private Handler<Void> drainHandler;
	protected boolean queueFull = true;
	protected Message<?> replyEvent = null;
	private boolean ended;

	private Handler<Message<Void>> busHandler = new Handler<Message<Void>>() {

		@Override
		public void handle(Message<Void> control) {

			logger.debug("control message");
			if (!queueFull) {
				logger.warn("queue not full, initial control message ?");
			}
			if (replyEvent != null) {
				logger.error("reply already there, why this message !");
				throw new RuntimeException("reply already there, why this message !");
			}
			replyEvent = control;

			if (!ended) {
				if (drainHandler != null) {
					logger.debug("writestream [{}] drain ", streamAddress);
					queueFull = false;
					drainHandler.handle(null);
					logger.debug("writestream [{}] drained ", streamAddress);

				} else {
					queueFull = false;
					logger.debug("warn no drain handler [{}]", streamAddress);
				}
			}

			if (ended) {
				logger.debug("writestream [{}] send end ", streamAddress);

				if (replyEvent != null) {
					replyEvent.reply();
					replyEvent = null;
				}

			}

		}

	};

	@SuppressWarnings("unused")
	private Vertx vertx;

	public BusWriteStream(Vertx vertx, String streamAddress) {
		this.streamAddress = streamAddress;
		this.vertx = vertx;
	}

	public void complete() {
		logger.debug("writestream [{}] complete", streamAddress);
		ended = true;
		if (!queueFull && replyEvent != null) {
			logger.debug("writestream [{}] send end ", streamAddress);
			replyEvent.reply();
			replyEvent = null;
		}
	}

	@Override
	public BusWriteStream exceptionHandler(Handler<Throwable> handler) {
		return this;
	}

	@Override
	public BusWriteStream setWriteQueueMaxSize(int maxSize) {
		return this;
	}

	@Override
	public boolean writeQueueFull() {
		return queueFull;
	}

	@Override
	public BusWriteStream drainHandler(Handler<Void> handler) {
		this.drainHandler = handler;
		return this;
	}

	public Handler<Message<Void>> busHandler() {
		return busHandler;
	}

	@Override
	public BusWriteStream write(final Buffer data) {
		if (queueFull) {
			logger.error("should not write when queue is full");
			throw new RuntimeException("should not write when queue is full");
		}

		final Message<?> current = replyEvent;
		replyEvent = null;
		queueFull = true;
		logger.debug(" stream producer[{}]:{} reply data {}", streamAddress, queueFull, data);

		current.replyWithTimeout(data, 10000, new Handler<AsyncResult<Message<Void>>>() {

			@Override
			public void handle(AsyncResult<Message<Void>> event) {
				if (event.succeeded()) {
					busHandler().handle(event.result());
				} else {
					logger.error("stream producer [{}] : write timeout", streamAddress);
				}
			}
		});

		return this;
	}
}
