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
package net.bluemind.todolist.usertodolist;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.user.api.IUserSubscription;
import net.bluemind.user.api.User;
import net.bluemind.user.hook.DefaultUserHook;

public class UserTodoListHook extends DefaultUserHook {

	private static Logger logger = LoggerFactory.getLogger(UserTodoListHook.class);

	public UserTodoListHook() {
	}

	private final IServiceProvider sp() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	}

	@Override
	public void onUserCreated(BmContext context, String domainUid, ItemValue<User> created) {
		if (!created.value.system) {
			ItemValue<User> user = created;
			String uid = getUserTodoListId(user);
			ContainerDescriptor todoList = ContainerDescriptor.create(uid, "$$mytasks$$", user.uid, ITodoUids.TYPE,
					domainUid, true);

			try {
				IContainers containers = sp().instance(IContainers.class);

				containers.create(uid, todoList);

				IContainerManagement manager = sp().instance(IContainerManagement.class, uid);
				manager.setAccessControlList(Arrays.asList(AccessControlEntry.create(user.uid, Verb.Write)));

				IUserSubscription userSubService = sp().instance(IUserSubscription.class, domainUid);
				userSubService.subscribe(user.uid, Arrays.asList(ContainerSubscription.create(uid, true)));

			} catch (ServerFault e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	@Override
	public void onUserUpdated(BmContext context, String domainUid, ItemValue<User> previous, ItemValue<User> current) {
		// Nothing to do
	}

	@Override
	public void beforeDelete(BmContext context, String domainUid, String uid, User previous) throws ServerFault {
		if (!previous.system) {
			ItemValue<User> user = ItemValue.create(uid, previous);
			String defaultTodo = getUserTodoListId(user);

			try {
				deleteTodoList(user, defaultTodo);
				IContainers cs = sp().instance(IContainers.class);
				ContainerQuery query = new ContainerQuery();
				query.type = "todolist";
				query.owner = user.uid;
				cs.all(query).forEach(todo -> deleteTodoList(user, todo.uid));
			} catch (ServerFault e) {
				logger.error("error during TodoList deletion ", e);
			}
		}
	}

	private void deleteTodoList(ItemValue<User> user, String container) {
		logger.info("Delete TodoList {} for user {}", container, user.value.login);
		ITodoList todo = sp().instance(ITodoList.class, container);
		todo.reset();
		sp().instance(IContainers.class).delete(container);
	}

	public static String getUserTodoListId(ItemValue<User> user) {
		return ITodoUids.defaultUserTodoList(user.uid);
	}

}
