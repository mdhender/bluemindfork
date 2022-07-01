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
package net.bluemind.calendar.hook;

import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.context.SecurityContext;

public class VEventMessage {

	public String itemUid;
	public VEventSeries vevent;
	public VEventSeries oldEvent;
	public boolean sendNotifications;
	public SecurityContext securityContext;
	public Container container;
	public String auditEventId;

	public VEventMessage() {
	}

	public VEventMessage(final VEventSeries vevent, final String uid, boolean sendNotifications,
			final SecurityContext securityContext, final String auditEventId, final Container container) {
		this.itemUid = uid;
		this.vevent = vevent;
		this.securityContext = securityContext;
		this.container = container;
		this.sendNotifications = sendNotifications;
		this.auditEventId = auditEventId;
	}

	public VEventMessage copy() {
		VEventMessage copy = new VEventMessage();
		copy.itemUid = this.itemUid;
		copy.vevent = this.vevent.copy();
		copy.oldEvent = this.oldEvent.copy();
		copy.sendNotifications = this.sendNotifications;
		copy.securityContext = this.securityContext;
		copy.container = this.container;
		copy.auditEventId = this.auditEventId;
		return copy;
	}

}
