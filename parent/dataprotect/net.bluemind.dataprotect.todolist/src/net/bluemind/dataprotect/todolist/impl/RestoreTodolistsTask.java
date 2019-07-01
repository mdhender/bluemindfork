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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.dataprotect.todolist.impl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.service.BackupDataProvider;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.todolist.api.VTodo;
import net.bluemind.todolist.api.VTodoChanges;

public class RestoreTodolistsTask implements IServerTask {
	private static final Logger logger = LoggerFactory.getLogger(RestoreTodolistsTask.class);

	private DataProtectGeneration backup;
	private Restorable item;

	public RestoreTodolistsTask(DataProtectGeneration backup, Restorable item) {
		this.backup = backup;
		this.item = item;
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		monitor.begin(10, "starting restore for uid " + item.entryUid);
		try (BackupDataProvider bdp = new BackupDataProvider(null, SecurityContext.SYSTEM, monitor)) {
			BmContext back = bdp.createContextWithData(backup, item);
			BmContext live = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();

			ContainerQuery cq = ContainerQuery.ownerAndType(item.entryUid, ITodoUids.TYPE);

			IContainers lContApi = live.provider().instance(IContainers.class);
			List<ContainerDescriptor> liveLists = lContApi.all(cq);

			IContainers bContApi = back.provider().instance(IContainers.class);
			List<ContainerDescriptor> dataProtectedLists = bContApi.all(cq);

			monitor.begin(dataProtectedLists.size(), "starting restore for uid " + item.entryUid + " : Backup contains "
					+ dataProtectedLists.size() + " todolist(s)");

			logger.info("Backup contains " + dataProtectedLists.size() + " todolist(s)");
			for (ContainerDescriptor cd : dataProtectedLists) {

				restore(back, live, cd, liveLists, monitor.subWork(1));

			}
		} catch (Exception e) {
			logger.warn("Error while restoring todolists", e);
			monitor.end(false, "finished with errors : " + e.getMessage(), "[]");
			return;
		}
		monitor.end(true, "finished.", "[]");
	}

	private void restore(BmContext back, BmContext live, ContainerDescriptor cd, List<ContainerDescriptor> liveLists,
			IServerTaskMonitor monitor) {
		IContainers lContApi = live.provider().instance(IContainers.class);
		ITodoList backupListApi = back.provider().instance(ITodoList.class, cd.uid);

		List<String> allUids = backupListApi.allUids();
		monitor.begin(allUids.size() + 1, "Restoring " + cd.name + " [uid=" + cd.uid + "]");

		if (liveLists.stream().filter(c -> c.uid.equals(cd.uid)).findFirst().isPresent()) {
			ITodoList liveABApi = live.provider().instance(ITodoList.class, cd.uid);
			liveABApi.reset();
			monitor.progress(1, "reset done");
		} else {
			lContApi.create(cd.uid, cd);
			monitor.progress(1, "todolist recreated");
		}

		ITodoList liveListApi = live.provider().instance(ITodoList.class, cd.uid);

		for (List<String> batch : Lists.partition(backupListApi.allUids(), 1000)) {
			List<ItemValue<VTodo>> todos = backupListApi.multipleGet(batch);
			VTodoChanges changes = VTodoChanges.create(todos.stream()
					.map(e -> VTodoChanges.ItemAdd.create(e.uid, e.value, false)).collect(Collectors.toList()),
					Collections.emptyList(), Collections.emptyList());
			liveListApi.updates(changes);
			monitor.progress(batch.size(), null);
		}
	}
}
