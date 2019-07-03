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
package net.bluemind.gwtconsoleapp.base.notification;

import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.gwtconsoleapp.base.eventbus.GwtEventBus;
import net.bluemind.gwtconsoleapp.base.eventbus.NotificationEvent;
import net.bluemind.gwtconsoleapp.base.eventbus.NotificationEvent.NotificationType;

public class Notification implements INotification {
	private static final INotification INSTANCE = new JsNotification();

	private Notification() {

	}

	public static INotification get() {
		return INSTANCE;
	}

	@Override
	public void reportError(String message) {
		GwtEventBus.bus.fireEvent(new NotificationEvent(message, NotificationType.ERROR));
	}

	@Override
	public void reportError(Throwable caught) {
		if (caught instanceof ServerFault) {
			ServerFault sf = (ServerFault) caught;
			if (sf.getCode() == ErrorCode.FORBIDDEN) {
				Window.Location.assign("/login/index.html?askedUri=" + URL.encode(Window.Location.getPath()));
				return;
			}
		}

		GwtEventBus.bus.fireEvent(new NotificationEvent(caught));
	}

	@Override
	public void reportInfo(String string) {
		GwtEventBus.bus.fireEvent(new NotificationEvent(string, NotificationType.INFO));
	}

	public static void exportNotificationFunction() {
		exportNotificationFunction(new Notification());
	}

	private static native void exportNotificationFunction(INotification notification)
	/*-{
		$wnd.showErrorMessage = function(message) {
		return notification.@net.bluemind.gwtconsoleapp.base.notification.INotification::reportError(Ljava/lang/String;) (message);
		};
		$wnd.showInfoMessage = function(message) {
		return notification.@net.bluemind.gwtconsoleapp.base.notification.INotification::reportInfo(Ljava/lang/String;) (message);
		};
	}-*/;

}
