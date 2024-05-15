/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
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
package net.bluemind.imap.endpoint.events;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.json.JsonObject;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;

public class IdleGlobalChannel extends AbstractVerticle {

	public static class IdleFactory implements IUniqueVerticleFactory, IVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new IdleGlobalChannel();
		}

	}

	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		MQ.init(() -> {
			MQ.registerConsumer(Topic.IMAP_ITEM_NOTIFICATIONS, msg -> {
				JsonObject jsMsg = msg.toJson();
				String contUid = jsMsg.getString("containerUid");
				vertx.eventBus().publish("imap.item.per-container." + contUid, jsMsg);
			});
			startPromise.complete();
		});
	}

}
