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
package net.bluemind.calendar.service.internal;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import net.bluemind.calendar.hook.CalendarHookAddress;
import net.bluemind.core.container.model.Container;

public class CalendarEventProducer {
	private EventBus eventBus;
	private Container container;

	public CalendarEventProducer(Container container, EventBus ev) {
		this.container = container;
		this.eventBus = ev;
	}

	public void changed() {
		JsonObject body = new JsonObject();
		body.put("loginAtDomain", container.owner);
		eventBus.publish(CalendarHookAddress.getChangedEventAddress(container.uid), body);

		eventBus.publish(CalendarHookAddress.CHANGED,
				new JsonObject().put("container", container.uid).put("type", container.type)
						.put("loginAtDomain", container.owner).put("domainUid", container.domainUid));
	}

	public void serviceAccessed(String calendarUid, String origin, boolean isInteractive, boolean isRemote) {
		JsonObject message = new JsonObject();
		message.put("calendarUid", calendarUid);
		message.put("origin", origin);
		message.put("isInteractive", isInteractive);
		message.put("isRemote", isRemote);
		eventBus.publish("bm.calendar.service.accessed", message);
	}

}
