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
package net.bluemind.xmpp.coresession.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class RosterManagerTests extends BaseXmppTests {

	@Test
	public void testAddBuddy() {
		String sessionId = login(user1);
		initiateConnection(user1, sessionId);

		String addr = "xmpp/session/" + sessionId + "/roster";

		eventBus.registerHandler(addr, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("rosterEvent", event.body());
			}
		});

		eventBus.send(addr + ":add-buddy", new JsonObject().putString("user", user2.login + "@" + domainName),
				new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> response) {
						queueAssertValue("roster", response.body());
					}
				});

		waitAssert("roster");
		JsonObject rosterEvent = waitRosterAssert("rosterEvent", "entries-added");

		assertEquals(1, rosterEvent.getObject("change").getArray("entries").size());

		assertEquals(user2.login + "@" + domainName, rosterEvent.getObject("change").getArray("entries").get(0));

		// after entries-added, we should receive entries-updated

	}

	@Test
	public void testRemoveBuddy() {

		String sessionId = login(user1);
		initiateConnection(user1, sessionId);

		String addr = "xmpp/session/" + sessionId + "/roster";

		eventBus.registerHandler(addr, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("rosterEvent", event.body());
			}
		});

		// add
		eventBus.send(addr + ":add-buddy", new JsonObject().putString("user", user2.login + "@" + domainName),
				new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> response) {
						queueAssertValue("roster", response.body());
					}
				});
		waitAssert("roster");

		// remove
		eventBus.send(addr + ":remove-buddy", new JsonObject().putString("user", user2.login + "@" + domainName),
				new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> response) {
						queueAssertValue("roster", response.body());
					}
				});

		JsonObject rosterEvent = waitRosterAssert("rosterEvent", "entries-deleted");

		assertEquals(1, rosterEvent.getObject("change").getArray("entries").size());

		assertEquals(user2.login + "@" + domainName, rosterEvent.getObject("change").getArray("entries").get(0));

	}

	@Test
	public void testEntries() {

		String sessionId = login(user1);
		initiateConnection(user1, sessionId);

		String addr = "xmpp/session/" + sessionId + "/roster";

		eventBus.send(addr + ":entries", new JsonObject(), new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> response) {
				queueAssertValue("roster", response.body());
			}
		});

		JsonObject jsonObject = waitAssert("roster");
		assertNotNull(jsonObject);
		assertNotNull(jsonObject.getArray("entries"));
		assertEquals(0, jsonObject.getArray("entries").size());

		// add
		eventBus.send(addr + ":add-buddy", new JsonObject().putString("user", user2.login + "@" + domainName),
				new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> response) {
						queueAssertValue("roster", response.body());
					}
				});
		waitAssert("roster");

		eventBus.send(addr + ":entries", new JsonObject(), new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> response) {
				queueAssertValue("roster", response.body());
			}
		});

		jsonObject = waitAssert("roster");
		assertNotNull(jsonObject);
		assertNotNull(jsonObject.getArray("entries"));
		assertEquals(1, jsonObject.getArray("entries").size());

		// retrieve only one
		eventBus.send(addr + ":entries",
				new JsonObject().putArray("entries", new JsonArray(new Object[] { user2.login + "@" + domainName })),
				new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> response) {
						queueAssertValue("roster", response.body());
					}
				});

		jsonObject = waitAssert("roster");
		assertNotNull(jsonObject);
		assertNotNull(jsonObject.getArray("entries"));
		assertEquals(1, jsonObject.getArray("entries").size());

	}

	@Test
	public void testRosterEvents() {
		String sessionId = login(user1);
		initiateConnection(user1, sessionId);

		String addr = "xmpp/session/" + sessionId + "/roster";

		eventBus.registerHandler(addr, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("event", event.body());
			}
		});
		eventBus.send(addr + ":add-buddy", new JsonObject().putString("user", user2.login + "@" + domainName),
				new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> response) {
						queueAssertValue("roster", response.body());
					}
				});

		JsonObject jsonObject = waitAssert("roster");
		assertNotNull(jsonObject);

		jsonObject = waitAssert("event");
		assertNotNull(jsonObject);

	}

	private JsonObject waitRosterAssert(String key, String type) {

		JsonObject rosterEvent = null;

		while (true) {
			rosterEvent = waitAssert(key);
			System.out.println(rosterEvent);
			assertNotNull(rosterEvent.getObject("change"));
			if (type.equals(rosterEvent.getObject("change").getString("type"))) {
				break;
			}
		}
		return rosterEvent;
	}
}
