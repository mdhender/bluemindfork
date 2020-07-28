/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.deferredaction.service.internal;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.IItemValueStore;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.deferredaction.api.DeferredAction;
import net.bluemind.deferredaction.persistence.DeferredActionStore;

public class ContainerDeferredActionStoreService extends ContainerStoreService<DeferredAction> {
	private DeferredActionStore store;

	public ContainerDeferredActionStoreService(DataSource pool, SecurityContext securityContext, Container container,
			IItemValueStore<DeferredAction> itemValueStore) {
		super(pool, securityContext, container, itemValueStore);
		store = (DeferredActionStore) itemValueStore;
	}

	public List<ItemValue<DeferredAction>> getByActionId(String actionId, Date to) {
		try {
			List<Long> ids = store.getByActionId(actionId, to);
			return getMultipleById(ids);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public List<ItemValue<DeferredAction>> getByReference(String reference) {
		try {
			List<Long> ids = store.getByReference(reference);
			return getMultipleById(ids);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

}
