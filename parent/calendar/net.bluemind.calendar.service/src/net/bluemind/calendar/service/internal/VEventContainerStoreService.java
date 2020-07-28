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
package net.bluemind.calendar.service.internal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.persistence.VEventSeriesStore;
import net.bluemind.calendar.persistence.VEventStore.ItemUid;
import net.bluemind.calendar.service.VEventWeight;
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

public class VEventContainerStoreService extends ContainerStoreService<VEventSeries> {

	private IInCoreTagRef tagRefService;

	public VEventContainerStoreService(BmContext context, DataSource dataSource, SecurityContext securityContext,
			Container container, IItemValueStore<VEventSeries> itemValueStore) {
		super(dataSource, securityContext, container, itemValueStore, v -> ItemFlag.SEEN, VEventWeight.seedProvider(),
				VEventWeight.weigthProvider());

		tagRefService = context.su().provider().instance(IInCoreTagRef.class, container.uid);
	}

	public VEventContainerStoreService(BmContext context, DataSource dataSource, SecurityContext securityContext,
			Container container) {
		this(context, dataSource, securityContext, container, new VEventSeriesStore(dataSource, container));
	}

	@Override
	protected void decorate(List<Item> items, List<ItemValue<VEventSeries>> values) throws ServerFault {
		List<List<TagRef>> refs = tagRefService.getMultiple(items);

		Iterator<Item> itItems = items.iterator();
		Iterator<ItemValue<VEventSeries>> itValues = values.iterator();
		Iterator<List<TagRef>> itRefs = refs.iterator();
		for (; itItems.hasNext();) {
			itItems.next();
			ItemValue<VEventSeries> value = itValues.next();
			List<TagRef> ref = itRefs.next();
			if (value == null || value.value == null) {
				logger.warn("Skip broken value {}", value);
				continue;
			}
			if (value.value.main != null) {
				if (ref != null) {
					value.value.main.categories = ref;
				} else {
					value.value.main.categories = Collections.emptyList();
				}
			}
			value.value.occurrences.forEach(occurrence -> {
				if (ref != null) {
					occurrence.categories = ref;
				} else {
					occurrence.categories = Collections.emptyList();
				}
			});
		}

	}

	@Override
	protected void decorate(Item item, ItemValue<VEventSeries> value) throws ServerFault {
		if (value.value == null || value.value.main == null) {
			return;
		}

		List<TagRef> tags = tagRefService.get(item);
		value.value.main.categories = tags != null ? tags : Collections.emptyList();
		value.value.occurrences.forEach(occurrence -> {
			occurrence.categories = tags != null ? tags : Collections.emptyList();
		});

	}

	@Override
	protected void createValue(Item item, VEventSeries value) throws ServerFault, SQLException {
		super.createValue(item, value);
		if (value.main == null) {
			return;
		}
		List<TagRef> tags = value.main.categories;
		if (tags == null) {
			tags = Collections.emptyList();
		}
		tagRefService.create(item, tags);
	}

	@Override
	protected void updateValue(Item item, VEventSeries value) throws ServerFault, SQLException {
		super.updateValue(item, value);
		if (value.main == null) {
			return;
		}
		List<TagRef> tags = value.main.categories;
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
		super.deleteValues();
		tagRefService.deleteAll();
	}

	public List<String> getReminder(BmDateTime dtalarm) throws ServerFault {
		try {
			List<ItemUid> r = ((VEventSeriesStore) getItemValueStore()).getReminder(dtalarm);

			List<String> ret = new ArrayList<String>(r.size());
			for (ItemUid i : r) {
				ret.add(i.itemUid);

			}
			return ret;
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public List<ItemValue<VEventSeries>> getByIcsUid(String uid) {
		return doOrFail(() -> {
			List<String> uids = ((VEventSeriesStore) getItemValueStore()).findByIcsUid(uid);
			return getMultiple(uids);
		});
	}

}
