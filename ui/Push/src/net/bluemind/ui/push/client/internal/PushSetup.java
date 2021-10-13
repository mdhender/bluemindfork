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
package net.bluemind.ui.push.client.internal;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.restbus.api.gwt.GwtRestRequest;
import net.bluemind.restbus.api.gwt.GwtRestResponse;
import net.bluemind.restbus.api.gwt.RestBus.OnlineListener;
import net.bluemind.restbus.api.gwt.RestBusImpl;

public class PushSetup {

	private static ConnectionHandler connectionHandler;

	/**
	 * @param queue
	 */
	public static void forUser(final String queue) {
		connectionHandler = new ConnectionHandler();
		initConnection(queue);
	}

	/**
	 * @param queue
	 */
	public static void initConnection(final String queue) {
		final String sid = getSidFromPage();
		final String mailboxUid = getUidFromPage();
		final String login = getLogin();

		OnlineListener cl = new OnlineListener() {

			public void onOpen() {
                setMailNotificationHandler(mailboxUid);

				// open session
				JSONObject message = new JSONObject();
				message.put("sessionId", new JSONString(sid));
				message.put("latd", new JSONString(login));

				if (hasIM()) {
					register("xmpp/session/" + sid + "/notification", new XmppNotificationHandler());

					register("xmpp/muc/" + sid + "/notification", new MucNotificationHandler());

					send("xmpp/sessions-manager:open", JsonUtils.safeEval(message.toString()),
							new AsyncHandler<JavaScriptObject>() {

								@Override
								public void success(JavaScriptObject value) {

									JSONObject msg = new JSONObject();
									msg.put("origin", new JSONString("push"));

									send("xmpp/session/" + sid + ":unread", JsonUtils.safeEval(msg.toString()));

									send("xmpp/muc/" + sid + ":pending", JsonUtils.safeEval(msg.toString()));

									connectionHandler.open();
								}

								@Override
								public void failure(Throwable e) {
								}
							});
				}
			}

			public void onClose() {
				GWT.log("close !!");
				connectionHandler.close();

			}

			@Override
			public void status(boolean connected) {
				GWT.log("changed !!" + connected);
				if (connected) {
					onOpen();
				} else {
					onClose();
				}

			}
		};

		RestBusImpl.get().addListener(cl);

	}

    // register MailNotificationHandler only if new notifications are not active
    private static native String setMailNotificationHandler(String mailboxUid) /*-{
                    if (!$wnd.bundles.hasOwnProperty("net.bluemind.webmodules.webapp.wrapper")) {
                        @net.bluemind.ui.push.client.internal.PushSetup::register(Ljava/lang/String;Lnet/bluemind/ui/push/client/internal/MessageHandler;)(mailboxUid + ".notifications.mails", @net.bluemind.ui.push.client.internal.MailNotificationHandler::new()());
                    } else {
                        $wnd.bundleResolve("net.bluemind.webmodules.webapp.wrapper", function() {
                            if (!$wnd.WebApp || $wnd.WebApp.hasNotifWhenReceivingMail !== true) {
                                @net.bluemind.ui.push.client.internal.PushSetup::register(Ljava/lang/String;Lnet/bluemind/ui/push/client/internal/MessageHandler;)(mailboxUid + ".notifications.mails", @net.bluemind.ui.push.client.internal.MailNotificationHandler::new()());
                            }
                        });
                    }
                }-*/;

	private static native String getSidFromPage() /*-{
													return $wnd.bmcSessionInfos['sid'];
													}-*/;

	private static native String getUidFromPage() /*-{
													return $wnd.bmcSessionInfos['userId'];
													}-*/;

	private static native String getLogin() /*-{
											return $wnd.bmcSessionInfos['defaultEmail'];
											}-*/;

	private static native boolean hasIM() /*-{
											return $wnd.bmcSessionInfos['roles'].includes('hasIM');
											}-*/;

	private static <T extends JavaScriptObject> void register(String path, MessageHandler<T> xmppMessageHandler) {
		RestBusImpl.get().sendMessage(GwtRestRequest.create(getSidFromPage(), "register", path, null),
				new AsyncHandler<GwtRestResponse>() {

					@Override
					public void success(GwtRestResponse value) {
						JavaScriptObject body = value.getBody();
						if (body != null) {
							xmppMessageHandler.onMessage(body.cast());
						} else {
							xmppMessageHandler.onMessage(null);
						}
					}

					@Override
					public void failure(Throwable e) {
					}

				});
	}

	public static void send(String path, JavaScriptObject message) {
		RestBusImpl.get().sendMessage(GwtRestRequest.create(getSidFromPage(), "event", path, message),
				new AsyncHandler<GwtRestResponse>() {

					@Override
					public void success(GwtRestResponse value) {
						// TODO Auto-generated method stub

					}

					@Override
					public void failure(Throwable e) {
						// TODO Auto-generated method stub

					}

				});
	}

	public static void send(String path, JavaScriptObject message, AsyncHandler<JavaScriptObject> handler) {
		RestBusImpl.get().sendMessage(GwtRestRequest.create(getSidFromPage(), "event", path, message),
				new AsyncHandler<GwtRestResponse>() {

					@Override
					public void success(GwtRestResponse value) {
						handler.success(value.getBody());

					}

					@Override
					public void failure(Throwable e) {
						// TODO Auto-generated method stub

					}

				});
	}
}
