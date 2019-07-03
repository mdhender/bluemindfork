/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.backend.cyrus.replication.server.state;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

public class ReadyStateNotifier {

	public static final ReadyStateNotifier INSTANCE = new ReadyStateNotifier();
	private static final Logger logger = LoggerFactory.getLogger(ReadyStateNotifier.class);

	private final AtomicLong notifyTimer;

	private ReadyStateNotifier() {
		notifyTimer = new AtomicLong(0);
	}

	public synchronized void notifyReady(Vertx vertx) {
		// the last timer will remain and this one will notify.
		// this is because we spawn multiple verticles and would like only one
		// notification in the end.
		long setTimer = notifyTimer.get();
		if (setTimer > 0) {
			vertx.cancelTimer(setTimer);
		}
		notifyTimer.set(vertx.setTimer(2000, tid -> {
			doNotify(vertx);
		}));

	}

	private void doNotify(Vertx vertx) {
		logger.info("******** Ready to receive Cyrus replication stream *********");
		JsonObject readyMsg = new JsonObject();
		vertx.eventBus().publish("mailreplica.receiver.ready", readyMsg);
	}

}
