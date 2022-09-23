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
package net.bluemind.core.container.service.internal;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.lib.vertx.utils.DebouncedEventPublisher;

/**
 * Publish items change event over Vert.x event bus. In order to avoid spamming,
 * there is a <i>debounce</i> time of {@value #DEBOUNCE_TIME_MILLIS}ms.
 */
public class ContainerChangeEventProducer {

	private static final int DEBOUNCE_TIME_MILLIS = 500;
	private DebouncedEventPublisher debouncedEventPublisher;
	private final JsonObject message;

	public ContainerChangeEventProducer(final SecurityContext securityContext, final EventBus eventBus,
			final Container container) {
		final String loginAtDomain = securityContext.getSubject();
		String address = String.format("bm.%s.hook.%s.changed", container.type, container.uid);
		this.message = new JsonObject();
		message.put("loginAtDomain", loginAtDomain);
		debouncedEventPublisher = new DebouncedEventPublisher(address, eventBus, DEBOUNCE_TIME_MILLIS);
	}

	public void produceEvent() {
		this.debouncedEventPublisher.publish(this.message);
	}

}