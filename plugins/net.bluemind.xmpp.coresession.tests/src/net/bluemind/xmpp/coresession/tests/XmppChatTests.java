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

import org.junit.Test;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class XmppChatTests extends BaseXmppTests {

	@Test
	public void testChatCreationOk() throws Exception {
		final String sessionId = login(user1);
		initiateConnection(user1, sessionId);

		eventBus.consumer("xmpp/session/" + sessionId, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue(sessionId, event.body());
			}
		});

		eventBus.send("xmpp/session/" + sessionId + ":chat", new JsonObject().put("userJID", "david@bm.lan"));

		// create chat
		JsonObject jsonObject = waitAssert(sessionId);
		assertNotNull(jsonObject);
		assertEquals(0, jsonObject.getInteger("status").intValue());
		assertEquals("chat", jsonObject.getString("category"));

		eventBus.send("xmpp/session/" + sessionId + ":close", "Good bye!");

	}

	@Test
	public void testChatCreationNotOk() throws Exception {
		final String sessionId = login(user1);
		initiateConnection(user1, sessionId);

		eventBus.consumer("xmpp/session/" + sessionId, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue(sessionId, event.body());
			}
		});

		eventBus.send("xmpp/session/" + sessionId + ":chat", new JsonObject());

		JsonObject jsonObject = waitAssert(sessionId);
		assertNotNull(jsonObject);
		assertEquals(1, jsonObject.getInteger("status").intValue());
		assertEquals("chat", jsonObject.getString("category"));

		eventBus.send("xmpp/session/" + sessionId + ":close", "Good bye!");
	}

	@Test
	public void testMarcoPolo() throws Exception {
		final String sessionUser1 = login(user1);
		initiateConnection(user1, sessionUser1);

		final String sessionUser2 = login(user2);
		initiateConnection(user2, sessionUser2);

		eventBus.consumer("xmpp/session/" + sessionUser1, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue(sessionUser1, event.body());

			}
		});

		eventBus.consumer("xmpp/session/" + sessionUser2, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue(sessionUser2, event.body());

			}
		});

		eventBus.send("xmpp/session/" + sessionUser1 + ":chat",
				new JsonObject().put("userJID", user2.login + "@" + domainName));

		// create chat
		JsonObject jsonObject = waitAssert(sessionUser1);
		assertNotNull(jsonObject);
		assertEquals(0, jsonObject.getInteger("status").intValue());
		assertEquals("chat", jsonObject.getString("category"));

		// user2 does not receive new chat creation because of no message sent
		// from user1
		assertNull(waitAssert(sessionUser2));

		//
		String threadId = jsonObject.getString("threadId");
		assertNotNull(threadId);

		// user1 sends message 'marco'
		eventBus.send("xmpp/session/" + sessionUser1 + "/chat/" + threadId + ":message",
				new JsonObject().put("message", "marco"));

		// user1 receives 'marco' too
		jsonObject = waitAssert(sessionUser1);
		assertNotNull(jsonObject);
		assertEquals(0, jsonObject.getInteger("status").intValue());
		assertEquals("message", jsonObject.getString("category"));
		assertEquals(threadId, jsonObject.getString("threadId"));
		assertEquals("marco", jsonObject.getString("body"));

		// user2 receives new chat
		jsonObject = waitAssert(sessionUser2);
		assertNotNull(jsonObject);
		assertEquals(0, jsonObject.getInteger("status").intValue());
		assertEquals("chat", jsonObject.getString("category"));

		// user2 receives message 'marco'
		jsonObject = waitAssert(sessionUser2);
		assertNotNull(jsonObject);
		assertEquals(0, jsonObject.getInteger("status").intValue());
		assertEquals("message", jsonObject.getString("category"));
		assertEquals(threadId, jsonObject.getString("threadId"));
		assertEquals("marco", jsonObject.getString("body"));

		// user2 replies 'polo'
		eventBus.send("xmpp/session/" + sessionUser2 + "/chat/" + threadId + ":message",
				new JsonObject().put("message", "polo"));

		// user2 receives message 'polo' too
		jsonObject = waitAssert(sessionUser2);
		assertNotNull(jsonObject);
		assertEquals(0, jsonObject.getInteger("status").intValue());
		assertEquals("message", jsonObject.getString("category"));
		assertEquals(threadId, jsonObject.getString("threadId"));
		assertEquals("polo", jsonObject.getString("body"));

		// user1 receives message 'polo'
		jsonObject = waitAssert(sessionUser1);
		assertNotNull(jsonObject);
		assertEquals(0, jsonObject.getInteger("status").intValue());
		assertEquals("message", jsonObject.getString("category"));
		assertEquals(threadId, jsonObject.getString("threadId"));
		assertEquals("polo", jsonObject.getString("body"));

		eventBus.send("xmpp/session/" + sessionUser1 + ":close", "Good bye!");
		eventBus.send("xmpp/session/" + sessionUser2 + ":close", "Good bye!");

	}

	@Test
	public void testInitChatWithMessage() throws Exception {
		final String sessionUser1 = login(user1);
		initiateConnection(user1, sessionUser1);

		final String sessionUser2 = login(user2);
		initiateConnection(user2, sessionUser2);

		eventBus.consumer("xmpp/session/" + sessionUser1, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue(sessionUser1, event.body());
			}
		});

		eventBus.consumer("xmpp/session/" + sessionUser2, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				queueAssertValue(sessionUser2, event.body());
			}
		});

		eventBus.send("xmpp/session/" + sessionUser1 + ":chat",
				new JsonObject().put("userJID", user2.login + "@" + domainName).put("message", "marco"));

		// create chat
		JsonObject jsonObject = waitAssert(sessionUser1);
		assertNotNull(jsonObject);
		assertEquals(0, jsonObject.getInteger("status").intValue());
		assertEquals("chat", jsonObject.getString("category"));

		String threadId = jsonObject.getString("threadId");
		assertNotNull(threadId);

		// user1 receives 'marco'
		jsonObject = waitAssert(sessionUser1);
		assertNotNull(jsonObject);
		assertEquals(0, jsonObject.getInteger("status").intValue());
		assertEquals("message", jsonObject.getString("category"));
		assertEquals(threadId, jsonObject.getString("threadId"));
		assertEquals("marco", jsonObject.getString("body"));

		// user2 receives new chat from user1 with message 'marco'
		jsonObject = waitAssert(sessionUser2);
		assertNotNull(jsonObject);
		assertEquals(0, jsonObject.getInteger("status").intValue());
		assertEquals("chat", jsonObject.getString("category"));

		jsonObject = waitAssert(sessionUser2);
		assertNotNull(jsonObject);
		assertEquals(0, jsonObject.getInteger("status").intValue());
		assertEquals("message", jsonObject.getString("category"));
		assertEquals(threadId, jsonObject.getString("threadId"));
		assertEquals("marco", jsonObject.getString("body"));

		// user2 replies 'polo'
		eventBus.send("xmpp/session/" + sessionUser2 + "/chat/" + threadId + ":message",
				new JsonObject().put("message", "polo"));

		// user1 receives 'marco'
		jsonObject = waitAssert(sessionUser2);
		assertNotNull(jsonObject);
		assertEquals(0, jsonObject.getInteger("status").intValue());
		assertEquals("message", jsonObject.getString("category"));
		assertEquals(threadId, jsonObject.getString("threadId"));
		assertEquals("polo", jsonObject.getString("body"));

		// user1 receives message 'polo'
		jsonObject = waitAssert(sessionUser1);
		assertNotNull(jsonObject);
		assertEquals(0, jsonObject.getInteger("status").intValue());
		assertEquals("message", jsonObject.getString("category"));
		assertEquals(threadId, jsonObject.getString("threadId"));
		assertEquals("polo", jsonObject.getString("body"));

		eventBus.send("xmpp/session/" + sessionUser1 + ":close", "Good bye!");
		eventBus.send("xmpp/session/" + sessionUser2 + ":close", "Good bye!");

	}

}
