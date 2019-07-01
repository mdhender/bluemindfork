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
package net.bluemind.exchange.mapi.service.internal;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.exchange.mapi.api.IMapiMailboxes;
import net.bluemind.exchange.mapi.api.MapiReplica;
import net.bluemind.exchange.mapi.persistence.MapiReplicaStore;

public class MapiMailboxesService implements IMapiMailboxes {

	private static final Logger logger = LoggerFactory.getLogger(MapiMailboxesService.class);

	private MapiReplicaStore mapiReplicaStore;

	public MapiMailboxesService(BmContext context, String domainUid) throws ServerFault {
		logger.debug("Creating for domain {}", domainUid);
		this.mapiReplicaStore = new MapiReplicaStore(context.getDataSource());
	}

	@Override
	public MapiReplica byMailboxGuid(String mailboxGuid) throws ServerFault {
		try {
			return mapiReplicaStore.byMailboxGuid(mailboxGuid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

}
