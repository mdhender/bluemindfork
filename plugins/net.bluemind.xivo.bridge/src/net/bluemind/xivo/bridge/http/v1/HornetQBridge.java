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
package net.bluemind.xivo.bridge.http.v1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.shareddata.ConcurrentSharedMap;

import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.xivo.common.PhoneStatus;

public class HornetQBridge extends BusModBase {

	private static final Logger logger = LoggerFactory.getLogger(HornetQBridge.class);

	public HornetQBridge() {
	}

	@Override
	public void start() {
		super.start();
		Handler<Message<JsonObject>> phoneStatus = new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> msg) {
				forwardStatus(msg);
			}
		};
		eb.registerHandler(Topic.XIVO_PHONE_STATUS, phoneStatus);
	}

	private void forwardStatus(Message<JsonObject> msg) {
		JsonObject jso = msg.body();

		// BM-10768
		if (jso.containsField("status")) {
			String latd = jso.getString("username") + "@" + jso.getString("domain");
			PhoneStatus status = PhoneStatus.fromCode(jso.getNumber("status").intValue());

			ConcurrentSharedMap<String, Integer> sharedStatus = vertx.sharedData().getMap("phone_status");
			sharedStatus.put(latd, status.code());

			logger.info("[{}] forward to MQ {} => {}...", Thread.currentThread().getName(), latd, status);
			OOPMessage hqMsg = MQ.newMessage();
			hqMsg.putStringProperty("latd", latd);
			hqMsg.putStringProperty("status", status.name());
			hqMsg.putStringProperty("operation", "xivo.updatePhoneStatus");
			MQ.getProducer(Topic.XIVO_PHONE_STATUS).send(hqMsg);
			logger.debug("[{}] sent.", Thread.currentThread().getName());
			sendOK(msg);
		} else {
			sendError(msg, "missing status in received body " + jso.encode());
		}
	}

	public void stop() {
		super.stop();
	}

}
