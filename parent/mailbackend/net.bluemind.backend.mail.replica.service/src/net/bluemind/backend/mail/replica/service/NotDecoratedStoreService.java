/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.backend.mail.replica.service;

import java.util.List;

import javax.sql.DataSource;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.IItemValueStore;
import net.bluemind.core.container.persistence.IWeightProvider;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.context.SecurityContext;

public class NotDecoratedStoreService<T> extends ContainerStoreService<T> {

	public NotDecoratedStoreService(DataSource pool, SecurityContext securityContext, Container container,
			IItemValueStore<T> itemValueStore, IItemFlagsProvider<T> fProv, IWeightSeedProvider<T> wsProv,
			IWeightProvider wProv) {
		super(pool, securityContext, container, itemValueStore, fProv, wsProv, wProv);
	}

	@Override
	protected void decorate(List<Item> items, List<ItemValue<T>> values) {
		// Not needed
	}

}
