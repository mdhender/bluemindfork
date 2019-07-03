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
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.xmpp.coresession.internal.XmppSessionMessage;

public class XmppSessionTests extends BaseXmppTests {

	@Test
	public void testXmppConnOk() throws Exception {
		String sessionId = login(user1);

		assertNotNull(sessionId);

		eventBus.registerHandler("xmpp/session/" + sessionId, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("session", event.body());
			}
		});
		eventBus.send("xmpp/sessions-manager:open",
				new JsonObject().putString("sessionId", sessionId).putString("latd", user1.login + "@" + domainName),
				new Handler<Message<Void>>() {

					@Override
					public void handle(Message<Void> event) {
						queueAssertValue("conn", event.body());
					}
				});

		assertNull(waitAssert("conn"));
		JsonObject sessionObject = waitAssert("session");
		assertNotNull(sessionObject);
	}

	@Test
	public void testXmppConnNoTimeout() throws Exception {
		String sessionId = login(user1);

		eventBus.registerHandler("xmpp/session/" + sessionId, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("session", event.body());
			}
		});

		eventBus.registerHandler("xmpp/session/" + sessionId + "/ping", new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				event.reply();
			}
		});

		eventBus.send("xmpp/sessions-manager:open",
				new JsonObject().putString("sessionId", sessionId).putString("latd", user1.login + "@" + domainName),
				new Handler<Message<Void>>() {

					@Override
					public void handle(Message<Void> event) {
						queueAssertValue("conn", event.body());
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
		eventBus.send("xmpp/sessions-manager:open",
				new JsonObject().putString("sessionId", sessionId).putString("latd", "fakeuser@bm.lan"),
				new Handler<Message<Void>>() {

					@Override
					public void handle(Message<Void> event) {
						queueAssertValue("conn", event.body());
					}
				});

		assertNotNull(waitAssert("conn"));
	}

	@Test
	public void testDisconnect() throws Exception {
		String sessionId = login(user1);
		initiateConnection(user1, sessionId);
		eventBus.registerHandler("xmpp/session/" + sessionId, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("session", event.body());
			}
		});

		eventBus.send("xmpp/session/" + sessionId + ":close", "GG");

		JsonObject state = null;
		assertNotNull(state = waitAssert("session"));
		assertNotNull(state.getObject("presence"));
		assertEquals(Presence.Type.unavailable.name(), state.getObject("presence").getString("type"));

	}

	@Test
	public void testChangePresence() throws Exception {
		String sessionId = login(user1);

		initiateConnection(user1, sessionId);

		eventBus.registerHandler("xmpp/session/" + sessionId, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("session", event.body());
			}
		});

		eventBus.send("xmpp/session/" + sessionId + ":presence",
				new JsonObject().putString("status", "Pas là").putString("mode", Mode.dnd.name()));

		JsonObject state = null;
		assertNotNull(state = waitAssert("session"));
		assertNotNull(state.getObject("presence"));
		assertEquals("Pas là", state.getObject("presence").getString("status"));
		assertEquals(Mode.dnd.name(), state.getObject("presence").getString("mode"));
	}

	@Test
	public void testAutoSubscription() throws Exception {

		String sessionId = login(user1);
		initiateConnection(user1, sessionId);

		String sessionId2 = login(user2);
		initiateConnection(user2, sessionId2);

		eventBus.registerHandler("xmpp/session/" + sessionId, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("session", event.body());
			}
		});

		eventBus.send("xmpp/session/" + sessionId2 + "/roster:add-buddy",
				new JsonObject().putString("user", user1.login + "@" + domainName));

		JsonObject state = null;
		assertNotNull(state = waitAssert("session"));
		assertEquals("presence", state.getString("category"));
		assertEquals("subscribe", state.getString("action"));
		assertNotNull(state.getObject("body"));
		assertEquals(user2.login + "@" + domainName, state.getObject("body").getString("from"));
	}

	@Test
	public void testAskSubscribe() throws Exception {

		String sessionId = login(user1);
		initiateConnection(user1, sessionId);

		String sessionId2 = login(user2);
		initiateConnection(user2, sessionId2);

		eventBus.registerHandler("xmpp/session/" + sessionId, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("session", event.body());
			}
		});

		eventBus.send("xmpp/session/" + sessionId2 + ":ask-subscribe",
				new JsonObject().putString("to", user1.login + "@" + domainName));

		JsonObject state = null;
		assertNotNull(state = waitAssert("session"));
		assertEquals("presence", state.getString("category"));
		assertEquals("subscribe", state.getString("action"));
		assertNotNull(state.getObject("body"));
		assertEquals(user2.login + "@" + domainName, state.getObject("body").getString("from"));
	}

	@Test
	public void testAcceptSubscribe() {

		String sessionId = login(user1);
		initiateConnection(user1, sessionId);

		String sessionId2 = login(user2);
		initiateConnection(user2, sessionId2);

		eventBus.registerHandler("xmpp/session/" + sessionId, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("session", event.body());
			}
		});

		eventBus.registerHandler("xmpp/session/" + sessionId2 + "/roster", new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("rosterEvent", event.body());
			}
		});
		eventBus.send("xmpp/session/" + sessionId2 + "/roster:add-buddy",
				new JsonObject().putString("user", user1.login + "@" + domainName));

		assertNotNull(waitAssert("session"));

		// now we can accept sub
		eventBus.send("xmpp/session/" + sessionId + ":accept-subscribe",
				new JsonObject().putString("to", user2.login + "@" + domainName), new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> event) {
						queueAssertValue("sub", event.body());
					}
				});

		JsonObject resp = null;
		assertNotNull((resp = waitAssert("sub")));
		assertEquals(XmppSessionMessage.OK, resp.getNumber("status"));

		JsonObject rosterEvent = waitRosterAssert("rosterEvent", "presence");
		assertEquals("available", rosterEvent.getObject("change").getString("subscription-type"));

	}

	@Test
	public void testDiscardSubscribe() throws Exception {

		String sessionId = login(user1);
		initiateConnection(user1, sessionId);

		String sessionId2 = login(user2);
		initiateConnection(user2, sessionId2);

		eventBus.registerHandler("xmpp/session/" + sessionId, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("session", event.body());
			}
		});

		eventBus.registerHandler("xmpp/session/" + sessionId2 + "/roster", new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("rosterEvent", event.body());
			}
		});
		eventBus.send("xmpp/session/" + sessionId2 + "/roster:add-buddy",
				new JsonObject().putString("user", user1.login + "@" + domainName));

		// user2 asked subscribe to user1
		waitAssert("session");

		// now we can accept sub
		eventBus.send("xmpp/session/" + sessionId + ":accept-subscribe",
				new JsonObject().putString("to", user2.login + "@" + domainName), new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> event) {
						queueAssertValue("sub", event.body());
					}
				});

		// sub accepted
		waitAssert("sub");

		// now user1 should be available to user2
		// wait for roster of user2 update
		waitRosterAssert("rosterEvent", "presence");

		// now we can discard sub
		eventBus.send("xmpp/session/" + sessionId + ":discard-subscribe",
				new JsonObject().putString("to", user2.login + "@" + domainName), new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> event) {
						queueAssertValue("sub", event.body());
					}
				});

		// now user1 should be unavailable to user2
		// wait for roster of user2 update
		JsonObject rosterEvent = waitRosterAssert("rosterEvent", "entries-updated");
		assertEquals(user1.login + "@" + domainName, rosterEvent.getObject("change").getArray("entries").get(0));
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
