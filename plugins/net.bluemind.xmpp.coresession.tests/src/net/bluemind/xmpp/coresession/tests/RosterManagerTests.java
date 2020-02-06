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

import java.util.Arrays;

import org.junit.Test;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class RosterManagerTests extends BaseXmppTests {

	@Test
	public void testAddBuddy() {
		String sessionId = login(user1);
		initiateConnection(user1, sessionId);

		String addr = "xmpp/session/" + sessionId + "/roster";

		eventBus.consumer(addr, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("rosterEvent", event.body());
			}
		});

		eventBus.request(addr + ":add-buddy", new JsonObject().put("user", user2.login + "@" + domainName),
				new Handler<AsyncResult<Message<JsonObject>>>() {

					@Override
					public void handle(AsyncResult<Message<JsonObject>> response) {
						queueAssertValue("roster", response.result().body());
					}
				});

		waitAssert("roster");
		JsonObject rosterEvent = waitRosterAssert("rosterEvent", "entries-added");

		assertEquals(1, rosterEvent.getJsonObject("change").getJsonArray("entries").size());

		assertEquals(user2.login + "@" + domainName,
				rosterEvent.getJsonObject("change").getJsonArray("entries").getValue(0));

		// after entries-added, we should receive entries-updated

	}

	@Test
	public void testRemoveBuddy() {

		String sessionId = login(user1);
		initiateConnection(user1, sessionId);

		String addr = "xmpp/session/" + sessionId + "/roster";

		eventBus.consumer(addr, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("rosterEvent", event.body());
			}
		});

		// add
		eventBus.request(addr + ":add-buddy", new JsonObject().put("user", user2.login + "@" + domainName),
				new Handler<AsyncResult<Message<JsonObject>>>() {

					@Override
					public void handle(AsyncResult<Message<JsonObject>> response) {
						queueAssertValue("roster", response.result().body());
					}
				});
		waitAssert("roster");

		// remove
		eventBus.request(addr + ":remove-buddy", new JsonObject().put("user", user2.login + "@" + domainName),
				new Handler<AsyncResult<Message<JsonObject>>>() {

					@Override
					public void handle(AsyncResult<Message<JsonObject>> response) {
						queueAssertValue("roster", response.result().body());
					}
				});

		JsonObject rosterEvent = waitRosterAssert("rosterEvent", "entries-deleted");

		assertEquals(1, rosterEvent.getJsonObject("change").getJsonArray("entries").size());

		assertEquals(user2.login + "@" + domainName,
				rosterEvent.getJsonObject("change").getJsonArray("entries").getValue(0));

	}

	@Test
	public void testEntries() {

		String sessionId = login(user1);
		initiateConnection(user1, sessionId);

		String addr = "xmpp/session/" + sessionId + "/roster";

		eventBus.request(addr + ":entries", new JsonObject(), new Handler<AsyncResult<Message<JsonObject>>>() {

			@Override
			public void handle(AsyncResult<Message<JsonObject>> response) {
				queueAssertValue("roster", response.result().body());
			}
		});

		JsonObject jsonObject = waitAssert("roster");
		assertNotNull(jsonObject);
		assertNotNull(jsonObject.getJsonArray("entries"));
		assertEquals(0, jsonObject.getJsonArray("entries").size());

		// add
		eventBus.request(addr + ":add-buddy", new JsonObject().put("user", user2.login + "@" + domainName),
				new Handler<AsyncResult<Message<JsonObject>>>() {

					@Override
					public void handle(AsyncResult<Message<JsonObject>> response) {
						queueAssertValue("roster", response.result().body());
					}
				});
		waitAssert("roster");

		eventBus.request(addr + ":entries", new JsonObject(), new Handler<AsyncResult<Message<JsonObject>>>() {

			@Override
			public void handle(AsyncResult<Message<JsonObject>> response) {
				queueAssertValue("roster", response.result().body());
			}
		});

		jsonObject = waitAssert("roster");
		assertNotNull(jsonObject);
		assertNotNull(jsonObject.getJsonArray("entries"));
		assertEquals(1, jsonObject.getJsonArray("entries").size());

		// retrieve only one
		eventBus.request(addr + ":entries",
				new JsonObject().put("entries", new JsonArray(Arrays.asList(user2.login + "@" + domainName))),
				new Handler<AsyncResult<Message<JsonObject>>>() {

					@Override
					public void handle(AsyncResult<Message<JsonObject>> response) {
						queueAssertValue("roster", response.result().body());
					}
				});

		jsonObject = waitAssert("roster");
		assertNotNull(jsonObject);
		assertNotNull(jsonObject.getJsonArray("entries"));
		assertEquals(1, jsonObject.getJsonArray("entries").size());

	}

	@Test
	public void testRosterEvents() {
		String sessionId = login(user1);
		initiateConnection(user1, sessionId);

		String addr = "xmpp/session/" + sessionId + "/roster";

		eventBus.consumer(addr, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("event", event.body());
			}
		});
		eventBus.request(addr + ":add-buddy", new JsonObject().put("user", user2.login + "@" + domainName),
				new Handler<AsyncResult<Message<JsonObject>>>() {

					@Override
					public void handle(AsyncResult<Message<JsonObject>> response) {
						queueAssertValue("roster", response.result().body());
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
			assertNotNull(rosterEvent.getJsonObject("change"));
			if (type.equals(rosterEvent.getJsonObject("change").getString("type"))) {
				break;
			}
		}
		return rosterEvent;
	}
}
