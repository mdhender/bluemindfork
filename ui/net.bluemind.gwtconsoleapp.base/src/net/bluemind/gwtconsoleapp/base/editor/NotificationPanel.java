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
package net.bluemind.gwtconsoleapp.base.editor;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Label;

public class NotificationPanel {
	private Label lbl;
	private Timer delay;
	final int VISIBLE_DELAY = 3000;

	public NotificationPanel() {
		super();
	}

	private final native void addDetails(NativeEvent evt, String type, String message)
	/*-{
    evt['detail'] = {};
    evt['detail']['type'] = type;
    evt['detail']['message'] = message;
	}-*/;

	public void showError(String msg) {
		NativeEvent evt = Document.get().createHtmlEvent("ui-notification", true, true);
		addDetails(evt, "error", msg);
		Document.get().getBody().dispatchEvent(evt);
	}

	public void showOk(String msg) {
		NativeEvent evt = Document.get().createHtmlEvent("ui-notification", true, true);
		addDetails(evt, "info", msg);
		Document.get().getBody().dispatchEvent(evt);
	}

}
