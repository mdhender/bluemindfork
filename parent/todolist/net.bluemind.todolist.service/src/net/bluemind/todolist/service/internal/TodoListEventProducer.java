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
package net.bluemind.todolist.service.internal;

import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.LocalJsonObject;
import net.bluemind.todolist.api.VTodo;
import net.bluemind.todolist.hook.TodoListHookAddress;
import net.bluemind.todolist.hook.internal.VTodoMessage;

public class TodoListEventProducer {

	private Container container;
	private String loginAtDomain;
	private EventBus eventBus;
	private SecurityContext securityContext;

	public TodoListEventProducer(Container container, SecurityContext sc, EventBus ev) {
		this.container = container;
		this.loginAtDomain = sc.getSubject();
		this.eventBus = ev;
		securityContext = sc;
	}

	public void vtodoCreated(String uid, VTodo vtodo) {
		VTodoMessage msg = getVTodoMessage(uid, vtodo);
		eventBus.publish(TodoListHookAddress.CREATED, new LocalJsonObject<>(msg));
	}

	// FIXME do we need old value ?
	public void vtodoUpdated(String uid, VTodo oldVtodo, VTodo vtodo) {
		VTodoMessage msg = getVTodoMessage(uid, vtodo);
		msg.oldVtodo = oldVtodo;
		eventBus.publish(TodoListHookAddress.UPDATED, new LocalJsonObject<>(msg));
	}

	public void vtodoDeleted(String uid, VTodo vtodo) {
		VTodoMessage msg = getVTodoMessage(uid, vtodo);

		eventBus.publish(TodoListHookAddress.DELETED, new LocalJsonObject<>(msg));
	}

	public void changed() {
		JsonObject body = new JsonObject();
		body.putString("loginAtDomain", loginAtDomain);
		eventBus.publish(TodoListHookAddress.getChangedEventAddress(container.uid), body);

		eventBus.publish(TodoListHookAddress.CHANGED,
				new JsonObject().putString("container", container.uid).putString("type", container.type)
						.putString("loginAtDomain", loginAtDomain)
						.putString("domainUid", securityContext.getContainerUid()));
	}

	private VTodoMessage getVTodoMessage(String uid, VTodo vtodo) {
		VTodoMessage ret = new VTodoMessage();
		ret.itemUid = uid;
		ret.vtodo = vtodo;
		ret.securityContext = securityContext;
		ret.container = this.container;
		return ret;
	}

}
