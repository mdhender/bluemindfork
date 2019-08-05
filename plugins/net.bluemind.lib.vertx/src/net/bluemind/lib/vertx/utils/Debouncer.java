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

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.vertx.java.core.Handler;

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
public class Debouncer<KEY, PAYLOAD> {
	private final ConcurrentHashMap<KEY, DebouncerTask> tasks = new ConcurrentHashMap<KEY, DebouncerTask>();
	private final BiConsumer<KEY, PAYLOAD> consumer;
	private final int interval;
	private final boolean noDebounceFirst;

	/**
	 * @param consumer        the code to execute in a <i>debounce</i> way
	 * @param interval        the <i>debounce</i> grace period in milliseconds
	 * @param noDebounceFirst the very first call (per key) will not suffer the
	 *                        grace period. It violates the <i>real debounce</i>
	 *                        pattern but may be useful in some cases.
	 */
	public Debouncer(final BiConsumer<KEY, PAYLOAD> consumer, final int interval, final boolean noDebounceFirst) {
		this.consumer = consumer;
		this.interval = interval;
		this.noDebounceFirst = noDebounceFirst;
	}

	/**
	 * Request a call to {@link #consumer}, based on the given <code>key</code>. The
	 * actual execution of the code depends on the debounce grace period: if no call
	 * with that key has been done within {@link #interval} milliseconds then
	 * execute the last call.
	 */
	public void call(final KEY key, final PAYLOAD payload) {
		final DebouncerTask debounceTask = new DebouncerTask(this.consumer, key, payload);
		final DebouncerTask previousDebounceTask = this.tasks.remove(key);
		final int actualInterval;
		if (previousDebounceTask != null) {
			this.cancelTask(previousDebounceTask);
			actualInterval = interval;
		} else {
			actualInterval = this.noDebounceFirst ? 0 : interval;
		}
		this.tasks.put(key, debounceTask);
		this.launchTask(actualInterval, debounceTask);
	}

	private void launchTask(final int delay, final DebouncerTask debouncerTask) {
		this.launchTask(delay, debouncerTask, debouncerTask);
	}

	private void launchTask(final int delay, final Handler<Long> handler, final DebouncerTask debouncerTask) {
		if (delay > 0) {
			// note: vertx.setTimer does not handle a 0ms delay
			debouncerTask.setTimerId(VertxPlatform.getVertx().setTimer(delay, handler));
		} else {
			final long id = -1L;
			debouncerTask.setTimerId(id);
			handler.handle(id);
		}
	}

	private void cancelTask(final DebouncerTask debouncerTask) {
		final long timerId = debouncerTask.getTimerId();
		if (timerId > -1) {
			VertxPlatform.getVertx().cancelTimer(timerId);
		}
	}

	/**
	 * Special task for {@link Debouncer}. A {@link Handler} that calls a
	 * {@link BiConsumer} if not aborted.
	 */
	private class DebouncerTask implements Handler<Long> {
		private final BiConsumer<KEY, PAYLOAD> consumer;
		private final KEY key;
		private final PAYLOAD payload;
		private long timerId;

		public DebouncerTask(final BiConsumer<KEY, PAYLOAD> consumer, final KEY key, final PAYLOAD payload) {
			this.consumer = consumer;
			this.key = key;
			this.payload = payload;
		}

		public void setTimerId(long timerId) {
			this.timerId = timerId;
		}

		public long getTimerId() {
			return timerId;
		}

		@Override
		public void handle(Long event) {
			this.consumer.accept(this.key, this.payload);
			final int actualInterval = noDebounceFirst ? interval : 0;
			launchTask(actualInterval, new Handler<Long>() {

				@Override
				public void handle(Long event) {
					tasks.remove(key);
				}
			}, this);
		}
	}

}
