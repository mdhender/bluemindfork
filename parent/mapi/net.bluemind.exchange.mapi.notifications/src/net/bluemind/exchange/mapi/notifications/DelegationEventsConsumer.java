/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.exchange.mapi.notifications;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.core.container.hooks.IAclHook;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.rest.BmContext;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.todolist.api.ITodoUids;

/**
 * When ACLs change on an interesting container, trigger a refresh of owner's
 * DIO
 *
 */
public class DelegationEventsConsumer implements IAclHook {

	private static final Logger logger = LoggerFactory.getLogger(DelegationEventsConsumer.class);

	@Override
	public void onAclChanged(BmContext context, ContainerDescriptor container, List<AccessControlEntry> previous,
			List<AccessControlEntry> current) {
		switch (container.type) {
		case IAddressBookUids.TYPE:
		case ITodoUids.TYPE:
		case ICalendarUids.TYPE:
		case IMailboxAclUids.TYPE:
			triggerRefresh(container);
			break;
		default:
			// do nothing
		}
	}

	private void triggerRefresh(ContainerDescriptor container) {
		JsonObject js = new JsonObject();
		js.putString("domain", container.domainUid).putString("owner", container.owner);
		js.putString("containerUid", container.uid).putString("type", container.type);
		logger.info("Trigger DIO refresh after ACL change on {} ({})", container.uid, container.type);
		VertxPlatform.eventBus().publish(Topic.MAPI_DELEGATION_NOTIFICATIONS, js);
	}

}
