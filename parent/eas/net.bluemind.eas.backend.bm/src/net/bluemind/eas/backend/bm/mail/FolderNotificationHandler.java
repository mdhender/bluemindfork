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

import io.vertx.core.json.JsonObject;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.OutOfProcessMessageHandler;
import net.bluemind.lib.vertx.VertxPlatform;

public class FolderNotificationHandler implements OutOfProcessMessageHandler {

	private static final Logger logger = LoggerFactory.getLogger(FolderNotificationHandler.class);

	@Override
	public void handle(OOPMessage m) {
		logger.debug("hierarchy notification onMsg op: {}", m);

		String owner = m.getStringProperty("owner");

		VertxPlatform.eventBus().publish("eas.hierarchy." + owner, new JsonObject());
	}

}
