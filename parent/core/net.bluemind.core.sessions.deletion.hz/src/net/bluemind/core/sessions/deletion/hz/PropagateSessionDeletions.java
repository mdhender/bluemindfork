/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.core.sessions.deletion.hz;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.sessions.ISessionDeletionListener;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.Producer;
import net.bluemind.hornetq.client.Topic;

public class PropagateSessionDeletions implements ISessionDeletionListener {

	private static final Logger logger = LoggerFactory.getLogger(PropagateSessionDeletions.class);

	private final CompletableFuture<Producer> producer;

	public PropagateSessionDeletions() {
		producer = MQ.init().thenApply(v -> MQ.getProducer(Topic.CORE_SESSIONS));
	}

	@Override
	public void deleted(String identity, String sid, SecurityContext securityContext) {
		if (securityContext.isInteractive()) {
			Producer prod = producer.getNow(null);
			if (prod != null) {
				OOPMessage cm = MQ.newMessage();
				cm.putStringProperty("sender", identity);
				cm.putStringProperty("operation", "logout");
				cm.putStringProperty("sid", sid);
				prod.send(cm);
				logger.debug("MQ: logout {} sent.", sid);
			} else {
				logger.warn("MQ is missing, logout support will fail");
			}
		}
	}

}
