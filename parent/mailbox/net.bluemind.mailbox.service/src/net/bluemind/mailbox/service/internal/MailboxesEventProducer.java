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
package net.bluemind.mailbox.service.internal;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.mailbox.api.MailboxBusAddresses;

public class MailboxesEventProducer {

	private String containerUid;
	private String loginAtDomain;
	private EventBus eventBus;

	public MailboxesEventProducer(String containerUid, SecurityContext sc, EventBus ev) {
		this.containerUid = containerUid;
		this.loginAtDomain = sc.getSubject();
		this.eventBus = ev;
	}

	public void created(String uid) {
		changed(uid, MailboxBusAddresses.CREATED);
	}

	public void updated(String uid) {
		changed(uid, MailboxBusAddresses.UPDATED);
	}

	public void deleted(String uid) {
		changed(uid, MailboxBusAddresses.DELETED);
	}

	private void changed(String uid, String address) {
		JsonObject body = new JsonObject();
		body.put("loginAtDomain", loginAtDomain);
		JsonObject eventData = new JsonObject().put("container", containerUid).put("containerUid", containerUid)
				.put("itemUid", uid).put("loginAtDomain", loginAtDomain);
		eventBus.publish(address, eventData);
		eventData.put("type", "mailboxacl");
		eventBus.publish(MailboxBusAddresses.getChangedEventAddress(containerUid), body);
		eventBus.publish(MailboxBusAddresses.CHANGED, eventData);

	}
}
