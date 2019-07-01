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
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.streams.Pump;
import org.vertx.java.core.streams.ReadStream;

public class VertxStreamProducerControlHandler {

	private Logger logger = LoggerFactory.getLogger(VertxStreamProducerControlHandler.class);
	private String controlAdr;
	private Vertx vertx;
	private ReadStream<?> bodyStream;
	private VertxStreamProducer producer;
	private Handler<Message<String>> handler;

	public VertxStreamProducerControlHandler(Vertx vertx, String controlAdr, ReadStream<?> bodyStream) {
		this.vertx = vertx;
		this.controlAdr = controlAdr;
		this.bodyStream = bodyStream;
	}

	public void stream() {
		bodyStream.pause();
		handler = new Handler<Message<String>>() {

			@Override
			public void handle(final Message<String> msg) {
				vertx.runOnContext(new Handler<Void>() {

					@Override
					public void handle(Void event) {
						handleControlMessage(msg);
					}
				});

			}
		};
		vertx.eventBus().registerHandler(controlAdr, handler);
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
			producer.writeQueueFull = true;
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
		if (handler != null) {
			vertx.eventBus().unregisterHandler(controlAdr, handler);
			handler = null;
		}
	}

	protected void stream(final Vertx vertx, final String addr, final boolean resume) {

		producer = new VertxStreamProducer(vertx, addr);

		bodyStream.endHandler(new Handler<Void>() {

			@Override
			public void handle(Void event) {
				producer.sendEnd();
				close();
			}
		});

		Pump pump = Pump.createPump(bodyStream, producer);
		pump.start();

		if (resume) {

			bodyStream.resume();
		}
	}
}
