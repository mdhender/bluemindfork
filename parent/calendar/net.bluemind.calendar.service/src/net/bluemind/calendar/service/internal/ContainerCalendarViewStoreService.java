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

import javax.sql.DataSource;

import net.bluemind.calendar.api.CalendarView;
import net.bluemind.calendar.persistence.CalendarViewStore;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.context.SecurityContext;

/**
 * @author mehdi
 *
 */
public class ContainerCalendarViewStoreService extends ContainerStoreService<CalendarView> {

	/**
	 * @param pool
	 * @param securityContext
	 * @param container
	 * @param itemType
	 * @param itemValueStore
	 */
	public ContainerCalendarViewStoreService(DataSource pool, SecurityContext securityContext, Container container,
			String itemType, CalendarViewStore itemValueStore) {
		super(pool, securityContext, container, itemValueStore);
	}

	public void setDefault(String uid) throws ServerFault {
		doOrFail(() -> {
			Item item = itemStore.get(uid);
			if (item == null) {
				throw new ServerFault("Calendar view " + uid + " doesnt exists", ErrorCode.NOT_FOUND);
			}

			((CalendarViewStore) itemValueStore).setDefault(item);
			return null;
		});

	}
}
