/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.lib.vertx.tests;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.lib.vertx.utils.DebouncedEventPublisher;

public class DebouncedEventPublisherTests {

	/**
	 * When events are too close, then only the first one and the last one should be
	 * sent.
	 */
	@Test
	public void testTooFastEvents() {
		final int debounceGracePeriod = 50;
		final int delayBetweenEvents = 40;
		final int maxSuccesiveCalls = 100;
		final String myPropKey = "prop";
		final String myPropValuePrefix = "myPropValue_";

		final List<JsonObject> receivedMessages = this.debounce(debounceGracePeriod, delayBetweenEvents,
				maxSuccesiveCalls, myPropKey, myPropValuePrefix);

		Assert.assertEquals(2, receivedMessages.size());
		Assert.assertEquals(myPropValuePrefix + 0, receivedMessages.get(0).getString(myPropKey));
		Assert.assertEquals(myPropValuePrefix + (maxSuccesiveCalls - 1), receivedMessages.get(1).getString(myPropKey));
	}

	/** When events are away enough, then all of them should be sent. */
	@Test
	public void testEventsWithEnoughDelay() {
		final int debounceGracePeriod = 30;
		final int delayBetweenEvents = 40;
		final int maxSuccesiveCalls = 100;
		final String myPropKey = "prop";
		final String myPropValuePrefix = "myPropValue_";

		final List<JsonObject> receivedMessages = this.debounce(debounceGracePeriod, delayBetweenEvents,
				maxSuccesiveCalls, myPropKey, myPropValuePrefix);

		Assert.assertEquals(maxSuccesiveCalls, receivedMessages.size());
		Assert.assertEquals(myPropValuePrefix + 0, receivedMessages.get(0).getString(myPropKey));
		Assert.assertEquals(myPropValuePrefix + 50, receivedMessages.get(50).getString(myPropKey));
		Assert.assertEquals(myPropValuePrefix + 99, receivedMessages.get(99).getString(myPropKey));
	}

	private List<JsonObject> debounce(final int debounceGracePeriod, final int delayBetweenEvents,
			final int maxSuccesiveCalls, final String myPropKey, final String myPropValuePrefix) {
		final List<JsonObject> receivedMessages = new ArrayList<>();

		final String address = "my.address";
		final String containerUid = "my-container-uid";

		VertxPlatform.eventBus().consumer(address, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				receivedMessages.add(event.body());
			}
		});

		final DebouncedEventPublisher debouncedEventPublisher = new DebouncedEventPublisher(address,
				VertxPlatform.eventBus(), debounceGracePeriod);

		for (int i = 0; i < maxSuccesiveCalls; i++) {
			final JsonObject message = new JsonObject();
			message.put(myPropKey, myPropValuePrefix + i);
			debouncedEventPublisher.publish(message);
			try {
				Thread.sleep(delayBetweenEvents);
			} catch (InterruptedException e) {
			}
		}

		// add more time to reach the grace period
		try {
			Thread.sleep(debounceGracePeriod);
		} catch (InterruptedException e) {
		}

		return receivedMessages;
	}
}
