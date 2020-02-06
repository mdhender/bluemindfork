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
package net.bluemind.calendar.hook.internal;

import java.util.List;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import net.bluemind.calendar.hook.CalendarHookAddress;
import net.bluemind.calendar.hook.ICalendarHook;
import net.bluemind.core.rest.LocalJsonObject;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class CalendarHookVerticle extends AbstractVerticle {

	@Override
	public void start() {

		RunnableExtensionLoader<ICalendarHook> loader = new RunnableExtensionLoader<>();

		List<ICalendarHook> hooks = loader.loadExtensionsWithPriority("net.bluemind.calendar", "hook", "hook", "impl");

		EventBus eventBus = vertx.eventBus();

		eventBus.consumer(CalendarHookAddress.EVENT_CREATED, new Handler<Message<LocalJsonObject<VEventMessage>>>() {
			public void handle(Message<LocalJsonObject<VEventMessage>> message) {
				for (final ICalendarHook hook : hooks) {
					hook.onEventCreated(message.body().getValue());
				}
			}
		});

		eventBus.consumer(CalendarHookAddress.EVENT_UPDATED, new Handler<Message<LocalJsonObject<VEventMessage>>>() {
			public void handle(Message<LocalJsonObject<VEventMessage>> message) {
				for (final ICalendarHook hook : hooks) {
					hook.onEventUpdated(message.body().getValue());
				}
			}
		});

		eventBus.consumer(CalendarHookAddress.EVENT_DELETED, new Handler<Message<LocalJsonObject<VEventMessage>>>() {
			public void handle(Message<LocalJsonObject<VEventMessage>> message) {
				for (final ICalendarHook hook : hooks) {
					hook.onEventDeleted(message.body().getValue());
				}
			}
		});

	}
}
