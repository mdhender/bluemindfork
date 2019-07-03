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

public class MucNotification extends ServerNotification {

	protected MucNotification() {

	}

	public final native String getCategory() /*-{
		return this.category;
	}-*/;

	public final native void debug() /*-{
		console.log(this);
	}-*/;

	public final native String getAction() /*-{
		return this.action;
	}-*/;

	private final native String _getInviter() /*-{
		return this.body.inviter;
	}-*/;

	private final native String _getPic() /*-{
		return this.body.pic;
	}-*/;

	public final String getInviter() {
		// room@muc.bm.lan/user
		String from = _getInviter();
		int idx = from.indexOf("/");
		return from.substring(idx + 1, from.length());
	}

	public final native String getInvitationReason() /*-{
		return this.body.reason;
	}-*/;

	public final String getPic() {
		String pic = _getPic();
		if (pic == null || pic.isEmpty()) {
			pic = Icons.INST.chat().getSafeUri().asString();
		} else {
			pic = "data:image/jpg;base64," + pic;
		}

		return pic;
	}
}
