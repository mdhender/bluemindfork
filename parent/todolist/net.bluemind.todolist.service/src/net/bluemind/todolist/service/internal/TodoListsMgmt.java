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
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.todolist.api.ITodoListsMgmt;
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.todolist.api.VTodo;
import net.bluemind.todolist.persistence.VTodoIndexStore;
import net.bluemind.todolist.persistence.VTodoStore;
import net.bluemind.todolist.service.IInCoreTodoListsMgmt;

public class TodoListsMgmt implements ITodoListsMgmt, IInCoreTodoListsMgmt {
	static final Logger logger = LoggerFactory.getLogger(TodoListsMgmt.class);
	private BmContext context;

	public TodoListsMgmt(BmContext context) {
		this.context = context;
	}

	@Override
	public TaskRef reindexAll() throws ServerFault {
		return context.provider().instance(ITasksManager.class).run(reindexAllTask());
	}

	@Override
	public void reindexAll(IServerTaskMonitor monitor) throws Exception {
		reindexAllTask().run(monitor);
	}

	@Override
	public TaskRef reindex(String calUid) throws ServerFault {
		return context.provider().instance(ITasksManager.class).run(reindexTask(calUid));

	}

	@Override
	public void reindex(String calUid, IServerTaskMonitor monitor) throws Exception {
		reindexTask(calUid).run(monitor);
	}

	private IServerTask reindexAllTask() {
		return new IServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) throws Exception {
				if (!context.getSecurityContext().isDomainGlobal()) {
					throw new ServerFault("only admin0 can call this method ", ErrorCode.FORBIDDEN);
				}

				Set<String> all = getContainerUids();

				monitor.begin(all.size() + 1, "begin todolists reindexation [" + all.size() + "]");
				ESearchActivator.resetIndex("todo");
				monitor.progress(1, "Index todo reseted");

				for (String uid : all) {
					IServerTaskMonitor subMonitor = monitor.subWork("todolist [" + uid + "]", 1);

					reindex(uid, subMonitor);
				}
			}
		};
	}

	private IServerTask reindexTask(final String todolistUid) {
		return new IServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) throws Exception {
				DataSource ds = DataSourceRouter.get(context, todolistUid);
				ContainerStore containerStore = new ContainerStore(context, ds, context.getSecurityContext());

				Container c = containerStore.get(todolistUid);

				if (c == null) {
					throw new ServerFault("todolist " + todolistUid + " not found", ErrorCode.NOT_FOUND);
				}

				if (!context.getSecurityContext().isDomainAdmin(c.domainUid)) {
					throw new ServerFault("only admin of " + c.domainUid + " can call this method ",
							ErrorCode.FORBIDDEN);
				}

				reindex(c, monitor);
			}
		};
	}

	private void reindex(Container container, IServerTaskMonitor monitor) throws ServerFault {
		DataSource ds = DataSourceRouter.get(context, container.uid);
		VTodoContainerStoreService storeService = new VTodoContainerStoreService(context, ds,
				context.getSecurityContext(), container, new VTodoStore(ds, container));
		VTodoIndexStore indexStore = new VTodoIndexStore(ESearchActivator.getClient(), container);
		logger.info("reindexing todolist {}", container.uid);
		// reinit container index
		indexStore.deleteAll();

		List<String> uids = storeService.allUids();
		monitor.begin(uids.size() + 1, "reindexing todolist [" + container.uid + "] (size:" + uids.size() + ")");
		Lists.partition(uids, 500).forEach(subUids -> {
			List<ItemValue<VTodo>> values = storeService.getMultiple(subUids);
			indexStore.updates(values);
			monitor.progress(subUids.size(), "todolist [" + container.uid + "] reindexing...");
		});

		// only report one time
		monitor.progress(1, "todolist [" + container.uid + "] indexed");
		logger.info("todolist {} reindexed", container.uid);

	}

	private Set<String> getContainerUids() throws SQLException {
		Collection<DataSource> dataSources = context.getAllMailboxDataSource();
		Set<String> all = new LinkedHashSet<>();

		for (DataSource ds : dataSources) {
			ContainerStore cs = new ContainerStore(context, ds, context.getSecurityContext());
			List<Container> containers = cs.findByType(ITodoUids.TYPE);
			all.addAll(containers.stream().map(c -> c.uid).collect(Collectors.toList()));
		}

		ContainerStore cs = new ContainerStore(context, context.getDataSource(), context.getSecurityContext());
		List<Container> containers = cs.findByType(ITodoUids.TYPE);
		all.addAll(containers.stream().map(c -> c.uid).collect(Collectors.toList()));

		return all;
	}
}
