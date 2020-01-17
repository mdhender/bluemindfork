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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;

public class ThrottleMessages<T> implements Handler<Message<T>> {

	private Vertx vertx;
	private int interval;

	private Map<Object, Throttle<T>> timersMap = new ConcurrentHashMap<>();
	private Handler<Message<T>> wrappredHandler;
	private Function<Message<T>, Object> eventToKey;
	private BiFunction<Message<T>, Message<T>, Message<T>> msgAccumulator;

	public ThrottleMessages(Function<Message<T>, Object> eventToKey, Handler<Message<T>> wrappedHandler, Vertx vertx,
			int throttleTimeInMs) {
		this(eventToKey, Throttle.lastAccumulator(), wrappedHandler, vertx, throttleTimeInMs);
	}

	public ThrottleMessages(Function<Message<T>, Object> eventToKey,
			BiFunction<Message<T>, Message<T>, Message<T>> msgAccu, Handler<Message<T>> wrappedHandler, Vertx vertx,
			int throttleTimeInMs) {
		this.eventToKey = eventToKey;
		this.vertx = vertx;
		this.interval = throttleTimeInMs;
		this.wrappredHandler = wrappedHandler;
		this.msgAccumulator = msgAccu;
	}

	@Override
	public void handle(Message<T> event) {
		Throttle<T> handler = timersMap.computeIfAbsent(eventToKey.apply(event),
				k -> new Throttle<>(this.wrappredHandler, msgAccumulator, vertx, this.interval));
		handler.handle(event);
	}

}
