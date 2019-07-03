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
package net.bluemind.core.container.service.internal;

import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.core.context.SecurityContext;

public class ContainersEventProducer {

	private EventBus eventBus;
	private SecurityContext securityContext;

	public ContainersEventProducer(SecurityContext securityContext, EventBus ev) {
		this.eventBus = ev;
		this.securityContext = securityContext;
	}

	public void changed(String type, String uid) {
		JsonObject body = new JsonObject();
		body.putString("loginAtDomain", securityContext.getSubject());
		eventBus.publish("bm." + type + ".hook." + uid + ".changed", body);
	}
}
