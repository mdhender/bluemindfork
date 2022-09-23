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
import io.vertx.core.eventbus.MessageProducer;

/**
 * Publish events on the Vert.x event bus in a <i>debounce-like</i> way.
 *
 * @see Debouncer
 */
public class DebouncedEventPublisher {

	private final Debouncer<Object> debouncer;

	/**
	 * 
	 * {@link DebouncedEventPublisher} with <code>noDebounceFirstMode=true</code>
	 * 
	 * @param address
	 * @param eventBus
	 * @param debounceTimeMillis
	 */
	public DebouncedEventPublisher(String address, EventBus eventBus, int debounceTimeMillis) {
		this(address, eventBus, debounceTimeMillis, true);
	}

	public DebouncedEventPublisher(String address, EventBus eventBus, int debounceTimeMillis,
			boolean noDebounceFirstMode) {
		MessageProducer<Object> publisher = eventBus.publisher(address);
		this.debouncer = new Debouncer<>(publisher::write, debounceTimeMillis, noDebounceFirstMode);
	}

	public void publish(Object message) {
		this.debouncer.call(message);
	}

}
