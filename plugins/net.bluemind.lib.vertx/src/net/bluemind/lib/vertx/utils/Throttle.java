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

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;

public class Throttle<T> implements Handler<Message<? extends T>> {

	private Message<? extends T> throttleEvent;
	private Vertx vertx;
	private int interval;

	private volatile Long timerId;
	private Handler<Message<? extends T>> wrappredHandler;

	private BiFunction<Message<? extends T>, Message<? extends T>, Message<? extends T>> msgAccumulator;

	public static <T> BiFunction<Message<? extends T>, Message<? extends T>, Message<? extends T>> lastAccumulator() {
		return (currentMessage, newMessage) -> newMessage;
	};

	public static <T> BiFunction<Message<? extends T>, Message<? extends T>, Message<? extends T>> firstAccumulator() {
		return (currentMessage, newMessage) -> currentMessage != null ? currentMessage : newMessage;
	};

	public Throttle(Handler<Message<? extends T>> wrappedHandler,
			BiFunction<Message<? extends T>, Message<? extends T>, Message<? extends T>> accu, Vertx vertx,
			int throttleTimeInMs) {
		this.vertx = vertx;
		this.interval = throttleTimeInMs;
		this.timerId = null;
		this.wrappredHandler = wrappedHandler;
		this.msgAccumulator = accu;
	}

	public Throttle(Handler<Message<? extends T>> wrappedHandler, Vertx vertx, int throttleTimeInMs) {
		this(wrappedHandler, lastAccumulator(), vertx, throttleTimeInMs);
	}

	private static final boolean DISABLED = "true".equals(System.getProperty("throttle.disabled", "false"));

	@Override
	public void handle(Message<? extends T> event) {

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
						Message<? extends T> t = throttleEvent;
						throttleEvent = null;
						fireEvent(t);
					}
				});
				fireEvent(event);
			}
		}
	}

	private void fireEvent(Message<? extends T> event) {
		if (event != null) {
			wrappredHandler.handle(event);
		}
	}
}
