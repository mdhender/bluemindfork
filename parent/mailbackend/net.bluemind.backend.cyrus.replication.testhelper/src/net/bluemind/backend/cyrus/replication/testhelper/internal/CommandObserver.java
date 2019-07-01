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
package net.bluemind.backend.cyrus.replication.testhelper.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.backend.cyrus.replication.observers.IReplicationObserver;
import net.bluemind.backend.cyrus.replication.observers.IReplicationObserverProvider;

public class CommandObserver implements IReplicationObserver {

	private static final Logger logger = LoggerFactory.getLogger(CommandObserver.class);

	public static class Factory implements IReplicationObserverProvider {

		@Override
		public IReplicationObserver create(Vertx vertx) {
			return new CommandObserver(vertx);
		}

	}

	private final Vertx vertx;

	public CommandObserver(Vertx vertx) {
		this.vertx = vertx;
	}

	@Override
	public void onApplyMessages(int count) {
		logger.debug("On APPLY MESSAGE {} message(s)", count);
		vertx.eventBus().publish("replication.apply.message", new JsonObject().putNumber("count", count));
	}

	@Override
	public void onApplyMailbox(String mboxUniqueId) {
		logger.debug("On APPLY MAILBOX {}", mboxUniqueId);
		vertx.eventBus().publish("replication.apply.mailbox." + mboxUniqueId, new JsonObject());
	}

}
