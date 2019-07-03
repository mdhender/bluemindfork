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
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.ItemDescriptor;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.todolist.hook.ITodoListHook;
import net.bluemind.todolist.hook.internal.VTodoMessage;

public class VTodoConsumer implements ITodoListHook {

	private EventBus eb;
	private static final Logger logger = LoggerFactory.getLogger(VTodoConsumer.class);

	public VTodoConsumer() {
		this.eb = VertxPlatform.eventBus();
	}

	private void itemChanged(VTodoMessage message, CrudOperation op) {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(message.securityContext);
		try {
			IContainerManagement cm = prov.instance(IContainerManagement.class, message.container.uid);
			List<ItemDescriptor> descriptors = cm.getItems(Arrays.asList(message.itemUid));
			for (ItemDescriptor id : descriptors) {
				JsonObject js = new JsonObject();
				js.putString("messageClass", "IPM.Task");
				js.putString("containerUid", message.container.uid);
				js.putNumber("internalId", id.internalId);
				js.putString("operation", op.name());
				eb.send(Topic.MAPI_ITEM_NOTIFICATIONS, js);
			}
		} catch (ServerFault sf) {
			logger.error(sf.getMessage(), sf);
		}
	}

	@Override
	public void onTodoCreated(VTodoMessage message) {
		itemChanged(message, CrudOperation.Create);
	}

	@Override
	public void onTodoUpdated(VTodoMessage message) {
		itemChanged(message, CrudOperation.Update);
	}

	@Override
	public void onTodoDeleted(VTodoMessage message) {
		JsonObject js = new JsonObject();
		js.putString("messageClass", "IPM.Task");
		js.putString("containerUid", message.container.uid);
		js.putNumber("internalId", 0L);
		js.putString("operation", CrudOperation.Delete.name());
		eb.send(Topic.MAPI_ITEM_NOTIFICATIONS, js);
	}

}
