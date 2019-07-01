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
package net.bluemind.gwtconsoleapp.base.eventbus;

import com.google.gwt.event.shared.GwtEvent;

public class NotificationEvent extends GwtEvent<NotificationEventHandler> {
	public final String message;
	public final NotificationType notificationType;
	public Throwable exception;

	public NotificationEvent(String message, NotificationType notificationType) {
		this.message = message;
		this.notificationType = notificationType;
	}

	public NotificationEvent(Throwable exception) {
		this.message = exception.getMessage();
		this.notificationType = NotificationType.EXCEPTION;
		this.exception = exception;
	}

	public static Type<NotificationEventHandler> TYPE = new Type<NotificationEventHandler>();

	@Override
	public Type<NotificationEventHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(NotificationEventHandler handler) {
		handler.onNotify(this);
	}

	public static enum NotificationType {
		INFO, ERROR, EXCEPTION
	}
}
