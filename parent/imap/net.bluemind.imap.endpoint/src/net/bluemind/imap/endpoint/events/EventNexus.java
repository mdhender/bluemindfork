/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.imap.endpoint.events;

import java.util.LinkedList;
import java.util.List;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import net.bluemind.imap.endpoint.SessionState;
import net.bluemind.imap.endpoint.driver.MailboxConnection;

public class EventNexus {

	private final EventBus eb;
	private List<MessageConsumer<?>> resources;
	private String id;

	public EventNexus(String id, EventBus eb) {
		this.id = id;
		this.eb = eb;
		resources = new LinkedList<>();
	}

	public void close() {
		resources.removeIf(cons -> {
			cons.unregister();
			return true;
		});
	}

	public void addStateListener(StateChangeListener scl) {
		MessageConsumer<String> cons = eb.consumer(id + ".imap.ep.state", (Message<String> state) -> {
			SessionState parsedState = SessionState.valueOf(state.body());
			scl.stateChanged(parsedState);
		});
		resources.add(cons);
	}

	public void dispatchStateChanged(SessionState authenticated) {
		eb.publish(id + ".imap.ep.state", authenticated.name());
	}

	public void addSequenceChangeListener(MailboxConnection mc, SequencesChangeListener scl) {
		MessageConsumer<JsonObject> cons = eb.consumer(mc.login() + ".imap.ep.size", (Message<JsonObject> state) -> {
			JsonObject body = state.body();
			// ignore notifications from self
			if (!id.equals(body.getString("fromConnection"))) {
				scl.markDirty(body.getString("fromTag"), body.getString("changedFolder"), body.getLong("version"));
			}
		});
		resources.add(cons);
	}

	public void dispatchSequencesChanged(MailboxConnection mc, String fromTag, String folderUid, long newVersion) {
		JsonObject payload = new JsonObject()//
				.put("fromTag", fromTag)//
				.put("fromConnection", id)//
				.put("changedFolder", folderUid)//
				.put("version", newVersion);

		eb.publish(mc.login() + ".imap.ep.size", payload);
	}

}
