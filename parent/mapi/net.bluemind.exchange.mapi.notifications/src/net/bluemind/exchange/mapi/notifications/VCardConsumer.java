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
package net.bluemind.exchange.mapi.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.hook.IAddressBookEventConsumer;
import net.bluemind.addressbook.hook.internal.VCardMessage;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.VertxPlatform;

public class VCardConsumer implements IAddressBookEventConsumer {

	private EventBus eb;
	private static final Logger logger = LoggerFactory.getLogger(VCardConsumer.class);

	public VCardConsumer() {
		this.eb = VertxPlatform.eventBus();
	}

	@Override
	public void vcardCreated(VCardMessage message) {
		itemChanged(message, CrudOperation.Create);
	}

	@Override
	public void vcardUpdated(VCardMessage msg) {
		itemChanged(msg, CrudOperation.Update);
	}

	@Override
	public void vcardDeleted(VCardMessage msg) {
		itemChanged(msg, CrudOperation.Delete);
	}

	private void itemChanged(VCardMessage message, CrudOperation op) {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(message.securityContext);
		try {
			IAddressBook book = prov.instance(IAddressBook.class, message.container.uid);
			ItemValue<VCard> cardItem = book.getComplete(message.itemUid);
			JsonObject js = new JsonObject();
			js.putString("messageClass", messageClass(cardItem));
			js.putString("containerUid", message.container.uid);
			js.putNumber("internalId", cardItem != null ? cardItem.internalId : 0L);
			js.putString("operation", op.name());
			eb.send(Topic.MAPI_ITEM_NOTIFICATIONS, js);
		} catch (ServerFault sf) {
			logger.error(sf.getMessage(), sf);
		}
	}

	private String messageClass(ItemValue<VCard> cardItem) {
		if (cardItem == null) {
			return "IPM.Contact";
		}
		switch (cardItem.value.kind) {
		case individual:
			return "IPM.Contact";
		case group:
			return "IPM.DistList";
		default:
			throw new RuntimeException("Unsupported card kind " + cardItem.value.kind);
		}
	}

}
