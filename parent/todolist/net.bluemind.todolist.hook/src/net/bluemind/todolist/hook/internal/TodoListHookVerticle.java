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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import net.bluemind.core.rest.LocalJsonObject;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.todolist.hook.ITodoListHook;
import net.bluemind.todolist.hook.TodoListHookAddress;

public class TodoListHookVerticle extends AbstractVerticle {

	@Override
	public void start() {
		RunnableExtensionLoader<ITodoListHook> loader = new RunnableExtensionLoader<>();
		List<ITodoListHook> hooks = loader.loadExtensions("net.bluemind.todolist", "hook", "hook", "impl");
		EventBus eventBus = vertx.eventBus();

		for (final ITodoListHook hook : hooks) {
			eventBus.consumer(TodoListHookAddress.CREATED,
					(Message<LocalJsonObject<VTodoMessage>> message) -> hook.onTodoCreated(message.body().getValue()));
			eventBus.consumer(TodoListHookAddress.UPDATED,
					(Message<LocalJsonObject<VTodoMessage>> message) -> hook.onTodoUpdated(message.body().getValue()));
			eventBus.consumer(TodoListHookAddress.DELETED,
					(Message<LocalJsonObject<VTodoMessage>> message) -> hook.onTodoDeleted(message.body().getValue()));
		}

	}
}
