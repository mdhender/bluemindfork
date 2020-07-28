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
package net.bluemind.server.service.internal;

import java.sql.SQLException;
import java.util.List;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.rest.BmContext;
import net.bluemind.server.api.Assignment;
import net.bluemind.server.api.Server;
import net.bluemind.server.persistence.ServerStore;

public class ServerStoreService extends ContainerStoreService<Server> {

	private ServerStore serverStore;

	public ServerStoreService(BmContext context, Container container) {
		super(context.getDataSource(), context.getSecurityContext(), container,
				new ServerStore(context.getDataSource(), container));
		serverStore = (ServerStore) getItemValueStore();
	}

	public void unassign(String serverUid, String domainUid, String tag) throws ServerFault {
		doOrFail(() -> {
			serverStore.unassign(serverUid, domainUid, tag);
			return null;
		});
	}

	public List<Assignment> getAssignments(String domainUid) throws ServerFault {
		try {
			return serverStore.getAssignments(domainUid);
		} catch (SQLException e) {
			throw new ServerFault(e);
		}
	}

	public void assign(String serverUid, String domainUid, String tag) throws ServerFault {
		doOrFail(() -> {
			serverStore.assign(serverUid, domainUid, tag);
			return null;
		});
	}

	public List<Assignment> getServerAssignements(String uid) throws ServerFault {
		try {
			return serverStore.getServerAssignments(uid);
		} catch (SQLException e) {
			throw new ServerFault(e);
		}
	}

}
