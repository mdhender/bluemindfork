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
package net.bluemind.ui.push.client.internal.notification;

import net.bluemind.ui.push.client.internal.resources.Icons;

public class ReminderNotification extends ServerNotification {

	protected ReminderNotification() {
	}

	public final native String getId() /*-{
		return this.id;
	}-*/;

	public final native String getAlert() /*-{
		return this.alert;
	}-*/;

	public final String getIco() {
		return Icons.INST.reminder().getSafeUri().asString();
	}

}
