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
package net.bluemind.core.backup.continuous.restore.domains.crud;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.ITodoLists;
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.todolist.api.VTodo;

public class RestoreVTodo extends CrudRestore<VTodo> {
	private static final ValueReader<ItemValue<VTodo>> reader = JsonUtils.reader(new TypeReference<ItemValue<VTodo>>() {
	});
	private final IServiceProvider target;

	Set<String> validatedLists = ConcurrentHashMap.newKeySet();

	public RestoreVTodo(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target) {
		super(log, domain);
		this.target = target;
	}

	@Override
	public String type() {
		return ITodoUids.TYPE;
	}

	@Override
	protected ValueReader<ItemValue<VTodo>> reader() {
		return reader;
	}

	@Override
	protected ITodoList api(ItemValue<Domain> domain, RecordKey key) {
		if (!validatedLists.contains(key.uid)) {
			IContainers contApi = target.instance(IContainers.class);
			if (contApi.getIfPresent(key.uid) == null) {
				ITodoLists mgmtApi = target.instance(ITodoLists.class);
				mgmtApi.create(key.uid,
						ContainerDescriptor.create(key.uid, "todo-" + key.uid, key.owner, key.type, domain.uid, false));
				validatedLists.add(key.uid);
			}
		}

		return target.instance(ITodoList.class, key.uid);
	}

}
