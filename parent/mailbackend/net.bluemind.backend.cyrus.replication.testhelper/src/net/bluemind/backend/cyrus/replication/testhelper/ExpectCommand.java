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
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

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
		Handler<Message<JsonObject>> handler = new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				vertx.eventBus().unregisterHandler("replication.apply.message", this);
				long elapsed = System.currentTimeMillis() - start;
				logger.info("APPLY MESSAGE after {}ms", elapsed);
				ret.complete(null);
			}
		};
		vertx.eventBus().registerLocalHandler("replication.apply.message", handler);
		return ret;
	}

	public CompletableFuture<Void> onNextApplyMailbox(String mboxUniqueId) {
		CompletableFuture<Void> ret = new CompletableFuture<>();
		long start = System.currentTimeMillis();
		Handler<Message<JsonObject>> handler = new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				vertx.eventBus().unregisterHandler("replication.apply.mailbox." + mboxUniqueId, this);
				long elapsed = System.currentTimeMillis() - start;
				logger.info("APPLY MAILBOX completion received for {} after {}ms", mboxUniqueId, elapsed);
				ret.complete(null);
			}
		};
		vertx.eventBus().registerLocalHandler("replication.apply.mailbox." + mboxUniqueId, handler);
		logger.info("Handler registered for {}", mboxUniqueId);
		return ret;
	}

}
