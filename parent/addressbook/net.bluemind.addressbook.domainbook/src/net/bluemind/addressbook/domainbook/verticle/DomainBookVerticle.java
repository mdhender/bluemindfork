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
package net.bluemind.addressbook.domainbook.verticle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.addressbook.domainbook.IDomainAddressBook;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.utils.ThrottleMessages;

public class DomainBookVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(DomainBookVerticle.class);

	public static boolean suspended = false;

	@Override
	public void start() {

		Handler<Message<JsonObject>> h = new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {

				if (suspended) {
					logger.warn("domain book replication is suspended {}", event.body());
					return;
				}
				String domain = event.body().getString("domain");
				logger.info("replicate domain book {}", domain);
				long time = System.currentTimeMillis();
				BmContext context = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();
				try {
					IDomainAddressBook dBook = context.provider().instance(IDomainAddressBook.class, domain);
					if (dBook != null) {
						dBook.sync();
					}
				} catch (Exception e) {
					logger.error("error during replication: {}", e.getMessage());
				}

				logger.info("replicate domain book {} DONE in {} ms", domain, (System.currentTimeMillis() - time));
				// to help unit tests wait
				vertx.eventBus().publish("domainbook.sync." + domain, "DONE");
			}

		};

		ThrottleMessages<JsonObject> tm = new ThrottleMessages<>(msg -> msg.body().getString("domain"), h, vertx, 5000);

		vertx.eventBus().consumer("dir.changed", (Message<JsonObject> msg) -> {
			if (suspended) {
				logger.warn("domain book replication is suspended: {}", msg.body());
				return;
			}
			tm.handle(msg);
		});
	}
}
