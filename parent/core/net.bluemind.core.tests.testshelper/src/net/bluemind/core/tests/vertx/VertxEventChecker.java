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
package net.bluemind.core.tests.vertx;

import static org.junit.Assert.fail;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import net.bluemind.lib.vertx.VertxPlatform;

public class VertxEventChecker<T> {
	private SettableFuture<Message<T>> futureMessage;

	public VertxEventChecker(String address) {
		futureMessage = futureEvent(address);
	}

	public Message<T> shouldSuccess() {
		launchTimer();

		try {
			return futureMessage.get();

		} catch (ExecutionException e1) {
			fail("no event has been fired");
			return null;
		} catch (InterruptedException e) {
			fail("timeout");
			Thread.currentThread().interrupt();
			return null;
		}

	}

	public void shouldFail() {
		launchTimer();
		try {
			futureMessage.get();
			fail("event received");
		} catch (ExecutionException e1) {
			// normal path
		} catch (InterruptedException e) {
			fail("timeout");
			Thread.currentThread().interrupt();
		}

	}

	private static <T> SettableFuture<Message<T>> futureEvent(final String address) {
		final SettableFuture<Message<T>> futureMessage = SettableFuture.create();

		final Handler<Message<T>> handler = new Handler<Message<T>>() {

			@Override
			public void handle(Message<T> event) {
				futureMessage.set(event);
			}
		};
		MessageConsumer<T> cons = VertxPlatform.eventBus().consumer(address, handler);

		Futures.addCallback(futureMessage, new FutureCallback<Message<?>>() {

			@Override
			public void onSuccess(Message<?> result) {
				cons.unregister();

			}

			@Override
			public void onFailure(Throwable t) {
				cons.unregister();
			}
		}, MoreExecutors.directExecutor());

		return futureMessage;
	}

	private void launchTimer() {
		if (!futureMessage.isDone() && !futureMessage.isCancelled()) {

			VertxPlatform.getVertx().setTimer(3000, event -> {
				if (!futureMessage.isDone() && !futureMessage.isCancelled()) {
					futureMessage.setException(new TimeoutException());
				}
			});
		}
	}
}
