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

import java.util.ArrayList;
import java.util.Collection;

import org.jivesoftware.smack.packet.Presence;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class RosterMessage {

	public static JsonObject error(String message) {
		return new JsonObject().put("status", XmppSessionMessage.KO).put("message", message);
	}

	public static JsonObject ok() {
		return new JsonObject().put("status", XmppSessionMessage.OK);
	}

	public static JsonObject presenceChanged(Presence presence) {
		JsonObject ret = rosterChanged("presence");
		JsonObject change = ret.getJsonObject("change");

		change.put("user", XmppSessionMessage.parseJabberId(presence.getFrom())).put("status", presence.getStatus());

		if (presence.getMode() != null) {
			change.put("mode", presence.getMode().name());
		}
		change.put("subscription-type", presence.getType().name());
		return ret;
	}

	public static JsonObject entriesUpdated(Collection<String> addresses) {
		JsonObject ret = rosterChanged("entries-updated");
		ret.getJsonObject("change").put("entries", array(addresses));
		return ret;
	}

	private static JsonArray array(Collection<String> str) {
		return new JsonArray(new ArrayList<>(str));
	}

	public static JsonObject entriesDeleted(Collection<String> addresses) {
		JsonObject ret = rosterChanged("entries-deleted");
		ret.getJsonObject("change").put("entries", array(addresses));
		return ret;
	}

	public static JsonObject entriesAdded(Collection<String> addresses) {
		JsonObject ret = rosterChanged("entries-added");
		ret.getJsonObject("change").put("entries", array(addresses));
		return ret;
	}

	private static JsonObject rosterChanged(String type) {
		return new JsonObject().put("category", "roster").put("action", "changed").put("change",
				new JsonObject().put("type", type));
	}
}
