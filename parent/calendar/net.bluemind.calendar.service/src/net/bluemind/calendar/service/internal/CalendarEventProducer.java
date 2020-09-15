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
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.auditlog.CalendarAuditor;
import net.bluemind.calendar.hook.CalendarHookAddress;
import net.bluemind.calendar.hook.internal.VEventMessage;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.LocalJsonObject;

public class CalendarEventProducer {
	private EventBus eventBus;
	private Container container;
	private SecurityContext securityContext;
	private CalendarAuditor auditor;

	public CalendarEventProducer(CalendarAuditor auditor, Container container, SecurityContext securityContext,
			EventBus ev) {
		this.auditor = auditor;
		this.container = container;
		this.eventBus = ev;
		this.securityContext = securityContext;
	}

	public void changed() {
		JsonObject body = new JsonObject();
		body.put("loginAtDomain", container.owner);
		eventBus.publish(CalendarHookAddress.getChangedEventAddress(container.uid), body);

		eventBus.publish(CalendarHookAddress.CHANGED,
				new JsonObject().put("container", container.uid).put("type", container.type)
						.put("loginAtDomain", container.owner).put("domainUid", container.domainUid));
	}

	public void veventCreated(VEventSeries event, String uid, boolean sendNotifications) {
		VEventMessage msg = new VEventMessage(event, uid, sendNotifications, securityContext, auditor.eventId(),
				container);
		eventBus.publish(CalendarHookAddress.EVENT_CREATED, new LocalJsonObject<>(msg));
	}

	public void veventUpdated(VEventSeries old, VEventSeries vevent, String uid, boolean sendNotifications) {
		VEventMessage msg = new VEventMessage(vevent, uid, sendNotifications, securityContext, auditor.eventId(),
				container);
		msg.oldEvent = old;
		eventBus.publish(CalendarHookAddress.EVENT_UPDATED, new LocalJsonObject<>(msg));
	}

	public void veventDeleted(VEventSeries vevent, String uid, boolean sendNotifications) {
		VEventMessage msg = new VEventMessage(vevent, uid, sendNotifications, securityContext, auditor.eventId(),
				container);
		eventBus.publish(CalendarHookAddress.EVENT_DELETED, new LocalJsonObject<>(msg));
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
