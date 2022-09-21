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
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.service.BackupDataProvider;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.todolist.api.VTodo;
import net.bluemind.todolist.api.VTodoChanges;

public class RestoreTodolistsTask extends BlockingServerTask implements IServerTask {
	private static final Logger logger = LoggerFactory.getLogger(RestoreTodolistsTask.class);

	private final DataProtectGeneration backup;
	private final Restorable item;

	public RestoreTodolistsTask(DataProtectGeneration backup, Restorable item) {
		this.backup = backup;
		this.item = item;
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		monitor.begin(10, String.format("Starting restore for uid %s", item.entryUid));
		try (BackupDataProvider bdp = new BackupDataProvider(null, SecurityContext.SYSTEM, monitor)) {
			BmContext back = bdp.createContextWithData(backup, item);
			BmContext live = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();

			IContainers liveContApi = live.provider().instance(IContainers.class);
			List<ContainerDescriptor> liveLists = liveContApi
					.all(ContainerQuery.ownerAndType(item.liveEntryUid(), ITodoUids.TYPE));

			IContainers backContApi = back.provider().instance(IContainers.class);
			List<ContainerDescriptor> backLists = backContApi
					.all(ContainerQuery.ownerAndType(item.entryUid, ITodoUids.TYPE));

			monitor.begin(backLists.size(),
					String.format("Starting restore for uid %s: Backup contains %d", item.entryUid, backLists.size()));

			logger.info("Backup contains " + backLists.size() + " todolist(s)");
			for (ContainerDescriptor backList : backLists) {
				restore(back, live, backList, liveLists, monitor.subWork(1));
			}
		} catch (Exception e) {
			logger.error("Error while restoring todolists", e);
			monitor.end(false, "finished with errors : " + e.getMessage(), "[]");
			return;
		}
		monitor.end(true, "finished.", "[]");
	}

	private void restore(BmContext back, BmContext live, ContainerDescriptor backList,
			List<ContainerDescriptor> liveLists, IServerTaskMonitor monitor) {
		IContainers liveContApi = live.provider().instance(IContainers.class);
		ITodoList backupListApi = back.provider().instance(ITodoList.class, backList.uid);

		List<String> allUids = backupListApi.allUids();
		monitor.begin(allUids.size() + 1, "Restoring " + backList.name + " [uid=" + backList.uid + "]");

		String listUid = mapListUid(backList.uid);

		if (liveLists.stream().filter(c -> c.uid.equals(listUid)).findFirst().isPresent()) {
			ITodoList liveABApi = live.provider().instance(ITodoList.class, listUid);
			liveABApi.reset();
			monitor.progress(1, "reset done");
		} else {
			backList.owner = item.liveEntryUid();
			liveContApi.create(listUid, backList);
			monitor.progress(1, "todolist recreated");
		}

		ITodoList liveListApi = live.provider().instance(ITodoList.class, listUid);

		for (List<String> batch : Lists.partition(backupListApi.allUids(), 1000)) {
			List<ItemValue<VTodo>> todos = backupListApi.multipleGet(batch);
			VTodoChanges changes = VTodoChanges.create(todos.stream()
					.map(e -> VTodoChanges.ItemAdd.create(e.uid, e.value, false)).collect(Collectors.toList()),
					Collections.emptyList(), Collections.emptyList());
			liveListApi.updates(changes);
			monitor.progress(batch.size(), null);
		}
	}

	private String mapListUid(String uid) {
		if (!item.entryUid.equals(item.liveEntryUid()) && uid.endsWith(String.format("_%s", item.entryUid))) {
			return String.format("%s%s", uid.substring(0, uid.length() - item.entryUid.length()), item.liveEntryUid());
		}

		return uid;
	}
}
