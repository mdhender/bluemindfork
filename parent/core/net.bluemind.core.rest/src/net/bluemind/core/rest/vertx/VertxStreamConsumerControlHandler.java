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

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.impl.DefaultContext;

public class VertxStreamConsumerControlHandler {

	private Logger logger = LoggerFactory.getLogger(VertxStreamConsumerControlHandler.class);
	private String controlAddress;
	private String recvAddress = null;
	private Vertx vertx;
	private VertxStreamConsumer stream;
	private DefaultContext context;
	private Handler<Message<VertxRestStreamObject>> handler;

	public VertxStreamConsumerControlHandler(Vertx vertx, VertxStreamConsumer vertxStreamConsumer,
			String controlAddress) {
		this.vertx = vertx;
		this.stream = vertxStreamConsumer;
		this.controlAddress = controlAddress;
	}

	public void start(boolean andResume) {
		if (recvAddress == null) {
			recvAddress = UUID.randomUUID().toString();
			handleData(recvAddress);
			logger.debug("ready stream {}", controlAddress);
			if (andResume) {
				logger.debug("send ready and resume to {}", controlAddress);
				vertx.eventBus().send(controlAddress, "ready-and-resume:" + recvAddress);
			} else {
				logger.debug("send ready to {}", controlAddress);
				vertx.eventBus().send(controlAddress, "ready:" + recvAddress);
			}
		} else if (andResume) {
			logger.debug("resume stream {}", controlAddress);
			vertx.eventBus().send(controlAddress, "resume");
		}

	}

	private void handleData(final String recvAddress) {
		handler = new Handler<Message<VertxRestStreamObject>>() {

			@Override
			public void handle(final Message<VertxRestStreamObject> msg) {

				vertx.runOnContext(new Handler<Void>() {

					@Override
					public void handle(Void event) {
						handleStreamObject(msg);

					}
				});

			}
		};
		vertx.eventBus().registerHandler(recvAddress, handler);
	}

	protected void handleStreamObject(Message<VertxRestStreamObject> event) {
		VertxRestStreamObject body = event.body();
		logger.debug("receive data ({}) from stream {} end : {} ", body.data, controlAddress, body.end);
		if (body.end) {
			if (stream.endHandler != null) {
				stream.pushEnd();
				close();
			} else {
				logger.warn("no end handler!");
			}
		} else if (!body.end) {
			stream.pushData(body.data);
		}
	}

	public void pause() {
		logger.debug("pause stream {}", controlAddress);
		vertx.eventBus().send(controlAddress, "pause");
	}

	public void resume() {
		logger.debug("resume stream {}", controlAddress);
		vertx.eventBus().send(controlAddress, "resume");
	}

	public void sendClose() {
		logger.debug("close stream {}", controlAddress);
		vertx.eventBus().send(controlAddress, "close");
		close();
	}

	private void close() {
		if (handler != null) {
			vertx.eventBus().unregisterHandler(recvAddress, handler);
		}
	}
}
