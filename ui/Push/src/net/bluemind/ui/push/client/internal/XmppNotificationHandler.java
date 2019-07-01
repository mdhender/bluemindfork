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

import com.google.gwt.dom.client.Element;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.DOM;

import net.bluemind.ui.push.client.internal.notification.NotificationsConstants;
import net.bluemind.ui.push.client.internal.notification.XmppNotification;

public class XmppNotificationHandler implements MessageHandler<XmppNotification> {

	private Storage storage;

	public XmppNotificationHandler() {
		storage = Storage.getLocalStorageIfSupported();
	}

	@Override
	public void onMessage(XmppNotification msg) {
		if ("message".equals(msg.getCategory())) {
			boolean notify = (storage.getItem("bm-im-focus") == null);
			if (notify) {
				if (msg.getBody() != null) {
					blink();
					notif(msg.getFrom(), msg.getPic(), msg.getBody());
				}
			}
		} else if ("mark-all-as-read".equals(msg.getCategory())) {
			unblink();
		} else if ("blink".equals(msg.getCategory())) {
			boolean notify = (storage.getItem("bm-im-focus") == null);
			if (notify) {
				blink();
			}
		} else if ("presence".equals(msg.getCategory())) {
			boolean notify = (storage.getItem("bm-im-focus") == null);
			if (notify) {
				blink();
				notif(msg.getSubscriptionFrom(), msg.getSubscriptionPic(),
						NotificationsConstants.INST.subscriptionRequest(msg.getSubscriptionFrom()));
			}
		}
	}

	private void notif(String title, String pic, String body) {
		if (Notifs.isHTML5NotificationsAvailable() && !Notifs.isHTML5NotificationsGranted()) {
			Notifs.HTML5NotificationsRequest();
		} else {
			HTML5Notification.getInstance().showAndOpenInAPopup("bm-im", title, pic, body, "/im/#");
		}
	}

	private void unblink() {
		Element bubble = DOM.getElementById("im-notifier");
		if (bubble != null) {
			bubble.removeClassName("blink");
		}
	}

	private void blink() {
		Element bubble = DOM.getElementById("im-notifier");
		if (bubble != null) {
			bubble.addClassName("blink");
		}
	}

}
