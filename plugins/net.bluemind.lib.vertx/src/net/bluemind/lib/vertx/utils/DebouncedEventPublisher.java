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

import java.util.function.BiConsumer;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

/**
 * Publish events on the Vert.x event bus in a <i>debounce-like</i> way (we use
 * the noDebounceFirstMode).
 *
 * @see Debouncer
 */
public class DebouncedEventPublisher {
	private Debouncer<String, PayLoad> debouncer;

	public DebouncedEventPublisher(final EventBus eventBus, final int debounceTimeMillis) {
		final boolean noDebounceFirstMode = true;
		this.debouncer = new Debouncer<>(new BiConsumer<String, PayLoad>() {

			@Override
			public void accept(final String key, final PayLoad payload) {
				eventBus.publish(payload.getAddress(), payload.getMessage());
			}
		}, debounceTimeMillis, noDebounceFirstMode);
	}

	public void publish(final String address, final JsonObject message, final String debounceKey) {
		final PayLoad payLoad = new PayLoad(address, message);
		this.debouncer.call(address, payLoad);
	}

	private class PayLoad {
		private String address;
		private JsonObject message;

		public PayLoad(final String address, final JsonObject message) {
			this.address = address;
			this.message = message;
		}

		public String getAddress() {
			return address;
		}

		public JsonObject getMessage() {
			return message;
		}
	}

}
