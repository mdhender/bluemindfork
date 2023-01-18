/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.common.vertx.msgpack.tests;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import net.bluemind.common.vertx.msgpack.MostlyBinaryJson;
import net.bluemind.lib.vertx.VertxPlatform;

public class VertxCodecTests {

	@Before
	public void before() {
		VertxPlatform.spawnBlocking(10, TimeUnit.SECONDS);
	}

	@Test
	public void testEvents() throws InterruptedException {
		String addr = "test." + System.nanoTime();
		EventBus eb = VertxPlatform.eventBus();
		DeliveryOptions codecOps = new DeliveryOptions().setCodecName("MsgpackCompliant");
		MessageConsumer<JsonObject> consumer = eb.consumer(addr);
		consumer.handler(msg -> {
			JsonObject js = msg.body();
			System.err.println("consumed: " + js);
			js.put("repl", "ok");
			MostlyBinaryJson mostly = new MostlyBinaryJson(js);
			msg.reply(mostly, codecOps);
		});

		CountDownLatch cdl = new CountDownLatch(1);
		eb.request(addr, new JsonObject().put("foo", "bar".getBytes()), (AsyncResult<Message<JsonObject>> ar) -> {
			if (ar.failed()) {
				ar.cause().printStackTrace();
			} else {
				JsonObject reply = ar.result().body();
				System.err.println("reply: " + reply);
			}
			cdl.countDown();
		});
		cdl.await();
		consumer.unregister();

	}

}
