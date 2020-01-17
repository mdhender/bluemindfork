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
package net.bluemind.backend.postfix.internal.maps.events;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.backend.postfix.Activator;
import net.bluemind.backend.postfix.internal.maps.PostfixMapUpdater;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.utils.ThrottleMessages;

public class DirtyMapEvent extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(DirtyMapEvent.class);
	public static final String dirtyMaps = "postfix.map.dirty";
	private static final Lock oneAtATime = new ReentrantLock(false);
	private EventBus eb;

	@Override
	public void start() {
		this.eb = vertx.eventBus();
		logger.info("Registering postfix dirty map listener");

		Handler<Message<JsonObject>> h = (message) -> doUpdate();
		ThrottleMessages<JsonObject> tm = new ThrottleMessages<>((msg) -> "postfixMaps", h, vertx, 10000);

		eb.consumer(dirtyMaps, tm);
		eb.consumer("dir.entry.deleted", tm);
	}

	private void doUpdate() {
		if (Activator.DISABLE_EVENT) {
			logger.error(dirtyMaps + " event disabled");
			return;
		}

		logger.info("Updating postfix maps");

		long time = System.currentTimeMillis();
		if (oneAtATime.tryLock()) {
			try {
				ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(PostfixMapUpdater.class)
						.refreshMaps();
			} catch (ServerFault sf) {
				logger.error("Fail to update postfix maps: " + sf.getMessage(), sf);
			} finally {
				oneAtATime.unlock();
			}

			logger.info("Postfix maps updated (" + (System.currentTimeMillis() - time) + " ms)");
		} else {
			vertx.setTimer(10000, tid -> doUpdate());
		}
	}
}
