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

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.platform.Verticle;

import net.bluemind.calendar.hook.CalendarHookAddress;
import net.bluemind.calendar.hook.ICalendarHook;
import net.bluemind.core.rest.LocalJsonObject;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class CalendarHookVerticle extends Verticle {

	@Override
	public void start() {

		RunnableExtensionLoader<ICalendarHook> loader = new RunnableExtensionLoader<>();

		List<ICalendarHook> hooks = loader.loadExtensions("net.bluemind.calendar", "hook", "hook", "impl");

		EventBus eventBus = vertx.eventBus();

		for (final ICalendarHook hook : hooks) {
			eventBus.registerHandler(CalendarHookAddress.EVENT_CREATED,
					new Handler<Message<LocalJsonObject<VEventMessage>>>() {
						public void handle(Message<LocalJsonObject<VEventMessage>> message) {
							hook.onEventCreated(message.body().getValue());
						}
					});

			eventBus.registerHandler(CalendarHookAddress.EVENT_UPDATED,
					new Handler<Message<LocalJsonObject<VEventMessage>>>() {
						public void handle(Message<LocalJsonObject<VEventMessage>> message) {
							hook.onEventUpdated(message.body().getValue());
						}
					});

			eventBus.registerHandler(CalendarHookAddress.EVENT_DELETED,
					new Handler<Message<LocalJsonObject<VEventMessage>>>() {
						public void handle(Message<LocalJsonObject<VEventMessage>> message) {
							hook.onEventDeleted(message.body().getValue());
						}
					});
		}

	}
}
