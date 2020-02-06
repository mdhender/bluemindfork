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
package net.bluemind.eas.verticle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.addressbook.api.AddressBookBusAddresses;
import net.bluemind.calendar.hook.CalendarHookAddress;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.Producer;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.todolist.hook.TodoListHookAddress;

public class EasContainerChangeVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(EasContainerChangeVerticle.class);
	private Producer calendarProducer;
	private Producer addressbookProducer;
	private Producer todolistProducer;

	@Override
	public void start() {
		MQ.init(new MQ.IMQConnectHandler() {

			@Override
			public void connected() {
				calendarProducer = MQ.registerProducer(Topic.CALENDAR_NOTIFICATIONS);
				addressbookProducer = MQ.registerProducer(Topic.CONTACT_NOTIFICATIONS);
				todolistProducer = MQ.registerProducer(Topic.TASK_NOTIFICATIONS);

			}
		});

		vertx.eventBus().consumer(CalendarHookAddress.CHANGED, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				if (calendarProducer != null) {
					OOPMessage msg = buildMessage(event);
					calendarProducer.send(msg);
					logger.info("Wake up {} devices for calendar changes", event.body().getString("loginAtDomain"));

				} else {
					logger.warn("no calendar change notification, failed to create producer");
				}

			}

		});

		vertx.eventBus().consumer(AddressBookBusAddresses.CHANGED, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				if (addressbookProducer != null) {
					OOPMessage msg = buildMessage(event);
					addressbookProducer.send(msg);
					logger.info("Wake up {} devices for contacts changes", event.body().getString("loginAtDomain"));
				} else {
					logger.warn("no contacts change notification, failed to create producer");
				}

			}

		});

		vertx.eventBus().consumer(TodoListHookAddress.CHANGED, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				if (todolistProducer != null) {
					OOPMessage msg = buildMessage(event);
					todolistProducer.send(msg);
					logger.info("Wake up {} devices for todolist changes", event.body().getString("loginAtDomain"));

				} else {
					logger.warn("no todolist change notification, failed to create producer");
				}

			}
		});

	}

	private OOPMessage buildMessage(Message<JsonObject> event) {
		OOPMessage msg = MQ.newMessage();
		msg.putStringProperty("container", event.body().getString("container"));
		msg.putStringProperty("userUid", event.body().getString("loginAtDomain"));
		msg.putStringProperty("domainUid", event.body().getString("domainUid"));
		return msg;
	}

}
