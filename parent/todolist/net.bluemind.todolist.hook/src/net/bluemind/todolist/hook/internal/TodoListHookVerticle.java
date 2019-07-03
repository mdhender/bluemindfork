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
package net.bluemind.todolist.hook.internal;

import java.util.List;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.platform.Verticle;

import net.bluemind.core.rest.LocalJsonObject;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.todolist.hook.ITodoListHook;
import net.bluemind.todolist.hook.TodoListHookAddress;

public class TodoListHookVerticle extends Verticle {

	@Override
	public void start() {

		RunnableExtensionLoader<ITodoListHook> loader = new RunnableExtensionLoader<ITodoListHook>();

		List<ITodoListHook> hooks = loader.loadExtensions("net.bluemind.todolist", "hook", "hook", "impl");

		EventBus eventBus = vertx.eventBus();

		for (final ITodoListHook hook : hooks) {
			eventBus.registerHandler(TodoListHookAddress.CREATED,
					new Handler<Message<LocalJsonObject<VTodoMessage>>>() {
						public void handle(Message<LocalJsonObject<VTodoMessage>> message) {
							hook.onTodoCreated(message.body().getValue());
						}
					});

			eventBus.registerHandler(TodoListHookAddress.UPDATED,
					new Handler<Message<LocalJsonObject<VTodoMessage>>>() {
						public void handle(Message<LocalJsonObject<VTodoMessage>> message) {
							hook.onTodoUpdated(message.body().getValue());
						}
					});

			eventBus.registerHandler(TodoListHookAddress.DELETED,
					new Handler<Message<LocalJsonObject<VTodoMessage>>>() {
						public void handle(Message<LocalJsonObject<VTodoMessage>> message) {
							hook.onTodoDeleted(message.body().getValue());
						}
					});
		}

	}
}
