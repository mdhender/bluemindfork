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

import io.vertx.core.eventbus.EventBus;

/**
 * Publish events on the Vert.x event bus in a <i>debounce-like</i> way.
 *
 * @see Debouncer
 */
public class DebouncedEventPublisher {

	private final Debouncer<String, PayLoad> debouncer;

	/**
	 * 
	 * {@link DebouncedEventPublisher} with <code>noDebounceFirstMode=true</code>
	 * 
	 * @param eventBus
	 * @param debounceTimeMillis
	 */
	public DebouncedEventPublisher(EventBus eventBus, int debounceTimeMillis) {
		this(eventBus, debounceTimeMillis, true);
	}

	public DebouncedEventPublisher(EventBus eventBus, int debounceTimeMillis, boolean noDebounceFirstMode) {
		this.debouncer = new Debouncer<>(//
				(String key, PayLoad payload) -> eventBus.publish(payload.getAddress(), payload.getMessage()), //
				debounceTimeMillis, noDebounceFirstMode);
	}

	public void publish(String address, Object message, String debounceKey) {
		final PayLoad payLoad = new PayLoad(address, message);
		this.debouncer.call(address, payLoad);
	}

	private class PayLoad {
		private final String address;
		private final Object message;

		public PayLoad(String address, Object message) {
			this.address = address;
			this.message = message;
		}

		public String getAddress() {
			return address;
		}

		public Object getMessage() {
			return message;
		}
	}

}
