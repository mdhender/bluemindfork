/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.sds.sync.service;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.sds.sync.api.SdsSyncEvent;
import net.bluemind.sds.sync.api.SdsSyncEvent.Body;
import net.bluemind.sds.sync.service.internal.queue.SdsSyncQueue;

public class SdsSyncVerticle extends AbstractVerticle {
	private SdsSyncQueue queue;

	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {
		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new SdsSyncVerticle();
		}
	}

	@Override
	public void start() {
		EventBus bus = vertx.eventBus();
		queue = new SdsSyncQueue();
		bus.<JsonObject>consumer(SdsSyncEvent.BODYADD.busName(), msg -> {
			queue.putBody(SdsSyncEvent.BODYADD, Body.fromJson(msg.body()));
			msg.reply("ok");
		});
		bus.<JsonObject>consumer(SdsSyncEvent.BODYDEL.busName(), msg -> {
			queue.putBody(SdsSyncEvent.BODYDEL, Body.fromJson(msg.body()));
			msg.reply("ok");
		});
		bus.<String>consumer(SdsSyncEvent.FHADD.busName(), msg -> {
			queue.putFileHosting(SdsSyncEvent.FHADD, msg.body());
			msg.reply("ok");
		});
	}

	@Override
	public void stop() {
		try {
			queue.close();
		} catch (Exception e) {
			// Ignore
		}
	}
}