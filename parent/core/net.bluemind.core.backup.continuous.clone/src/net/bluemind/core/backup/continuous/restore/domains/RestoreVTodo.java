/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.core.backup.continuous.restore.domains;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.todolist.api.VTodo;

public class RestoreVTodo implements RestoreDomainType {

	private static final ValueReader<ItemValue<VTodo>> mrReader = JsonUtils
			.reader(new TypeReference<ItemValue<VTodo>>() {
			});
	private final IServerTaskMonitor monitor;
	private IServiceProvider target;

	public RestoreVTodo(IServerTaskMonitor monitor, IServiceProvider target) {
		this.monitor = monitor;
		this.target = target;
	}

	@Override
	public String type() {
		return ITodoUids.TYPE;
	}

	@Override
	public void restore(DataElement de) {
		String payload = new String(de.payload);
		ITodoList todoApi = target.instance(ITodoList.class, de.key.uid);
		if (de.payload.length > 0) {
			createOrUpdate(payload, todoApi);
		} else {
			delete(de.key, todoApi);
		}
	}

	private void createOrUpdate(String payload, ITodoList todoApi) {
		ItemValue<VTodo> item = mrReader.read(payload);
		ItemValue<VTodo> existing = todoApi.getCompleteById(item.internalId);
		if (existing != null) {
			todoApi.updateWithItem(item);
			monitor.log("Update VTodo '" + item.displayName + "'");
		} else {
			todoApi.createWithItem(item);
			monitor.log("Create VTodo '" + item.displayName + "'");
		}
	}

	private void delete(RecordKey key, ITodoList todoApi) {
		try {
			todoApi.deleteById(key.id);
		} catch (Exception e) {
			monitor.log("Failed to delete resourceTypes: " + key);
		}
	}

}
