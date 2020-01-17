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
package net.bluemind.xmpp.coresession.internal;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.Presence.Type;

import io.vertx.core.json.JsonObject;

public class XmppSessionMessage {

	public static final Number OK = 0;
	public static final Number KO = 1;

	public static JsonObject sessionOk() {
		return new JsonObject().put("status", OK).put("message", "session initialized");
	}

	public static JsonObject sessionConnectionFailed() {
		return new JsonObject().put("status", KO).put("message", "connection to xmpp server failed");
	}

	public static JsonObject sessionAuthenticationFailed() {
		return new JsonObject().put("status", KO).put("message", "authentication to xmpp server failed");

	}

	public static JsonObject presence(Type type, String status, Mode mode) {
		JsonObject ret = new JsonObject().put("type", type.name());
		if (status != null) {
			ret.put("status", status);
		}

		if (mode != null) {
			ret.put("mode", mode.name());
		}
		return ret;
	}

	public static JsonObject chatCreationOk(Chat chat) {
		return new JsonObject().put("status", OK).put("category", "chat")
				.put("from", parseJabberId(chat.getParticipant())).put("threadId", chat.getThreadID());
	}

	public static JsonObject chatCreationFailed() {
		return new JsonObject().put("status", KO).put("category", "chat");
	}

	public static JsonObject message(String thread, String from, String message) {
		return new JsonObject().put("status", OK).put("category", "message").put("threadId", thread)
				.put("from", parseJabberId(from)).put("body", message);
	}

	public static JsonObject messageNotification(String thread, String from, String pic, String message) {
		return new JsonObject().put("status", OK).put("category", "message").put("threadId", thread).put("from", from)
				.put("pic", pic).put("body", message);
	}

	public static JsonObject blinkNotification() {
		return new JsonObject().put("status", OK).put("category", "blink");
	}

	public static JsonObject mucCreationOk(String roomName) {
		return new JsonObject().put("status", OK).put("roomName", roomName);
	}

	public static JsonObject mucCreationFailed(String message) {
		return new JsonObject().put("status", KO).put("message", message);
	}

	public static JsonObject mucInvitation(String room, String inviter, String reason) {

		JsonObject message = new JsonObject();
		message.put("category", "muc");
		message.put("action", "invite");
		message.put("body", //
				new JsonObject() //
						.put("room", room) //
						.put("inviter", inviter) //
						.put("reason", reason));

		return message;
	}

	public static JsonObject mucInvitationNotification(String room, String inviter, String pic, String reason) {

		JsonObject message = new JsonObject();
		message.put("category", "muc");
		message.put("action", "invite");
		message.put("body", //
				new JsonObject() //
						.put("room", room) //
						.put("inviter", inviter) //
						.put("pic", pic) //
						.put("reason", reason));

		return message;
	}

	public static JsonObject mucJoinOk() {
		return new JsonObject().put("status", OK);
	}

	public static JsonObject ok() {
		return new JsonObject().put("status", OK);
	}

	public static JsonObject error(String message) {
		return new JsonObject().put("status", XmppSessionMessage.KO).put("message", message);
	}

	public static JsonObject ping() {
		return new JsonObject() //
				.put("category", "session") //
				.put("action", "ping");

	}

	public static JsonObject markAllAsRead() {
		return new JsonObject().put("status", OK).put("category", "mark-all-as-read");
	}

	/**
	 * Fetch localpart from jabberId localpart/resource
	 * 
	 * @param jabberId
	 * @return
	 */
	public static String parseJabberId(String jabberId) {
		int idx = jabberId.indexOf("/");

		if (idx > 0) {
			return jabberId.substring(0, idx);
		}

		return jabberId;
	}

	public static Object errorMessage(String thread, String message) {
		return new JsonObject().put("status", OK).put("category", "error").put("threadId", thread).put("body", message);
	}

}
