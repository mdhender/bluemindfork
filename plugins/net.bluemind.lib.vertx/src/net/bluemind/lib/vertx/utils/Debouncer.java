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
package net.bluemind.lib.vertx.utils;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.vertx.core.Vertx;
import net.bluemind.lib.vertx.VertxPlatform;

/**
 * Block calls to the given {@link BiConsumer} in a <i>debounce</i> way. Allow
 * the last call to be executed after a grace period ({@link #interval}). Each
 * call is identified using <code>KEY</code>. Only calls sharing the same
 * identity block each others. Thread safe.
 * 
 * @see https://stackoverflow.com/questions/4742210/implementing-debounce-in-java
 * @see http://reactivex.io/documentation/operators/debounce.html
 */
public class Debouncer<P> {

	private final Consumer<P> consumer;
	private final int interval;
	private final boolean noDebounceFirst;
	private final AtomicReference<Long> lastTimer;
	private final Vertx vertx;

	/**
	 * @param consumer        the code to execute in a <i>debounce</i> way
	 * @param interval        the <i>debounce</i> grace period in milliseconds
	 * @param noDebounceFirst the very first call (per key) will not suffer the
	 *                        grace period. It violates the <i>real debounce</i>
	 *                        pattern but may be useful in some cases.
	 */
	public Debouncer(Consumer<P> consumer, final int interval, final boolean noDebounceFirst) {
		this.consumer = consumer;
		this.interval = interval;
		this.noDebounceFirst = noDebounceFirst;
		this.lastTimer = new AtomicReference<>();
		this.vertx = VertxPlatform.getVertx();
	}

	/**
	 * Request a call to {@link #consumer}, based on the given <code>key</code>. The
	 * actual execution of the code depends on the debounce grace period: if no call
	 * with that key has been done within {@link #interval} milliseconds then
	 * execute the last call.
	 */
	public void call(P payload) {
		int actualInterval;
		Long lastVal = lastTimer.get();
		if (lastVal != null) {
			vertx.cancelTimer(lastVal);
			actualInterval = interval;
		} else {
			actualInterval = noDebounceFirst ? 0 : interval;
		}
		launchTask(actualInterval, payload);
	}

	private void launchTask(final int delay, P payload) {
		if (delay > 0) {
			lastTimer.set(vertx.setTimer(delay, tid -> {
				consumer.accept(payload);
				lastTimer.set(null);
			}));
		} else {
			lastTimer.set(vertx.setTimer(1, tid -> consumer.accept(payload)));

		}
	}

}
