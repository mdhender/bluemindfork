/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.core.backup.continuous.events;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.lib.vertx.VertxPlatform;

public class Bubble {

	private Bubble() {

	}

	public static void owner(String domain, String owner) {
		VertxPlatform.eventBus().send(BubbleEventsVerticle.BUBBLE_ADDR,
				new JsonObject().put("domain", domain).put("owner", owner));
	}

	public static void owner(BaseContainerDescriptor cd) {
		if (cd.domainUid != null && cd.owner != null) {
			owner(cd.domainUid, cd.owner);
		}
	}

}
