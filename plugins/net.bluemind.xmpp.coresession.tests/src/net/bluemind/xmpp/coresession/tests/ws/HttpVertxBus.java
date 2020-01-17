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
package net.bluemind.xmpp.coresession.tests.ws;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import org.asynchttpclient.AsyncHttpClient;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.WebSocket;

@SuppressWarnings("rawtypes")
public class HttpVertxBus {

	private WebSocket webSocket;

	private final ConcurrentMap<String, Handler<? extends Message>> handlerMap = new ConcurrentHashMap<>();

	private class ReplyHandler<T> implements Handler<Message<T>> {

		private String replyAddress;
		private Handler<Message<T>> handler;

		public ReplyHandler(String replyAddress, Handler<Message<T>> handler) {
			this.handler = handler;
			this.replyAddress = replyAddress;
		}

		@Override
		public void handle(Message<T> event) {
			try {
				handler.handle(event);
			} finally {
				handlerMap.remove(replyAddress);
			}
		}

	}

	public HttpVertxBus(AsyncHttpClient client, String ws)
			throws InterruptedException, ExecutionException, IOException {

		// WebSocket webSocket = client
		// .prepareGet(ws)
		// .execute(
		// new WebSocketUpgradeHandler.Builder()
		// .addWebSocketListener(
		// new DefaultWebSocketListener() {
		//
		// @Override
		// public void onOpen(
		// WebSocket websocket) {
		// System.out
		// .println("socket open ");
		// }
		//
		// @Override
		// public void onError(Throwable t) {
		// t.printStackTrace();
		// }
		//
		// @Override
		// public void onClose(
		// WebSocket websocket) {
		// System.out
		// .println("socket closed ");
		// }
		//
		// @Override
		// public void onMessage(String message) {
		// System.err.println("message "
		// + message);
		// }
		//
		// @Override
		// public void onFragment(
		// String fragment,
		// boolean last) {
		// System.out
		// .println("message fragment "
		// + fragment);
		// }
		//
		// @Override
		// public void onMessage(byte[] message) {
		// JsonObject object = new JsonObject(
		// new String(message));
		//
		// System.err.println("message "
		// + object);
		//
		// String addr = object
		// .getString("address");
		// Handler<Message<Object>> handler = (Handler<Message<Object>>)
		// handlerMap
		// .get(addr);
		// if (handler != null) {
		// handler.handle(new MessageImpl<>(
		// true,
		// null,
		// object.getValue("body")));
		// }
		// }
		//
		// @Override
		// public void onFragment(
		// byte[] fragment,
		// boolean last) {
		// System.err.println("message "
		// + fragment);
		// }
		//
		// }).build()).get();
		//
		// this.webSocket = webSocket;

	}

	public void registerHandler(String addr, Handler<Message> handler) {
		//
		// handlerMap.put(addr, handler);
		// JsonObject msg = new JsonObject().putString("type", "register")
		// .putString("address", addr);
		//
		// webSocket.sendTextMessage(msg.toString());
	}

	public void send(String addr, Object value) {
		send(addr, value, null);
	}

	public <T> void send(String addr, Object value, Handler<Message<T>> replyHandler) {
		// JsonObject msg = new JsonObject().putString("type", "send")
		// .putString("address", addr).putValue("body", value);
		//
		// if (replyHandler != null) {
		// String replyAddr = UUID.randomUUID().toString();
		// handlerMap
		// .put(replyAddr, new ReplyHandler(replyAddr, replyHandler));
		// msg.putString("replyAddress", replyAddr);
		// }
		// webSocket.sendTextMessage(msg.toString());
	}

	public void close() {
		webSocket.close();
	}
}
