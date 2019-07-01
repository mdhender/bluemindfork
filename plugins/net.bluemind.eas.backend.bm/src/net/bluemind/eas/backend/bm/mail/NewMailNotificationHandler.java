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
package net.bluemind.eas.backend.bm.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.eventbus.EventBus;

import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.eas.backend.HierarchyNode;
import net.bluemind.eas.dto.EasBusEndpoints;
import net.bluemind.eas.dto.push.PushTrigger;
import net.bluemind.eas.exception.CollectionNotFoundException;
import net.bluemind.eas.store.ISyncStorage;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.OutOfProcessMessageHandler;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.vertx.common.LocalJsonObject;

public class NewMailNotificationHandler implements OutOfProcessMessageHandler {

	private static final Logger logger = LoggerFactory.getLogger(NewMailNotificationHandler.class);
	private static final EventBus eb = VertxPlatform.eventBus();

	private ISyncStorage store;

	public NewMailNotificationHandler(ISyncStorage store) {
		this.store = store;
	}

	@Override
	public void handle(OOPMessage m) {

		logger.debug("new mail notification onMsg op: {}", m);
		String messageClass = m.getStringProperty("messageClass");
		if (!"IPM.Note".equalsIgnoreCase(messageClass)) {
			return;
		}

		String uniqueId = m.getStringProperty("containerUid");
		String userUid = m.getStringProperty("owner");
		String domainUid = m.getStringProperty("domain");

		try {
			HierarchyNode node = store.getHierarchyNode(domainUid, userUid,
					ContainerHierarchyNode.uidFor(uniqueId, IMailReplicaUids.MAILBOX_RECORDS, domainUid));
			PushTrigger pt = PushTrigger.forCollection((int) node.collectionId);
			LocalJsonObject<PushTrigger> jso = new LocalJsonObject<>(pt);
			eb.send(EasBusEndpoints.PUSH_TRIGGER, jso);
		} catch (CollectionNotFoundException e) {
			logger.error(e.getMessage(), e);
		}
	}
}
