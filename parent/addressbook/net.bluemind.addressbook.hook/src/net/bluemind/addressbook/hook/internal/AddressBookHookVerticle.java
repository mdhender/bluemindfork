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
package net.bluemind.addressbook.hook.internal;

import java.util.List;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.platform.Verticle;

import net.bluemind.addressbook.api.AddressBookBusAddresses;
import net.bluemind.addressbook.hook.IAddressBookEventConsumer;
import net.bluemind.core.rest.LocalJsonObject;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class AddressBookHookVerticle extends Verticle {

	@Override
	public void start() {

		RunnableExtensionLoader<IAddressBookEventConsumer> loader = new RunnableExtensionLoader<IAddressBookEventConsumer>();

		List<IAddressBookEventConsumer> hooks = loader.loadExtensions("net.bluemind.addressbook", "hook", "hook",
				"impl");

		EventBus eventBus = vertx.eventBus();

		for (final IAddressBookEventConsumer hook : hooks) {
			eventBus.registerHandler(AddressBookBusAddresses.CREATED,
					new Handler<Message<LocalJsonObject<VCardMessage>>>() {
						public void handle(Message<LocalJsonObject<VCardMessage>> message) {
							hook.vcardCreated(message.body().getValue());
						}
					});

			eventBus.registerHandler(AddressBookBusAddresses.UPDATED,
					new Handler<Message<LocalJsonObject<VCardMessage>>>() {
						public void handle(Message<LocalJsonObject<VCardMessage>> message) {
							hook.vcardUpdated(message.body().getValue());
						}
					});

			eventBus.registerHandler(AddressBookBusAddresses.DELETED,
					new Handler<Message<LocalJsonObject<VCardMessage>>>() {
						public void handle(Message<LocalJsonObject<VCardMessage>> message) {
							hook.vcardDeleted(message.body().getValue());
						}
					});
		}

	}
}
