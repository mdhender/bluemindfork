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
package net.bluemind.ui.im.client.push;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.restbus.api.gwt.GwtRestRequest;
import net.bluemind.restbus.api.gwt.GwtRestResponse;
import net.bluemind.restbus.api.gwt.RestBus;
import net.bluemind.restbus.api.gwt.RestBus.OnlineListener;
import net.bluemind.restbus.api.gwt.RestBusImpl;
import net.bluemind.ui.im.client.IMCtrl;
import net.bluemind.ui.im.client.push.handler.MucMessageHandler;
import net.bluemind.ui.im.client.push.handler.MucNotificationHandler;
import net.bluemind.ui.im.client.push.handler.RosterEntriesMessageHandler;
import net.bluemind.ui.im.client.push.handler.RosterMessageHandler;
import net.bluemind.ui.im.client.push.handler.XivoMessageHandler;
import net.bluemind.ui.im.client.push.handler.XmppErrorHandler;
import net.bluemind.ui.im.client.push.handler.XmppMessageHandler;
import net.bluemind.ui.im.client.push.handler.XmppNotificationHandler;

public class Push {

	/**
	 * @param queue
	 */
	public static void setup() {
		initConnection();
	}

	/**
	 * @param queue
	 */
	public static void initConnection() {

		final String sid = getSidFromPage();
		final String login = getLogin();

		OnlineListener cl = new OnlineListener() {

			@Override
			public void status(boolean online) {
				if (online) {
					onOpen();
				} else {
					onClose();
				}
			}

			public void onOpen() {
				RestBus bus = RestBusImpl.get();

				JSONObject message = new JSONObject();
				message.put("sessionId", new JSONString(sid));
				message.put("latd", new JSONString(login));

				bus.sendMessage(GwtRestRequest.create(sid, "event", "xmpp/sessions-manager:open",
						message.getJavaScriptObject()), new AsyncHandler<GwtRestResponse>() {

							@Override
							public void success(GwtRestResponse message) {
								entries();
								register("xmpp/session/" + sid, new XmppMessageHandler());

								register("xmpp/xivo/status", new XivoMessageHandler());

								register("xmpp/session/" + sid + "/roster", new RosterMessageHandler());

								register("xmpp/muc/" + sid, new MucMessageHandler());

								// notifications
								register("xmpp/session/" + sid + "/notification", new XmppNotificationHandler());

								register("xmpp/muc/" + sid + "/notification", new MucNotificationHandler());

								// unread handlers
								register("xmpp/session/" + sid + "/unread", new XmppMessageHandler());
								register("xmpp/muc/" + sid + "/pending", new MucMessageHandler());

								// error handler
								register("xmpp/session/" + sid + "/error", new XmppErrorHandler());

								IMCtrl.getInstance().open();

								// load unread stuff
								IMCtrl.getInstance().loadUnreadMessage();
								IMCtrl.getInstance().loadPendingMuc();

								// fetch my own presence
								IMCtrl.getInstance().loadPresence();
							}

							@Override
							public void failure(Throwable e) {
								// TODO Auto-generated method stub

							}
						});
			}

			public void onClose() {
				IMCtrl.getInstance().close();
			}

		};

		RestBusImpl.get().addListener(cl);
	}

	public static void entries() {
		GwtRestRequest request = GwtRestRequest.create(getSidFromPage(), "event",
				"xmpp/session/" + getSidFromPage() + "/roster:entries", new JSONObject().getJavaScriptObject());
		RestBusImpl.get().sendMessage(request, new AsyncHandler<GwtRestResponse>() {

			@Override
			public void success(GwtRestResponse value) {
				new RosterEntriesMessageHandler().onMessage(value.getBody());
			}

			@Override
			public void failure(Throwable e) {

			}

		});
	}

	public static native String getSidFromPage() /*-{
    return $wnd.bmcSessionInfos['sid'];
	}-*/;

	private static native String getLogin() /*-{
    return $wnd.bmcSessionInfos['defaultEmail'];
	}-*/;

	private static <T extends JavaScriptObject> void register(String path, MessageHandler<T> xmppMessageHandler) {
		RestBusImpl.get().sendMessage(GwtRestRequest.create(getSidFromPage(), "register", path, null),
				new AsyncHandler<GwtRestResponse>() {

					@Override
					public void success(GwtRestResponse value) {
						JavaScriptObject body = value.getBody();
						if (body != null) {
							xmppMessageHandler.onMessage(body.<T> cast());
						} else {
							xmppMessageHandler.onMessage(null);
						}
					}

					@Override
					public void failure(Throwable e) {
						// TODO Auto-generated method stub

					}

				});
	}

	public static void send(String path, JSONObject message) {
		RestBusImpl.get().sendMessage(
				GwtRestRequest.create(getSidFromPage(), "event", path, message.getJavaScriptObject()), null);
	}

	public static <T extends JavaScriptObject> void send(String path, JSONObject message, MessageHandler<T> handler) {
		RestBusImpl.get().sendMessage(
				GwtRestRequest.create(getSidFromPage(), "event", path, message.getJavaScriptObject()),
				new AsyncHandler<GwtRestResponse>() {

					@Override
					public void success(GwtRestResponse value) {
						handler.onMessage(value.getBody().cast());

					}

					@Override
					public void failure(Throwable e) {
						// TODO Auto-generated method stub

					}

				});
	}
}
