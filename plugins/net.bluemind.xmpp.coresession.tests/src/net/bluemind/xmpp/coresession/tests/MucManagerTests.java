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
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.xmpp.coresession.internal.XmppSessionMessage;

public class MucManagerTests extends BaseXmppTests {

	@Test
	public void testCreate() {
		String sessionId = login(user1);
		initiateConnection(user1, sessionId);

		createMuc(sessionId, "mucroom" + System.nanoTime(), user1.login);
	}

	private String createMuc(String sessionId, String roomName, String nickname) {
		String addr = "xmpp/muc/" + sessionId;

		eventBus.send(addr + ":create", new JsonObject().putString("name", roomName).putString("nickname", nickname),
				new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> reply) {
						queueAssertValue("muc", reply.body());
					}
				});

		JsonObject value = waitAssert("muc");
		assertNotNull(value);
		assertEquals(XmppSessionMessage.OK, value.getNumber("status"));
		assertNotNull(value.getString("roomName"));
		assertTrue(value.getString("roomName").startsWith(roomName));
		return value.getString("roomName");
	}

	@Test
	public void testInvite() {

		String sessionId = login(user1);
		initiateConnection(user1, sessionId);

		String sessionId2 = login(user2);
		initiateConnection(user2, sessionId2);

		eventBus.registerHandler("xmpp/muc/" + sessionId2, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("muc", event.body());
			}
		});

		String room = createMuc(sessionId, "mucroom" + System.nanoTime(), user1.login);

		eventBus.send("xmpp/muc/" + sessionId + "/" + room + ":invite", new JsonObject()
				.putString("latd", user2.login + "@" + domainName).putString("reason", "come on buddy !"));

		JsonObject value = waitAssert("muc");

		assertNotNull(value);
		assertEquals("muc", value.getString("category"));
		assertEquals("invite", value.getString("action"));
		assertNotNull(value.getObject("body"));

		assertEquals("come on buddy !", value.getObject("body").getString("reason"));

	}

	@Test
	public void testJoin() {

		String sessionId = login(user1);
		initiateConnection(user1, sessionId);

		String sessionId2 = login(user2);
		initiateConnection(user2, sessionId2);

		eventBus.registerHandler("xmpp/muc/" + sessionId2, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("muc", event.body());
			}
		});

		String room = createMuc(sessionId, "mucroom" + System.nanoTime(), user1.login);

		eventBus.send("xmpp/muc/" + sessionId2 + ":join",
				new JsonObject().putString("room", room).putString("nickname", user2.login),
				new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> event) {
						queueAssertValue("mucJoin", event.body());
					}
				});

		JsonObject value = waitAssert("mucJoin");

		assertNotNull(value);
		assertEquals(XmppSessionMessage.OK, value.getNumber("status"));
	}

	@Test
	public void testRoomParticipants() {

		String sessionId = login(user1);
		initiateConnection(user1, sessionId);

		String sessionId2 = login(user2);
		initiateConnection(user2, sessionId2);

		eventBus.registerHandler("xmpp/muc/" + sessionId2, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("muc", event.body());
			}
		});

		String room = createMuc(sessionId, "mucroom" + System.nanoTime(), user1.login);

		eventBus.send("xmpp/muc/" + sessionId + "/" + room + ":participants", new JsonObject(),
				new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> event) {
						queueAssertValue("participants", event.body());
					}

				});

		JsonObject participtantsResp = waitAssert("participants");
		assertNotNull(participtantsResp);
		assertEquals(XmppSessionMessage.OK, participtantsResp.getNumber("status"));

		assertNotNull(participtantsResp.getArray("participants"));

		Set<String> pset = new HashSet<>();
		for (Object p : participtantsResp.getArray("participants")) {
			pset.add((String) p);
		}

		assertEquals(1, pset.size());
		assertTrue(pset.contains(user1.login));
	}

	@Test
	public void testRoomParticipantsChanged() {

		String sessionId = login(user1);
		initiateConnection(user1, sessionId);

		String sessionId2 = login(user2);
		initiateConnection(user2, sessionId2);

		eventBus.registerHandler("xmpp/muc/" + sessionId2, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("muc", event.body());
			}
		});

		String room = createMuc(sessionId, "mucroom" + System.nanoTime(), user1.login);

		eventBus.registerHandler("xmpp/muc/" + sessionId, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				System.out.println("muc message " + event.body());
				queueAssertValue("mucSession", event.body());
			}
		});

		eventBus.send("xmpp/muc/" + sessionId2 + ":join",
				new JsonObject().putString("room", room).putString("nickname", user2.login),
				new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> event) {
						queueAssertValue("mucJoin", event.body());
					}
				});

		waitAssert("mucJoin");

		JsonObject value = waitAssert("mucSession");
		assertNotNull(value);
		assertEquals("participants", value.getString("action"));

		eventBus.send("xmpp/muc/" + sessionId + "/" + room + ":participants", new JsonObject(),
				new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> event) {
						System.out.println(event.body());
						queueAssertValue("participants", event.body());
					}

				});

		JsonObject participtantsResp = waitAssert("participants");
		assertNotNull(participtantsResp);
		assertEquals(XmppSessionMessage.OK, participtantsResp.getNumber("status"));

		assertNotNull(participtantsResp.getArray("participants"));

		Set<String> pset = new HashSet<>();
		for (Object p : participtantsResp.getArray("participants")) {
			pset.add((String) p);
		}

		assertEquals(2, pset.size());
		assertTrue(pset.contains(user1.login));
		assertTrue(pset.contains(user2.login));
	}

	@Test
	public void testLeave() {

		String sessionId = login(user1);
		initiateConnection(user1, sessionId);

		String sessionId2 = login(user2);
		initiateConnection(user2, sessionId2);

		String room = createMuc(sessionId, "mucroom" + System.nanoTime(), user1.login);

		eventBus.send("xmpp/muc/" + sessionId2 + ":join",
				new JsonObject().putString("room", room).putString("nickname", user2.login),
				new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> event) {
						queueAssertValue("mucJoin", event.body());
					}
				});

		waitAssert("mucJoin");

		eventBus.registerHandler("xmpp/muc/" + sessionId, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				System.out.println("muc message " + event.body());
				queueAssertValue("mucSession", event.body());
			}
		});

		eventBus.send("xmpp/muc/" + sessionId2 + "/" + room + ":leave", new JsonObject(),
				new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> event) {
						queueAssertValue("leave", Boolean.TRUE);
					}

				});
		assertNotNull(waitAssert("leave"));

		JsonObject value = waitAssert("mucSession");
		assertNotNull(value);
		assertEquals("participants", value.getString("action"));
	}

	@Test
	public void testMessage() {

		String sessionId = login(user1);
		initiateConnection(user1, sessionId);

		String room = createMuc(sessionId, "mucroom" + System.nanoTime(), user1.login);

		eventBus.registerHandler("xmpp/muc/" + sessionId, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				System.out.println("muc message " + event.body());
				queueAssertValue("mucSession", event.body());
			}
		});

		eventBus.send("xmpp/muc/" + sessionId + "/" + room + ":message",
				new JsonObject().putString("message", "yeah ha !"));

		JsonObject message = waitAssert("mucSession");
		assertNotNull(message);
		assertEquals("message", message.getString("action"));
		assertNotNull(message.getObject("body"));
		assertEquals("yeah ha !", message.getObject("body").getString("message"));
	}
}
