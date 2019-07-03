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
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import net.bluemind.addressbook.domainbook.IDomainAddressBook;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.utils.ThrottleMessages;

public class DomainBookVerticle extends Verticle {

	private static final Logger logger = LoggerFactory.getLogger(DomainBookVerticle.class);

	public static boolean suspended = false;

	@Override
	public void start() {

		Handler<Message<? extends JsonObject>> h = new Handler<Message<? extends JsonObject>>() {

			@Override
			public void handle(Message<? extends JsonObject> event) {

				if (suspended) {
					logger.warn("domain book replication is suspended");
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
					logger.error("error during replication", e);
				}

				logger.info("replicate domain book {} DONE in {} ms", domain, (System.currentTimeMillis() - time));

			}

		};

		ThrottleMessages<JsonObject> tm = new ThrottleMessages<JsonObject>((msg) -> msg.body().getString("domain"), h,
				vertx, 5000);

		getVertx().eventBus().registerHandler("dir.changed", (Message<? extends JsonObject> msg) -> {
			if (suspended) {
				logger.warn("domain book replication is suspended");
				return;
			}
			tm.handle(msg);
		});
	}
}
