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

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import net.bluemind.calendar.hook.internal.VEventMessage;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.ItemDescriptor;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.VertxPlatform;

public class VEventConsumer implements net.bluemind.calendar.hook.ICalendarHook {

	private EventBus eb;
	private static final Logger logger = LoggerFactory.getLogger(VEventConsumer.class);

	public VEventConsumer() {
		this.eb = VertxPlatform.eventBus();
	}

	@Override
	public void onEventCreated(VEventMessage message) {
		itemChanged(message, CrudOperation.Create);
	}

	@Override
	public void onEventUpdated(VEventMessage message) {
		itemChanged(message, CrudOperation.Update);
	}

	@Override
	public void onEventDeleted(VEventMessage message) {
		JsonObject js = new JsonObject();
		js.put("messageClass", "IPM.Appointment");
		js.put("containerUid", message.container.uid);
		js.put("internalId", 0L);
		js.put("operation", CrudOperation.Delete.name());
		eb.send(Topic.MAPI_ITEM_NOTIFICATIONS, js);
	}

	private void itemChanged(VEventMessage message, CrudOperation op) {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(message.securityContext);
		try {
			IContainerManagement cm = prov.instance(IContainerManagement.class, message.container.uid);
			List<ItemDescriptor> descriptors = cm.getItems(Arrays.asList(message.itemUid));
			for (ItemDescriptor id : descriptors) {
				JsonObject js = new JsonObject();
				js.put("messageClass", "IPM.Appointment");
				js.put("containerUid", message.container.uid);
				js.put("internalId", id.internalId);
				js.put("operation", op.name());
				eb.send(Topic.MAPI_ITEM_NOTIFICATIONS, js);
			}
		} catch (ServerFault sf) {
			logger.error(sf.getMessage(), sf);
		}
	}

}
