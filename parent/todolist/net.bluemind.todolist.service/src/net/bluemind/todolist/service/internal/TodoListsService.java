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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.api.ContainerSubscriptionDescriptor;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.hierarchy.hook.HierarchyIdsHints;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemContainerValue;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.ITodoLists;
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.todolist.api.TodoListsVTodoQuery;
import net.bluemind.todolist.api.VTodo;
import net.bluemind.todolist.api.VTodoQuery;
import net.bluemind.todolist.persistence.VTodoIndexStore;
import net.bluemind.user.api.IUserSubscription;

public class TodoListsService implements ITodoLists {
	private static final Logger logger = LoggerFactory.getLogger(TodoListsService.class);

	private BmContext ctx;

	public TodoListsService(BmContext context) {
		ctx = context;
	}

	@Override
	public void create(String uid, ContainerDescriptor descriptor) throws ServerFault {
		descriptor.type = ITodoUids.TYPE;

		if (descriptor.internalId > 0) {
			String hierUid = ContainerHierarchyNode.uidFor(uid, ITodoUids.TYPE, descriptor.domainUid);
			HierarchyIdsHints.putHint(hierUid, descriptor.internalId);
		}

		IServiceProvider sprovider = ctx.su().provider();
		sprovider.instance(IContainers.class).create(uid, descriptor);

		// acl
		IContainerManagement containerManagementService = ctx.su().provider().instance(IContainerManagement.class, uid);

		// FIXME maybe we do not need acl
		containerManagementService.setAccessControlList(
				Arrays.asList(AccessControlEntry.create(ctx.getSecurityContext().getSubject(), Verb.All)));

		// auto-subscribre
		IUserSubscription userSubService = ctx.su().provider().instance(IUserSubscription.class, descriptor.domainUid);
		userSubService.subscribe(descriptor.owner, Arrays.asList(ContainerSubscription.create(uid, false)));

	}

	@Override
	public void delete(String uid) throws ServerFault {
		DataSource ds = DataSourceRouter.get(ctx, uid);

		ContainerStore containerStore = new ContainerStore(ctx, ds, ctx.getSecurityContext());
		Container container;
		try {
			container = containerStore.get(uid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		if (container == null) {
			throw new ServerFault("todolist " + uid + " not found", ErrorCode.NOT_FOUND);
		}

		if (!ITodoUids.TYPE.equals(container.type)) {
			logger.warn("try to delete {} as a todolist but it's not a todolist but {}", uid, container.type);
			throw new ServerFault("todolist " + uid + " not found", ErrorCode.NOT_FOUND);
		}
		RBACManager.forContext(ctx).forContainer(container).check("Manage");

		VTodoContainerStoreService storeService = new VTodoContainerStoreService(ctx, ds, ctx.getSecurityContext(),
				container);
		VTodoIndexStore indexStore = new VTodoIndexStore(ESearchActivator.getClient(), container);

		storeService.prepareContainerDelete();
		indexStore.deleteAll();

		// delete container
		ctx.su().provider().instance(IContainers.class).delete(uid);

	}

	@Override
	public List<ItemContainerValue<VTodo>> search(TodoListsVTodoQuery query) throws ServerFault {
		List<ItemContainerValue<VTodo>> ret = new ArrayList<ItemContainerValue<VTodo>>();

		Set<String> containers = null == query.containers ? new HashSet<>() : new HashSet<>(query.containers);

		if (null != query.owner) {
			ContainerQuery containerQuery = ContainerQuery.ownerAndType(query.owner, "todolist");
			final IContainers containerService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IContainers.class);
			containerService.allForUser(ctx.getSecurityContext().getContainerUid(), query.owner, containerQuery)
					.stream().filter(cal -> {
						try {
							return RBACManager.forContext(ctx).forContainer(cal.uid).can(Verb.Read.name());
						} catch (Exception e) {
							return false;
						}
					}).forEach(cal -> containers.add(cal.uid));
		}

		if (null == query.owner && containers.isEmpty()) {
			List<ContainerSubscriptionDescriptor> subscriptions = ctx.getServiceProvider()
					.instance(IUserSubscription.class, ctx.getSecurityContext().getContainerUid())
					.listSubscriptions(ctx.getSecurityContext().getSubject(), ITodoUids.TYPE);

			for (ContainerSubscriptionDescriptor c : subscriptions) {
				containers.add(c.containerUid);
			}

		}

		for (String containerUid : containers) {

			try {

				ITodoList todolist = ctx.provider().instance(ITodoList.class, containerUid);

				VTodoQuery vtodoQuery = query.vtodoQuery;
				ListResult<ItemValue<VTodo>> resp = todolist.search(vtodoQuery);
				for (ItemValue<VTodo> vtodo : resp.values) {
					ItemContainerValue<VTodo> v = ItemContainerValue.create(containerUid, vtodo, vtodo.value);
					ret.add(v);
				}
			} catch (ServerFault e) {
				if (e.getCode() == ErrorCode.PERMISSION_DENIED) {
					logger.warn("user {} try to access {} but doesnt have right",
							ctx.getSecurityContext().getSubject() + "@" + ctx.getSecurityContext().getContainerUid(),
							containerUid);
				} else {
					throw e;
				}
			}
		}

		return ret;
	}

}
