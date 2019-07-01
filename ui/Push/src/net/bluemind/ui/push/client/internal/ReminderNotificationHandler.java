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
package net.bluemind.ui.push.client.internal;

import com.google.gwt.core.client.GWT;

import net.bluemind.ui.push.client.internal.notification.NotificationsConstants;
import net.bluemind.ui.push.client.internal.notification.ReminderNotification;

public class ReminderNotificationHandler implements MessageHandler<ReminderNotification> {

	public ReminderNotificationHandler() {

	}

	@Override
	public void onMessage(ReminderNotification msg) {
		GWT.log("msg: '" + msg + "', not.avail: " + Notifs.isHTML5NotificationsAvailable() + ", not.granted: "
				+ Notifs.isHTML5NotificationsGranted());

		if (Notifs.isHTML5NotificationsAvailable() && !Notifs.isHTML5NotificationsGranted()) {
			GWT.log("notif request");
			Notifs.HTML5NotificationsRequest();
		} else {

			int alert = Integer.parseInt(msg.getAlert());
			String body = "";
			if (alert == 0) {
				body = NotificationsConstants.INST.reminderBodyRightNow(msg.getBody());
			} else if (alert % 60 != 0) {
				body = NotificationsConstants.INST.reminderBody(msg.getBody(), Integer.toString(alert),
						NotificationsConstants.INST.seconds());
			} else if (alert % 3600 != 0) {
				alert = alert / 60;
				body = NotificationsConstants.INST.reminderBody(msg.getBody(), Integer.toString(alert),
						NotificationsConstants.INST.minutes());
			} else if (alert % 86400 != 0) {
				alert = alert / 3600;
				body = NotificationsConstants.INST.reminderBody(msg.getBody(), Integer.toString(alert),
						NotificationsConstants.INST.hours());
			} else {
				alert = alert / 86400;
				body = NotificationsConstants.INST.reminderBody(msg.getBody(), Integer.toString(alert),
						NotificationsConstants.INST.days());
			}

			String tag = "bmReminder_" + msg.getId();

			HTML5Notification.getInstance().show(tag, NotificationsConstants.INST.reminderTitle(), msg.getIco(), body,
					"/cal");
		}

	}

}
