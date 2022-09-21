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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.ImportStats;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.todolist.adapter.VTodoAdapter;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.VTodo;
import net.bluemind.todolist.api.VTodoChanges;
import net.bluemind.todolist.api.VTodoChanges.ItemAdd;
import net.bluemind.todolist.api.VTodoChanges.ItemModify;
import net.bluemind.todolist.api.VTodoQuery;

public class VTodoImportTask extends BlockingServerTask implements IServerTask {

	private static final Logger logger = LoggerFactory.getLogger(VTodoImportTask.class);
	private ITodoList todolist;
	private String vtodo;

	public VTodoImportTask(ITodoList todoService, String ics) {
		this.todolist = todoService;
		this.vtodo = ics;
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		monitor.begin(3, "Begin import");

		List<ItemValue<VTodo>> todos = new VTodoAdapter().convertToVTodoList(vtodo);
		monitor.progress(1, "VTODO parsed ( " + todos.size() + " tasks )");
		ImportStats ret = importAll(todos, monitor.subWork("", 2));
		// FIXME ret should be returned as ImportStats
		monitor.end(true, ret.total + " todos imported", JsonUtils.asString(ret));

	}

	private ImportStats importAll(List<ItemValue<VTodo>> todos, IServerTaskMonitor monitor) throws ServerFault {
		monitor.begin(todos.size(), "Import " + todos.size() + " todos");

		VTodoChanges changes = new VTodoChanges();
		changes.add = new ArrayList<VTodoChanges.ItemAdd>();
		changes.modify = new ArrayList<VTodoChanges.ItemModify>();
		changes.delete = new ArrayList<VTodoChanges.ItemDelete>();

		for (ItemValue<VTodo> itemValue : todos) {
			VTodo todo = itemValue.value;

			VTodoQuery query = new VTodoQuery();
			query.todoUid = itemValue.uid;
			ListResult<ItemValue<VTodo>> old = todolist.search(query);

			logger.info("Found {} existing tasks with value.uid {}", old.total, itemValue.uid);

			if (old.total == 0) {
				changes.add.add(ItemAdd.create(itemValue.uid != null ? itemValue.uid : UUID.randomUUID().toString(),
						todo, false));
			} else {
				ItemValue<VTodo> existing = old.values.get(0);
				logger.info("Updating Task {}", existing.uid);
				changes.modify.add(ItemModify.create(existing.uid, todo, false));
			}
			monitor.progress(1, "in progress");
		}

		ContainerUpdatesResult result = todolist.updates(changes);
		ImportStats ret = new ImportStats();
		ret.total = todos.size();
		ret.uids = new ArrayList<String>();
		ret.uids.addAll(result.added);
		ret.uids.addAll(result.updated);

		return ret;
	}

}
