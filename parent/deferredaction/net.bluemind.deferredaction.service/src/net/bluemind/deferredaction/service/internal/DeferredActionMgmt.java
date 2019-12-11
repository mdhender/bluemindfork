/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.rest.BmContext;
import net.bluemind.deferredaction.persistence.DeferredActionStore;
import net.bluemind.deferredaction.service.IDeferredActionMgmt;

public class DeferredActionMgmt implements IDeferredActionMgmt {

	private final ContainerDeferredActionStoreService storeService;
	private final Logger logger = LoggerFactory.getLogger(DeferredActionMgmt.class);
	private final Container container;

	public DeferredActionMgmt(BmContext context, DataSource dataSource, Container container) {
		this.container = container;
		DeferredActionStore deferredActionStore = new DeferredActionStore(dataSource, container);
		this.storeService = new ContainerDeferredActionStoreService(dataSource, context.getSecurityContext(), container,
				"deferredAction", deferredActionStore);
	}

	@Override
	public void prepareContainerDelete() {
		logger.info("Preparing container deletion of {}", container.uid);
		storeService.prepareContainerDelete();
	}

}
