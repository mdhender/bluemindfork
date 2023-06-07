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
package net.bluemind.ui.push.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.user.client.Window;

import net.bluemind.ui.push.client.internal.PushSetup;

public class Push implements EntryPoint {

	public void onModuleLoad() {
		try {
			protectedModuleLoad();
		} catch (Exception e) {
			GWT.log("error during push module initialisation");
		}

	}

	private void protectedModuleLoad() {

		GWT.runAsync(new RunAsyncCallback() {

			@Override
			public void onFailure(Throwable reason) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onSuccess() {
				initQueue();
			}
		});
	}

	private void initQueue() {
		String queue = getQueue();
		if (queue != null) {
			PushSetup.forUser(queue);
		}
	}

	private String getQueue() {
		String queue = Window.Location.getParameter("pushQueue");
		if (queue == null) {
			queue = getQueueFromPage();
		}
		return queue;
	}

	private static native String getQueueFromPage()
	/*-{
	return "client.session." + $wnd.bmcSessionInfos['sid'];
	}-*/;

	private static native String consoleLog(String str)
	/*-{
	$wnd.console.log(str);
	}-*/;
}
