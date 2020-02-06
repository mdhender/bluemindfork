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
package net.bluemind.lib.vertx.utils;

import java.util.function.BiFunction;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;

public class Throttle<T> implements Handler<Message<T>> {

	private Message<T> throttleEvent;
	private Vertx vertx;
	private int interval;

	private volatile Long timerId;
	private Handler<Message<T>> wrappredHandler;

	private BiFunction<Message<T>, Message<T>, Message<T>> msgAccumulator;

	public static <T> BiFunction<Message<T>, Message<T>, Message<T>> lastAccumulator() {
		return (currentMessage, newMessage) -> newMessage;
	};

	public static <T> BiFunction<Message<T>, Message<T>, Message<T>> firstAccumulator() {
		return (currentMessage, newMessage) -> currentMessage != null ? currentMessage : newMessage;
	};

	public Throttle(Handler<Message<T>> wrappedHandler, BiFunction<Message<T>, Message<T>, Message<T>> accu,
			Vertx vertx, int throttleTimeInMs) {
		this.vertx = vertx;
		this.interval = throttleTimeInMs;
		this.timerId = null;
		this.wrappredHandler = wrappedHandler;
		this.msgAccumulator = accu;
	}

	public Throttle(Handler<Message<T>> wrappedHandler, Vertx vertx, int throttleTimeInMs) {
		this(wrappedHandler, lastAccumulator(), vertx, throttleTimeInMs);
	}

	private static final boolean DISABLED = "true".equals(System.getProperty("throttle.disabled", "false"));

	@Override
	public void handle(Message<T> event) {

		if (DISABLED) {
			fireEvent(event);
			return;
		}

		synchronized (this) {
			if (timerId != null) {
				this.throttleEvent = msgAccumulator.apply(throttleEvent, event);
			} else {
				timerId = vertx.setTimer(this.interval, (id) -> {
					synchronized (this) {
						timerId = null;
						Message<T> t = throttleEvent;
						throttleEvent = null;
						fireEvent(t);
					}
				});
				fireEvent(event);
			}
		}
	}

	private void fireEvent(Message<T> event) {
		if (event != null) {
			wrappredHandler.handle(event);
		}
	}
}
