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
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;

import net.bluemind.restbus.api.gwt.RestBus.OnlineListener;
import net.bluemind.restbus.api.gwt.RestBusImpl;
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
		initWidget();

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

	private static Element createBubble() {
		final Anchor a = new AutoAttachAnchor();
		a.getElement().setId("im-notifier");
		a.getElement().setClassName("fa fa-lg fa-comments");
		a.getElement().getStyle().setCursor(Cursor.POINTER);
		a.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				showIM();
			}
		});

		RestBusImpl.get().addListener(new OnlineListener() {

			@Override
			public void status(boolean online) {
				a.setVisible(online);
			}
		});

		return a.getElement();
	}

	private static native String initWidget()
	/*-{
    $wnd['bubbleCreator'] = function() {
      var el = @net.bluemind.ui.push.client.Push::createBubble()();
      return el;
    };

	}-*/;

	private static native void showIM()
	/*-{
    var p = window
        .open(
            '',
            'IM',
            'height=500, width=700, top=100, left=100, toolbar=no, menubar=no, location=no, resizable=yes, scrollbars=no, status=no');
    if (p.location == 'about:blank') {
      p.location.href = '../im/#';
      p.focus();
    } else {
      p.focus();
    }
    localStorage.removeItem('bm-unread');
    return false;
	}-*/;
}
