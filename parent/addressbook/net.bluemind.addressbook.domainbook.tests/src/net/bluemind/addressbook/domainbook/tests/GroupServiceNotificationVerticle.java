/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.addressbook.domainbook.tests;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.group.service.internal.UpdateGroupVcardVerticle;
import net.bluemind.lib.vertx.VertxPlatform;

public class GroupServiceNotificationVerticle extends AbstractVerticle {
	private static Set<String> updatedVcards = ConcurrentHashMap.newKeySet();

	public static void clear() {
		updatedVcards.clear();
	}

	public static boolean contains(String k) {
		return updatedVcards.remove(k);
	}

	@Override
	public void start() {
		VertxPlatform.eventBus().consumer(UpdateGroupVcardVerticle.VCARD_NOTIFY_BUS_ADDRESS,
				(Message<JsonObject> msg) -> updatedVcards.add(msg.body().getString("group_uid")));
	}

	@Override
	public void stop() {
		updatedVcards.clear();
	}
}
