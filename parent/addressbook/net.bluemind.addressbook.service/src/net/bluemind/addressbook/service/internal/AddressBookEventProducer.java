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
package net.bluemind.addressbook.service.internal;

import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.addressbook.api.AddressBookBusAddresses;
import net.bluemind.addressbook.hook.internal.VCardMessage;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.LocalJsonObject;

public class AddressBookEventProducer {

	private EventBus eventBus;
	private Container container;
	private SecurityContext securityContext;

	public AddressBookEventProducer(Container container, SecurityContext securityContext, EventBus ev) {
		this.container = container;
		this.eventBus = ev;
		this.securityContext = securityContext;
	}

	public void vcardCreated(String vcardUid) {
		VCardMessage msg = getMessage(vcardUid);
		eventBus.publish(AddressBookBusAddresses.CREATED, new LocalJsonObject<>(msg));
	}

	public void vcardUpdated(String vcardUid) {
		VCardMessage msg = getMessage(vcardUid);
		eventBus.publish(AddressBookBusAddresses.UPDATED, new LocalJsonObject<>(msg));
	}

	public void vcardDeleted(String vcardUid) {
		VCardMessage msg = getMessage(vcardUid);
		eventBus.publish(AddressBookBusAddresses.DELETED, new LocalJsonObject<>(msg));
	}

	public void changed() {
		JsonObject body = new JsonObject();
		body.putString("loginAtDomain", securityContext.getSubject());
		eventBus.publish(AddressBookBusAddresses.getChangedEventAddress(container.uid), body);
		eventBus.publish(AddressBookBusAddresses.CHANGED,
				new JsonObject().putString("container", container.uid).putString("type", container.type)
						.putString("loginAtDomain", securityContext.getSubject())
						.putString("domainUid", securityContext.getContainerUid()));
	}

	private VCardMessage getMessage(String vcardUid) {
		VCardMessage ret = new VCardMessage();
		ret.itemUid = vcardUid;
		ret.securityContext = securityContext;
		ret.container = container;
		return ret;
	}
}
