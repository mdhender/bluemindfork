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

import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.auditlog.CalendarAuditor;
import net.bluemind.calendar.hook.CalendarHookAddress;
import net.bluemind.calendar.hook.internal.VEventMessage;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.LocalJsonObject;

public class CalendarEventProducer {
	private String loginAtDomain;
	private EventBus eventBus;
	private Container container;
	private SecurityContext securityContext;
	private CalendarAuditor auditor;

	public CalendarEventProducer(CalendarAuditor auditor, Container container, SecurityContext securityContext,
			EventBus ev) {
		this.auditor = auditor;
		this.container = container;
		this.loginAtDomain = securityContext.getSubject();
		this.eventBus = ev;
		this.securityContext = securityContext;
	}

	public void changed() {
		JsonObject body = new JsonObject();
		body.putString("loginAtDomain", loginAtDomain);
		eventBus.publish(CalendarHookAddress.getChangedEventAddress(container.uid), body);

		eventBus.publish(CalendarHookAddress.CHANGED,
				new JsonObject().putString("container", container.uid).putString("type", container.type)
						.putString("loginAtDomain", loginAtDomain)
						.putString("domainUid", securityContext.getContainerUid()));

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

	public void serviceAccessed(final String calendarUid, final String origin, final boolean isRemote) {
		final JsonObject message = new JsonObject();
		message.putString("calendarUid", calendarUid);
		message.putString("origin", origin);
		message.putBoolean("isRemote", isRemote);
		eventBus.publish("bm.calendar.service.accessed", message);
	}

}
