/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.metrics.core.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.metrics.alerts.api.IAlerts;

public class AlertsService implements IAlerts {

	private static final Logger logger = LoggerFactory.getLogger(AlertsService.class);
	private final BmContext context;

	public AlertsService(BmContext context) {
		this.context = context;
		logger.debug("ctx {}", this.context);
	}

	@Override
	public void receive(Stream payload) {
		logger.debug("Got stream {}", payload);
		CompletableFuture<Void> handled = new CompletableFuture<>();
		ReadStream<Buffer> toRead = VertxStream.read(payload);
		Buffer content = Buffer.buffer();
		toRead.endHandler(v -> handled.complete(null));
		toRead.handler(content::appendBuffer);
		toRead.resume();

		try {
			handled.get(5, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new ServerFault(e);
		}
		JsonObject jsPayload = new JsonObject(content.toString());
		logger.info("Got payload {}, forwarding to kapacitor.alert.", jsPayload);
		VertxPlatform.eventBus().publish("kapacitor.alert", jsPayload);
	}
}
