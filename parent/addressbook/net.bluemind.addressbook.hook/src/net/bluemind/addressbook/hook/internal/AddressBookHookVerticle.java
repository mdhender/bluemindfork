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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import net.bluemind.addressbook.api.AddressBookBusAddresses;
import net.bluemind.addressbook.hook.IAddressBookEventConsumer;
import net.bluemind.core.rest.LocalJsonObject;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class AddressBookHookVerticle extends AbstractVerticle {

	@Override
	public void start() {

		RunnableExtensionLoader<IAddressBookEventConsumer> loader = new RunnableExtensionLoader<>();

		List<IAddressBookEventConsumer> hooks = loader.loadExtensions("net.bluemind.addressbook", "hook", "hook",
				"impl");

		EventBus eventBus = vertx.eventBus();

		for (final IAddressBookEventConsumer hook : hooks) {
			eventBus.consumer(AddressBookBusAddresses.CREATED,
					(Message<LocalJsonObject<VCardMessage>> message) -> hook.vcardCreated(message.body().getValue()));
			eventBus.consumer(AddressBookBusAddresses.UPDATED,
					(Message<LocalJsonObject<VCardMessage>> message) -> hook.vcardUpdated(message.body().getValue()));
			eventBus.consumer(AddressBookBusAddresses.DELETED,
					(Message<LocalJsonObject<VCardMessage>> message) -> hook.vcardDeleted(message.body().getValue()));
		}
	}
}
