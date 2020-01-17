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
import static org.junit.Assert.assertNull;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.junit.Test;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.xmpp.coresession.internal.XmppSessionMessage;

public class XmppSessionTests extends BaseXmppTests {

	@Test
	public void testXmppConnOk() throws Exception {
		String sessionId = login(user1);

		assertNotNull(sessionId);

		eventBus.consumer("xmpp/session/" + sessionId, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("session", event.body());
			}
		});
		eventBus.request("xmpp/sessions-manager:open",
				new JsonObject().put("sessionId", sessionId).put("latd", user1.login + "@" + domainName),
				new Handler<AsyncResult<Message<Void>>>() {

					@Override
					public void handle(AsyncResult<Message<Void>> event) {
						queueAssertValue("conn", event.result().body());
					}
				});

		assertNull(waitAssert("conn"));
		JsonObject sessionObject = waitAssert("session");
		assertNotNull(sessionObject);
	}

	@Test
	public void testXmppConnNoTimeout() throws Exception {
		String sessionId = login(user1);

		eventBus.consumer("xmpp/session/" + sessionId, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("session", event.body());
			}
		});

		eventBus.consumer("xmpp/session/" + sessionId + "/ping", new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				event.reply("yeah");
			}
		});

		eventBus.request("xmpp/sessions-manager:open",
				new JsonObject().put("sessionId", sessionId).put("latd", user1.login + "@" + domainName),
				new Handler<AsyncResult<Message<Void>>>() {

					@Override
					public void handle(AsyncResult<Message<Void>> event) {
						queueAssertValue("conn", event.result().body());
					}
				});

		assertNull(waitAssert("conn"));
		JsonObject sessionObject = waitAssert("session");
		assertNotNull(sessionObject);

		Thread.sleep(2000);

		sessionObject = waitAssert("session");
		assertNull(sessionObject);

		Thread.sleep(2000);

		sessionObject = waitAssert("session");
		assertNull(sessionObject);

	}

	@Test
	public void testXmppConnNotOk() throws Exception {
		String sessionId = "badId";
		eventBus.request("xmpp/sessions-manager:open",
				new JsonObject().put("sessionId", sessionId).put("latd", "fakeuser@bm.lan"),
				new Handler<AsyncResult<Message<Void>>>() {

					@Override
					public void handle(AsyncResult<Message<Void>> event) {
						queueAssertValue("conn", event.result().body());
					}
				});

		assertNotNull(waitAssert("conn"));
	}

	@Test
	public void testDisconnect() throws Exception {
		String sessionId = login(user1);
		initiateConnection(user1, sessionId);
		eventBus.consumer("xmpp/session/" + sessionId, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("session", event.body());
			}
		});

		eventBus.send("xmpp/session/" + sessionId + ":close", "GG");

		JsonObject state = null;
		assertNotNull(state = waitAssert("session"));
		assertNotNull(state.getJsonObject("presence"));
		assertEquals(Presence.Type.unavailable.name(), state.getJsonObject("presence").getString("type"));

	}

	@Test
	public void testChangePresence() throws Exception {
		String sessionId = login(user1);

		initiateConnection(user1, sessionId);

		eventBus.consumer("xmpp/session/" + sessionId, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("session", event.body());
			}
		});

		eventBus.send("xmpp/session/" + sessionId + ":presence",
				new JsonObject().put("status", "Pas là").put("mode", Mode.dnd.name()));

		JsonObject state = null;
		assertNotNull(state = waitAssert("session"));
		assertNotNull(state.getJsonObject("presence"));
		assertEquals("Pas là", state.getJsonObject("presence").getString("status"));
		assertEquals(Mode.dnd.name(), state.getJsonObject("presence").getString("mode"));
	}

	@Test
	public void testAutoSubscription() throws Exception {

		String sessionId = login(user1);
		initiateConnection(user1, sessionId);

		String sessionId2 = login(user2);
		initiateConnection(user2, sessionId2);

		eventBus.consumer("xmpp/session/" + sessionId, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("session", event.body());
			}
		});

		eventBus.send("xmpp/session/" + sessionId2 + "/roster:add-buddy",
				new JsonObject().put("user", user1.login + "@" + domainName));

		JsonObject state = null;
		assertNotNull(state = waitAssert("session"));
		assertEquals("presence", state.getString("category"));
		assertEquals("subscribe", state.getString("action"));
		assertNotNull(state.getJsonObject("body"));
		assertEquals(user2.login + "@" + domainName, state.getJsonObject("body").getString("from"));
	}

	@Test
	public void testAskSubscribe() throws Exception {

		String sessionId = login(user1);
		initiateConnection(user1, sessionId);

		String sessionId2 = login(user2);
		initiateConnection(user2, sessionId2);

		eventBus.consumer("xmpp/session/" + sessionId, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("session", event.body());
			}
		});

		eventBus.send("xmpp/session/" + sessionId2 + ":ask-subscribe",
				new JsonObject().put("to", user1.login + "@" + domainName));

		JsonObject state = null;
		assertNotNull(state = waitAssert("session"));
		assertEquals("presence", state.getString("category"));
		assertEquals("subscribe", state.getString("action"));
		assertNotNull(state.getJsonObject("body"));
		assertEquals(user2.login + "@" + domainName, state.getJsonObject("body").getString("from"));
	}

	@Test
	public void testAcceptSubscribe() {

		String sessionId = login(user1);
		initiateConnection(user1, sessionId);

		String sessionId2 = login(user2);
		initiateConnection(user2, sessionId2);

		eventBus.consumer("xmpp/session/" + sessionId, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("session", event.body());
			}
		});

		eventBus.consumer("xmpp/session/" + sessionId2 + "/roster", new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("rosterEvent", event.body());
			}
		});
		eventBus.send("xmpp/session/" + sessionId2 + "/roster:add-buddy",
				new JsonObject().put("user", user1.login + "@" + domainName));

		assertNotNull(waitAssert("session"));

		// now we can accept sub
		eventBus.request("xmpp/session/" + sessionId + ":accept-subscribe",
				new JsonObject().put("to", user2.login + "@" + domainName),
				new Handler<AsyncResult<Message<JsonObject>>>() {

					@Override
					public void handle(AsyncResult<Message<JsonObject>> event) {
						queueAssertValue("sub", event.result().body());
					}
				});

		JsonObject resp = null;
		assertNotNull((resp = waitAssert("sub")));
		assertEquals(XmppSessionMessage.OK, resp.getInteger("status"));

		JsonObject rosterEvent = waitRosterAssert("rosterEvent", "presence");
		assertEquals("available", rosterEvent.getJsonObject("change").getString("subscription-type"));

	}

	@Test
	public void testDiscardSubscribe() throws Exception {

		String sessionId = login(user1);
		initiateConnection(user1, sessionId);

		String sessionId2 = login(user2);
		initiateConnection(user2, sessionId2);

		eventBus.consumer("xmpp/session/" + sessionId, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("session", event.body());
			}
		});

		eventBus.consumer("xmpp/session/" + sessionId2 + "/roster", new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("rosterEvent", event.body());
			}
		});
		eventBus.send("xmpp/session/" + sessionId2 + "/roster:add-buddy",
				new JsonObject().put("user", user1.login + "@" + domainName));

		// user2 asked subscribe to user1
		waitAssert("session");

		// now we can accept sub
		eventBus.request("xmpp/session/" + sessionId + ":accept-subscribe",
				new JsonObject().put("to", user2.login + "@" + domainName),
				new Handler<AsyncResult<Message<JsonObject>>>() {

					@Override
					public void handle(AsyncResult<Message<JsonObject>> event) {
						queueAssertValue("sub", event.result().body());
					}
				});

		// sub accepted
		waitAssert("sub");

		// now user1 should be available to user2
		// wait for roster of user2 update
		waitRosterAssert("rosterEvent", "presence");

		// now we can discard sub
		eventBus.request("xmpp/session/" + sessionId + ":discard-subscribe",
				new JsonObject().put("to", user2.login + "@" + domainName),
				new Handler<AsyncResult<Message<JsonObject>>>() {

					@Override
					public void handle(AsyncResult<Message<JsonObject>> event) {
						queueAssertValue("sub", event.result().body());
					}
				});

		// now user1 should be unavailable to user2
		// wait for roster of user2 update
		JsonObject rosterEvent = waitRosterAssert("rosterEvent", "entries-updated");
		assertEquals(user1.login + "@" + domainName,
				rosterEvent.getJsonObject("change").getJsonArray("entries").getValue(0));
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
