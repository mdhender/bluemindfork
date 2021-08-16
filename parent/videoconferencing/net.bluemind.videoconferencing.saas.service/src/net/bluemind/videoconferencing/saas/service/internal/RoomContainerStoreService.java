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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */

package net.bluemind.videoconferencing.saas.service.internal;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.videoconferencing.saas.api.BlueMindVideoRoom;
import net.bluemind.videoconferencing.saas.persistence.RoomStore;

public class RoomContainerStoreService extends ContainerStoreService<BlueMindVideoRoom> {

	private static final Logger logger = LoggerFactory.getLogger(RoomContainerStoreService.class);

	public RoomContainerStoreService(DataSource pool, SecurityContext securityContext, Container container) {
		super(pool, securityContext, container, new RoomStore(pool, container));
		this.hasChangeLog = false;
	}

	public ItemValue<BlueMindVideoRoom> byIdentifier(String identifier) throws ServerFault {
		try {
			String uid = ((RoomStore) itemValueStore).byIdentifier(identifier);
			return uid != null ? get(uid, null) : null;
		} catch (SQLException e) {
			logger.error("error during sql execution ", e);
			return null;
		}
	}
}
