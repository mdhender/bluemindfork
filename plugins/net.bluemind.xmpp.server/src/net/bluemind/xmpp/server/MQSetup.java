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
package net.bluemind.xmpp.server;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.IMQConnectHandler;
import net.bluemind.hornetq.client.Producer;
import net.bluemind.hornetq.client.Topic;

public class MQSetup {

	private static final Logger logger = LoggerFactory.getLogger(MQSetup.class);
	private static MQListener mqListener;

	public static final void init(CountDownLatch cdl) {
		mqListener = new MQListener();
		MQ.init(new IMQConnectHandler() {

			@Override
			public void connected() {
				MQ.registerConsumer(Topic.SYSTEM_NOTIFICATIONS, mqListener);

				// PRESENCE_NOTIFICATIONS: EventBoundariesJob, DNDHook
				MQ.registerConsumer(Topic.PRESENCE_NOTIFICATIONS, mqListener);
				MQ.registerConsumer(Topic.XIVO_PHONE_STATUS, mqListener);
				MQ.registerConsumer(Topic.CORE_NOTIFICATIONS, mqListener);

				MQ.registerProducer(Topic.IM_NOTIFICATIONS);
				logger.info("MQ setup complete.");
				cdl.countDown();
			}
		});

	}

	public static MQListener getMqListener() {
		return mqListener;
	}

	public static Producer getProducer() {
		return MQ.getProducer(Topic.IM_NOTIFICATIONS);
	}
}
