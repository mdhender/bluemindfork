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

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
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

		eventBus.request(addr + ":create", new JsonObject().put("name", roomName).put("nickname", nickname),
				new Handler<AsyncResult<Message<JsonObject>>>() {

					@Override
					public void handle(AsyncResult<Message<JsonObject>> reply) {
						queueAssertValue("muc", reply.result().body());
					}
				});

		JsonObject value = waitAssert("muc");
		assertNotNull(value);
		assertEquals(XmppSessionMessage.OK, value.getInteger("status"));
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

		eventBus.consumer("xmpp/muc/" + sessionId2, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("muc", event.body());
			}
		});

		String room = createMuc(sessionId, "mucroom" + System.nanoTime(), user1.login);

		eventBus.send("xmpp/muc/" + sessionId + "/" + room + ":invite",
				new JsonObject().put("latd", user2.login + "@" + domainName).put("reason", "come on buddy !"));

		JsonObject value = waitAssert("muc");

		assertNotNull(value);
		assertEquals("muc", value.getString("category"));
		assertEquals("invite", value.getString("action"));
		assertNotNull(value.getJsonObject("body"));

		assertEquals("come on buddy !", value.getJsonObject("body").getString("reason"));

	}

	@Test
	public void testJoin() {

		String sessionId = login(user1);
		initiateConnection(user1, sessionId);

		String sessionId2 = login(user2);
		initiateConnection(user2, sessionId2);

		eventBus.consumer("xmpp/muc/" + sessionId2, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("muc", event.body());
			}
		});

		String room = createMuc(sessionId, "mucroom" + System.nanoTime(), user1.login);

		eventBus.request("xmpp/muc/" + sessionId2 + ":join",
				new JsonObject().put("room", room).put("nickname", user2.login),
				new Handler<AsyncResult<Message<JsonObject>>>() {

					@Override
					public void handle(AsyncResult<Message<JsonObject>> event) {
						queueAssertValue("mucJoin", event.result().body());
					}
				});

		JsonObject value = waitAssert("mucJoin");

		assertNotNull(value);
		assertEquals(XmppSessionMessage.OK, value.getInteger("status"));
	}

	@Test
	public void testRoomParticipants() {

		String sessionId = login(user1);
		initiateConnection(user1, sessionId);

		String sessionId2 = login(user2);
		initiateConnection(user2, sessionId2);

		eventBus.consumer("xmpp/muc/" + sessionId2, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("muc", event.body());
			}
		});

		String room = createMuc(sessionId, "mucroom" + System.nanoTime(), user1.login);

		eventBus.request("xmpp/muc/" + sessionId + "/" + room + ":participants", new JsonObject(),
				new Handler<AsyncResult<Message<JsonObject>>>() {

					@Override
					public void handle(AsyncResult<Message<JsonObject>> event) {
						queueAssertValue("participants", event.result().body());
					}

				});

		JsonObject participtantsResp = waitAssert("participants");
		assertNotNull(participtantsResp);
		assertEquals(XmppSessionMessage.OK, participtantsResp.getInteger("status"));

		assertNotNull(participtantsResp.getJsonArray("participants"));

		Set<String> pset = new HashSet<>();
		for (Object p : participtantsResp.getJsonArray("participants")) {
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

		eventBus.consumer("xmpp/muc/" + sessionId2, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue("muc", event.body());
			}
		});

		String room = createMuc(sessionId, "mucroom" + System.nanoTime(), user1.login);

		eventBus.consumer("xmpp/muc/" + sessionId, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				System.out.println("muc message " + event.body());
				queueAssertValue("mucSession", event.body());
			}
		});

		eventBus.request("xmpp/muc/" + sessionId2 + ":join",
				new JsonObject().put("room", room).put("nickname", user2.login),
				new Handler<AsyncResult<Message<JsonObject>>>() {

					@Override
					public void handle(AsyncResult<Message<JsonObject>> event) {
						queueAssertValue("mucJoin", event.result().body());
					}
				});

		waitAssert("mucJoin");

		JsonObject value = waitAssert("mucSession");
		assertNotNull(value);
		assertEquals("participants", value.getString("action"));

		eventBus.request("xmpp/muc/" + sessionId + "/" + room + ":participants", new JsonObject(),
				new Handler<AsyncResult<Message<JsonObject>>>() {

					@Override
					public void handle(AsyncResult<Message<JsonObject>> event) {
						System.out.println(event.result().body());
						queueAssertValue("participants", event.result().body());
					}

				});

		JsonObject participtantsResp = waitAssert("participants");
		assertNotNull(participtantsResp);
		assertEquals(XmppSessionMessage.OK, participtantsResp.getInteger("status"));

		assertNotNull(participtantsResp.getJsonArray("participants"));

		Set<String> pset = new HashSet<>();
		for (Object p : participtantsResp.getJsonArray("participants")) {
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

		eventBus.request("xmpp/muc/" + sessionId2 + ":join",
				new JsonObject().put("room", room).put("nickname", user2.login),
				new Handler<AsyncResult<Message<JsonObject>>>() {

					@Override
					public void handle(AsyncResult<Message<JsonObject>> event) {
						queueAssertValue("mucJoin", event.result().body());
					}
				});

		waitAssert("mucJoin");

		eventBus.consumer("xmpp/muc/" + sessionId, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				System.out.println("muc message " + event.body());
				queueAssertValue("mucSession", event.body());
			}
		});

		eventBus.request("xmpp/muc/" + sessionId2 + "/" + room + ":leave", new JsonObject(),
				new Handler<AsyncResult<Message<JsonObject>>>() {

					@Override
					public void handle(AsyncResult<Message<JsonObject>> event) {
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

		eventBus.consumer("xmpp/muc/" + sessionId, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				System.out.println("muc message " + event.body());
				queueAssertValue("mucSession", event.body());
			}
		});

		eventBus.send("xmpp/muc/" + sessionId + "/" + room + ":message", new JsonObject().put("message", "yeah ha !"));

		JsonObject message = waitAssert("mucSession");
		assertNotNull(message);
		assertEquals("message", message.getString("action"));
		assertNotNull(message.getJsonObject("body"));
		assertEquals("yeah ha !", message.getJsonObject("body").getString("message"));
	}
}
