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

import org.jivesoftware.smack.packet.Presence;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class RosterMessage {

	public static JsonObject error(String message) {
		return new JsonObject().putNumber("status", XmppSessionMessage.KO).putString("message", message);
	}

	public static JsonObject ok() {
		return new JsonObject().putNumber("status", XmppSessionMessage.OK);
	}

	public static JsonObject presenceChanged(Presence presence) {
		JsonObject ret = rosterChanged("presence");
		JsonObject change = ret.getObject("change");

		change.putString("user", XmppSessionMessage.parseJabberId(presence.getFrom())).putString("status",
				presence.getStatus());

		if (presence.getMode() != null) {
			change.putString("mode", presence.getMode().name());
		}
		change.putString("subscription-type", presence.getType().name());
		return ret;
	}

	public static JsonObject entriesUpdated(Collection<String> addresses) {
		JsonObject ret = rosterChanged("entries-updated");
		ret.getObject("change").putArray("entries", new JsonArray(addresses.toArray()));
		return ret;
	}

	public static JsonObject entriesDeleted(Collection<String> addresses) {
		JsonObject ret = rosterChanged("entries-deleted");
		ret.getObject("change").putArray("entries", new JsonArray(addresses.toArray()));
		return ret;
	}

	public static JsonObject entriesAdded(Collection<String> addresses) {
		JsonObject ret = rosterChanged("entries-added");
		ret.getObject("change").putArray("entries", new JsonArray(addresses.toArray()));
		return ret;
	}

	private static JsonObject rosterChanged(String type) {
		return new JsonObject().putString("category", "roster").putString("action", "changed").putObject("change",
				new JsonObject().putString("type", type));
	}
}
