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
package net.bluemind.eas.backend.bm.calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.eas.backend.HierarchyNode;
import net.bluemind.eas.exception.CollectionNotFoundException;
import net.bluemind.eas.store.ISyncStorage;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.OutOfProcessMessageHandler;
import net.bluemind.lib.vertx.VertxPlatform;

public class CalendarsNotificationHandler implements OutOfProcessMessageHandler {

	private static final Logger logger = LoggerFactory.getLogger(CalendarsNotificationHandler.class);
	private final ISyncStorage store;
	private static final EventBus eb = VertxPlatform.eventBus();

	public CalendarsNotificationHandler(ISyncStorage store) {
		this.store = store;
	}

	@Override
	public void handle(OOPMessage m) {
		logger.debug("calendar notification onMsg op: {}", m);

		String domainUid = m.getStringProperty("domainUid");

		if ("global.virt".equals(domainUid)) {
			return;
		}

		String container = m.getStringProperty("container");
		String userUid = m.getStringProperty("userUid");

		try {
			HierarchyNode node = store.getHierarchyNode(domainUid, userUid,
					ContainerHierarchyNode.uidFor(container, ICalendarUids.TYPE, domainUid));
			eb.publish("eas.collection." + node.collectionId, new JsonObject());
		} catch (CollectionNotFoundException e) {
			logger.error(e.getMessage(), e);
		}

	}

}
