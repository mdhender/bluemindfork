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

import java.util.Collection;

import org.jivesoftware.smackx.muc.Occupant;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class MucSessionMessage {

	public static JsonObject participantsChanged(String room) {
		return new JsonObject().putString("category", "muc").putString("action", "participants").putString("room",
				room);
	}

	public static JsonObject participants(Collection<Occupant> participants) {

		JsonArray array = new JsonArray();
		for (Occupant occ : participants) {
			array.addString(occ.getNick());
		}

		return new JsonObject().putNumber("status", XmppSessionMessage.OK).putArray("participants", array);
	}

	public static JsonObject error(String message) {
		return new JsonObject().putNumber("status", XmppSessionMessage.KO).putString("message", message);
	}

	public static JsonObject ok() {
		return new JsonObject().putNumber("status", XmppSessionMessage.OK);
	}

	public static JsonObject message(String from, String body) {
		return new JsonObject().putString("category", "muc").putString("action", "message").putObject("body",
				new JsonObject().putString("from", from).putString("message", body));

	}

	public static JsonObject join(String room, String participant) {
		return new JsonObject().putString("category", "muc").putString("action", "join").putString("room", room)
				.putString("participant", participant);
	}

	public static JsonObject leave(String room, String participant) {
		return new JsonObject().putString("category", "muc").putString("action", "leave").putString("room", room)
				.putString("participant", participant);

	}
}
