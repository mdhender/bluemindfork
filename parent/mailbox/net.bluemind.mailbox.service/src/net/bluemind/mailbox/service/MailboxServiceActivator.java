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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.mailbox.service;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.MailboxBusAddresses;

public class MailboxServiceActivator implements BundleActivator {

	private static final Logger logger = LoggerFactory.getLogger(MailboxServiceActivator.class);

	@Override
	public void start(BundleContext context) throws Exception {
		registerEventHandler();
	}

	@Override
	public void stop(BundleContext context) throws Exception {

	}

	public static void registerEventHandler() {

		logger.info("Registering Topic.MAILBOX_NOTIFICATIONS forwarding");

		MQ.init(new MQ.IMQConnectHandler() {

			@Override
			public void connected() {
				MQ.registerProducer(Topic.MAILBOX_NOTIFICATIONS);
			}
		});

		VertxPlatform.eventBus().registerHandler(MailboxBusAddresses.CHANGED, (message) -> {
			JsonObject body = (JsonObject) message.body();
			JsonObject mapped = new JsonObject() //
					.putString("mailbox", body.getString("itemUid")) //
					.putString("domain", body.getString("containerUid"));
			OOPMessage cm = new OOPMessage(mapped);
			MQ.getProducer(Topic.MAILBOX_NOTIFICATIONS).send(cm);
		});
	}

}
