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
package net.bluemind.ui.im.client.push.message;

import com.google.gwt.core.client.JsArrayString;

public class RosterMessage extends XmppMessage {
	protected RosterMessage() {

	}

	public final native String getType() /*-{
		return this.change.type;
	}-*/;

	public final native JsArrayString getEntries() /*-{
		return this.change.entries;
	}-*/;

	public final native String getUser() /*-{
		return this.change.user;
	}-*/;

	public final native String getName() /*-{
		return this.change.name;
	}-*/;

	public final native String getMode() /*-{
		return this.change.mode;
	}-*/;

	public final native String getPresenceStatus() /*-{
		return this.change.status;
	}-*/;

	public final native String getSubscriptionType() /*-{
		return this.change['subscription-type'];
	}-*/;

	public final native String getSubs() /*-{
		return this.change.subs;
	}-*/;

}
