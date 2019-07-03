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
import org.vertx.java.core.json.JsonObject;

public class XmppSessionMessage {

	public static final Number OK = 0;
	public static final Number KO = 1;

	public static JsonObject sessionOk() {
		return new JsonObject().putNumber("status", OK).putString("message", "session initialized");
	}

	public static JsonObject sessionConnectionFailed() {
		return new JsonObject().putNumber("status", KO).putString("message", "connection to xmpp server failed");
	}

	public static JsonObject sessionAuthenticationFailed() {
		return new JsonObject().putNumber("status", KO).putString("message", "authentication to xmpp server failed");

	}

	public static JsonObject presence(Type type, String status, Mode mode) {
		JsonObject ret = new JsonObject().putString("type", type.name());
		if (status != null) {
			ret.putString("status", status);
		}

		if (mode != null) {
			ret.putString("mode", mode.name());
		}
		return ret;
	}

	public static JsonObject chatCreationOk(Chat chat) {
		return new JsonObject().putNumber("status", OK).putString("category", "chat")
				.putString("from", parseJabberId(chat.getParticipant())).putString("threadId", chat.getThreadID());
	}

	public static JsonObject chatCreationFailed() {
		return new JsonObject().putNumber("status", KO).putString("category", "chat");
	}

	public static JsonObject message(String thread, String from, String message) {
		return new JsonObject().putNumber("status", OK).putString("category", "message").putString("threadId", thread)
				.putString("from", parseJabberId(from)).putString("body", message);
	}

	public static JsonObject messageNotification(String thread, String from, String pic, String message) {
		return new JsonObject().putNumber("status", OK).putString("category", "message").putString("threadId", thread)
				.putString("from", from).putString("pic", pic).putString("body", message);
	}

	public static JsonObject blinkNotification() {
		return new JsonObject().putNumber("status", OK).putString("category", "blink");
	}

	public static JsonObject mucCreationOk(String roomName) {
		return new JsonObject().putNumber("status", OK).putString("roomName", roomName);
	}

	public static JsonObject mucCreationFailed(String message) {
		return new JsonObject().putNumber("status", KO).putString("message", message);
	}

	public static JsonObject mucInvitation(String room, String inviter, String reason) {

		JsonObject message = new JsonObject();
		message.putString("category", "muc");
		message.putString("action", "invite");
		message.putObject("body", //
				new JsonObject() //
						.putString("room", room) //
						.putString("inviter", inviter) //
						.putString("reason", reason));

		return message;
	}

	public static JsonObject mucInvitationNotification(String room, String inviter, String pic, String reason) {

		JsonObject message = new JsonObject();
		message.putString("category", "muc");
		message.putString("action", "invite");
		message.putObject("body", //
				new JsonObject() //
						.putString("room", room) //
						.putString("inviter", inviter) //
						.putString("pic", pic) //
						.putString("reason", reason));

		return message;
	}

	public static JsonObject mucJoinOk() {
		return new JsonObject().putNumber("status", OK);
	}

	public static JsonObject ok() {
		return new JsonObject().putNumber("status", OK);
	}

	public static JsonObject error(String message) {
		return new JsonObject().putNumber("status", XmppSessionMessage.KO).putString("message", message);
	}

	public static JsonObject ping() {
		return new JsonObject() //
				.putString("category", "session") //
				.putString("action", "ping");

	}

	public static JsonObject markAllAsRead() {
		return new JsonObject().putNumber("status", OK).putString("category", "mark-all-as-read");
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
		return new JsonObject().putNumber("status", OK).putString("category", "error").putString("threadId", thread)
				.putString("body", message);
	}

}
