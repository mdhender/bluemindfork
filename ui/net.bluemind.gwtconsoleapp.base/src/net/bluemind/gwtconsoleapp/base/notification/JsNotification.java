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

public class JsNotification implements INotification {

	@Override
	public native void reportError(String message)
	/*-{
    $wnd.showErrorMessage(message);
	}-*/;

	@Override
	public void reportError(Throwable caught) {
		if (caught instanceof ServerFault) {
			ServerFault sf = (ServerFault) caught;
			if (sf.getCode() == ErrorCode.FORBIDDEN) {
				Window.Location.assign("/login/index.html?askedUri=" + URL.encode(Window.Location.getPath()));
				return;
			}
		}

		reportError(caught.getMessage());
	}

	@Override
	public native void reportInfo(String message)
	/*-{
    $wnd.showInfoMessage(message);
	}-*/;
}
