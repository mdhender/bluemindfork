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

import net.bluemind.ui.push.client.internal.notification.MailNotification;

public class MailNotificationHandler implements MessageHandler<MailNotification> {

	@Override
	public void onMessage(MailNotification msg) {
		GWT.log("msg: '" + msg + "', not.avail: " + Notifs.isHTML5NotificationsAvailable() + ", not.granted: "
				+ Notifs.isHTML5NotificationsGranted());

		if (Notifs.isHTML5NotificationsAvailable() && !Notifs.isHTML5NotificationsGranted()) {
			GWT.log("notif request");
			Notifs.HTML5NotificationsRequest();
		} else {

			String url = "/webmail/?_task=mail&_action=show&_uid=" + msg.getUid() + "&_mbox=INBOX";

			HTML5Notification.getInstance().show("bmMail", msg.getTitle(), msg.getIco(), msg.getBody(), url);
		}

	}
}
