/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.sds.proxy.mapping;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.IVerticleFactory;

public class MappingCacheCleaner extends AbstractVerticle {

	public static class Build implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new MappingCacheCleaner();
		}

	}

	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		MQ.init().whenComplete((v, ex) -> {
			if (ex != null) {
				startPromise.fail(ex);
			} else {
				MQ.registerConsumer(Topic.MAPI_ITEM_NOTIFICATIONS, msg -> {
					// containerUid & owner
					String cont = msg.getStringProperty("containerUid");
					if (cont != null && cont.startsWith("mbox_records_")) {
						String folderId = IMailReplicaUids.uniqueId(cont);
						vertx.eventBus().send("mapping.ctrl.discard", folderId);
					}
				});
				startPromise.complete();
			}
		});
	}
}
