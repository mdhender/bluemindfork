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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.IItemValueStore;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.tag.api.TagRef;
import net.bluemind.tag.service.IInCoreTagRef;
import net.bluemind.todolist.api.VTodo;
import net.bluemind.todolist.persistence.VTodoStore;
import net.bluemind.todolist.service.VTodoWeight;

public class VTodoContainerStoreService extends ContainerStoreService<VTodo> {

	private IInCoreTagRef tagRefService;

	public VTodoContainerStoreService(BmContext context, DataSource dataSource, SecurityContext securityContext,
			Container container, IItemValueStore<VTodo> itemValueStore) {
		super(dataSource, securityContext, container, itemValueStore, todo -> ItemFlag.SEEN, VTodoWeight.seedProvider(),
				VTodoWeight.weigthProvider());
		this.tagRefService = context.su().provider().instance(IInCoreTagRef.class, container.uid);
	}

	public VTodoContainerStoreService(BmContext context, DataSource dataSource, SecurityContext securityContext,
			Container container) {
		this(context, dataSource, securityContext, container, new VTodoStore(dataSource, container));
	}

	@Override
	protected void decorate(Item item, ItemValue<VTodo> value) throws ServerFault {
		try {
			value.value.categories = tagRefService.get(item);
		} catch (ServerFault sf) {
			logger.error(sf.getMessage(), sf);
			return;
		}
	}

	@Override
	protected void createValue(Item item, VTodo value) throws ServerFault, SQLException {
		super.createValue(item, value);
		List<TagRef> tags = value.categories;
		if (tags == null) {
			tags = Collections.emptyList();
		}
		tagRefService.create(item, tags);
	}

	@Override
	protected void updateValue(Item item, VTodo value) throws ServerFault, SQLException {
		super.updateValue(item, value);
		List<TagRef> tags = value.categories;
		if (tags == null) {
			tags = Collections.emptyList();
		}
		tagRefService.update(item, tags);
	}

	@Override
	protected void deleteValue(Item item) throws ServerFault, SQLException {
		super.deleteValue(item);
		tagRefService.delete(item);
	}

	@Override
	protected void deleteValues() throws ServerFault {
		tagRefService.deleteAll();
		super.deleteValues();
	}

	public List<String> getReminder(BmDateTime dtalarm) throws ServerFault {
		try {
			List<VTodoStore.ItemUid> r = ((net.bluemind.todolist.persistence.VTodoStore) getItemValueStore())
					.getReminder(dtalarm);

			return r.stream().map((i) -> i.itemUid).collect(Collectors.toList());

		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

}
