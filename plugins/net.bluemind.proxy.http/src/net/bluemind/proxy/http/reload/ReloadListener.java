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
package net.bluemind.proxy.http.reload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.eventbus.EventBus;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.OutOfProcessMessageHandler;
import net.bluemind.hornetq.client.Topic;

public class ReloadListener {

	private static final Logger logger = LoggerFactory.getLogger(ReloadListener.class);

	public ReloadListener() {

	}

	public void start(EventBus eventBus) {
		MQ.init(new MQ.IMQConnectHandler() {

			@Override
			public void connected() {
				MQ.registerConsumer(Topic.SERVICE_HPS_RELOAD, new OutOfProcessMessageHandler() {

					@Override
					public void handle(OOPMessage msg) {
						String origin = msg.getStringProperty("origin");
						logger.info("Reload bm-hps asked by {}", origin);

						eventBus.publish("bm.defaultdomain.changed", (Object) null);

					}
				});
			}
		});
	}

}
