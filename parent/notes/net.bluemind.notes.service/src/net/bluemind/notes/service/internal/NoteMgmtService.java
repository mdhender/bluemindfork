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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.notes.service.internal;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.rest.BmContext;
import net.bluemind.notes.api.VNote;
import net.bluemind.notes.persistence.VNoteStore;
import net.bluemind.notes.service.INoteMgmt;

public class NoteMgmtService implements INoteMgmt {

	private final ContainerStoreService<VNote> storeService;
	private final Logger logger = LoggerFactory.getLogger(NoteMgmtService.class);
	private final Container container;
	private final BmContext context;

	public NoteMgmtService(BmContext context, DataSource dataSource, Container container) {
		this.container = container;
		VNoteStore noteStore = new VNoteStore(dataSource, container);
		this.context = context;
		this.storeService = new ContainerStoreService<>(dataSource, context.getSecurityContext(), container, noteStore);
	}

	@Override
	public void prepareContainerDelete() {
		logger.info("Preparing container deletion of {}", container.uid);
		storeService.prepareContainerDelete();
	}

}
