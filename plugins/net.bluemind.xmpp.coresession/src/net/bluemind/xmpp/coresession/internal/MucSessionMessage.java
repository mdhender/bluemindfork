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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class MucSessionMessage {

	public static JsonObject participantsChanged(String room) {
		return new JsonObject().put("category", "muc").put("action", "participants").put("room", room);
	}

	public static JsonObject participants(Collection<Occupant> participants) {

		JsonArray array = new JsonArray();
		for (Occupant occ : participants) {
			array.add(occ.getNick());
		}

		return new JsonObject().put("status", XmppSessionMessage.OK).put("participants", array);
	}

	public static JsonObject error(String message) {
		return new JsonObject().put("status", XmppSessionMessage.KO).put("message", message);
	}

	public static JsonObject ok() {
		return new JsonObject().put("status", XmppSessionMessage.OK);
	}

	public static JsonObject message(String from, String body) {
		return new JsonObject().put("category", "muc").put("action", "message").put("body",
				new JsonObject().put("from", from).put("message", body));

	}

	public static JsonObject join(String room, String participant) {
		return new JsonObject().put("category", "muc").put("action", "join").put("room", room).put("participant",
				participant);
	}

	public static JsonObject leave(String room, String participant) {
		return new JsonObject().put("category", "muc").put("action", "leave").put("room", room).put("participant",
				participant);

	}
}
