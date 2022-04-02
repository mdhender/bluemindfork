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
package net.bluemind.backend.cyrus.replication.testhelper;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import net.bluemind.lib.vertx.VertxPlatform;

public class ExpectCommand {

	private final Vertx vertx;
	private static final Logger logger = LoggerFactory.getLogger(ExpectCommand.class);

	public ExpectCommand() {
		this.vertx = VertxPlatform.getVertx();
	}

	public CompletableFuture<Void> onNextApplyMessage() {
		CompletableFuture<Void> ret = new CompletableFuture<>();
		long start = System.currentTimeMillis();

		MessageConsumer<JsonObject> cons = vertx.eventBus().consumer("replication.apply.message");
		Handler<Message<JsonObject>> handler = (Message<JsonObject> event) -> {

			cons.unregister();
			long elapsed = System.currentTimeMillis() - start;
			logger.info("APPLY MESSAGE after {}ms", elapsed);
			ret.complete(null);
		};
		cons.handler(handler);
		return ret;
	}

	public CompletableFuture<Void> onNextApplyMailbox(String mboxUniqueId) {
		CompletableFuture<Void> ret = new CompletableFuture<>();
		long start = System.currentTimeMillis();

		MessageConsumer<JsonObject> cons = vertx.eventBus().consumer("replication.apply.mailbox." + mboxUniqueId);
		Handler<Message<JsonObject>> handler = (Message<JsonObject> event) -> {
			cons.unregister();
			long elapsed = System.currentTimeMillis() - start;
			logger.info("APPLY MAILBOX completion received for {} after {}ms", mboxUniqueId, elapsed);
			ret.complete(null);
		};
		cons.handler(handler);
		logger.info("Handler registered for {}", mboxUniqueId);
		return ret;
	}

}
