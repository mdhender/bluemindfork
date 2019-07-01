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
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.ReadStream;
import org.vertx.java.core.streams.WriteStream;

import net.bluemind.core.api.Stream;

public class VertxStreamProducer implements WriteStream<VertxStreamProducer>, Stream {
	private static final Logger logger = LoggerFactory.getLogger(VertxStreamProducer.class);

	private Vertx vertx;
	private String dataStream;
	private Handler<Throwable> exceptionHandler;
	public boolean writeQueueFull = false;

	private Handler<Void> drainHandler;

	private boolean ended;

	public VertxStreamProducer(Vertx vertx, String streamAddress) {
		this.vertx = vertx;
		this.dataStream = streamAddress;
	}

	@Override
	public VertxStreamProducer exceptionHandler(Handler<Throwable> handler) {
		this.exceptionHandler = handler;
		return this;
	}

	@Override
	public VertxStreamProducer setWriteQueueMaxSize(int maxSize) {
		return this;
	}

	@Override
	public boolean writeQueueFull() {
		return writeQueueFull;
	}

	@Override
	public VertxStreamProducer drainHandler(Handler<Void> handler) {
		this.drainHandler = handler;
		return this;
	}

	@Override
	public VertxStreamProducer write(Buffer data) {
		logger.debug("send data {} to stream {} queueFull {} ended : {}", data, dataStream, writeQueueFull, ended);

		vertx.eventBus().send(dataStream, new VertxRestStreamObject(data, false));
		return this;
	}

	protected void drain() {
		logger.debug("drain producer (stream {}, queueFull {} , ended : {})", dataStream, writeQueueFull, ended);

		if (ended) {
			sendEnd();
		} else if (drainHandler != null) {
			drainHandler.handle(null);
		}
	}

	public void sendEnd() {
		ended = true;
		logger.info("send ended  to stream {} queueFull {} ended : {}", dataStream, writeQueueFull, ended);
		vertx.eventBus().send(dataStream, new VertxRestStreamObject(null, true));
	}

	public static void stream(final Vertx vertx, String controlAdr, final ReadStream<?> bodyStream) {
		new VertxStreamProducerControlHandler(vertx, controlAdr, bodyStream).stream();
	}

	public void closed() {
		if (exceptionHandler != null) {
			exceptionHandler.handle(new Exception("closed before end"));
		}
	}

}
