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

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.streams.ReadStream;

public class VertxStreamProducerControlHandler {

	private Logger logger = LoggerFactory.getLogger(VertxStreamProducerControlHandler.class);
	private String controlAdr;
	private Vertx vertx;
	private ReadStream<Buffer> bodyStream;
	private VertxStreamProducer producer;
	private MessageConsumer<String> cons;

	public VertxStreamProducerControlHandler(Vertx vertx, String controlAdr, ReadStream<Buffer> bodyStream) {
		this.vertx = vertx;
		this.controlAdr = controlAdr;
		this.bodyStream = bodyStream;
	}

	public void stream() {
		bodyStream.pause();
		Handler<Message<String>> handler = (final Message<String> msg) -> vertx
				.runOnContext((Void event) -> handleControlMessage(msg));
		this.cons = vertx.eventBus().consumer(controlAdr, handler);
	}

	protected void handleControlMessage(Message<String> msg) {

		final String b = msg.body();
		logger.info("receive something {}", b);
		if (b.startsWith("ready:")) {
			logger.info("recieve ready from {}", b.substring("ready:".length()));
			stream(vertx, b.substring("ready:".length()), false);
		} else if (b.startsWith("ready-and-resume:")) {
			logger.debug("reciveve ready and stream from {}", b.substring("ready-and-resume:".length()));
			stream(vertx, b.substring("ready-and-resume:".length()), true);
		} else if (b.equals("resume")) {
			producer.drain();
		} else if (b.equals("pause")) {
			producer.markQueueFull();
		} else if (b.equals("close")) {
			logger.info("close before completion !");
			// something want wrong
			if (producer != null) {
				producer.closed();
			}
			close();
		}

	}

	private void close() {
		logger.debug("closestream {}", controlAdr);
		if (cons != null) {
			cons.unregister();
			cons = null;
		}
	}

	protected void stream(final Vertx vertx, final String addr, final boolean resume) {
		producer = new VertxStreamProducer(vertx, addr);
		bodyStream.pipe().endOnComplete(false).to(producer, h -> {
			producer.sendEnd();
			close();
		});
		if (resume) {
			bodyStream.resume();
		}
	}
}