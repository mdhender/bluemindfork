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

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.todolist.api.VTodo;

public class RestoreVTodo extends CrudRestore<VTodo> {
	private static final ValueReader<ItemValue<VTodo>> reader = JsonUtils.reader(new TypeReference<ItemValue<VTodo>>() {
	});
	private final IServiceProvider target;

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
		return target.instance(ITodoList.class, key.uid);
	}

}
