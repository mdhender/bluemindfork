/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.replica.service;

import java.sql.SQLException;

import javax.sql.DataSource;

import net.bluemind.backend.mail.api.IMailConversationActions;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordStore;
import net.bluemind.backend.mail.replica.service.internal.MailConversationActionsService;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;

public class MailConversationActionsServiceFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<IMailConversationActions> {

	@Override
	public Class<IMailConversationActions> factoryClass() {
		return IMailConversationActions.class;
	}

	@Override
	public IMailConversationActions instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 2) {
			throw new ServerFault("wrong number of instance parameters");
		}
		String subtreeContainerUid = params[0];
		String replicatedMailboxUid = params[1];

		DataSource ds = DataSourceRouter.get(context, subtreeContainerUid);
		if (ds == context.getDataSource()) {
			throw new ServerFault("Service is invoked with directory datasource for " + subtreeContainerUid + ".");
		}

		ContainerStore containerStore = new ContainerStore(context, ds, context.getSecurityContext());
		Container subtreeContainer = null;
		try {
			subtreeContainer = containerStore.get(subtreeContainerUid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		if (subtreeContainer == null) {
			throw new ServerFault("container " + subtreeContainerUid + " not found", ErrorCode.NOT_FOUND);
		}

		if (!subtreeContainer.type.equals(IMailReplicaUids.REPLICATED_MBOXES)) {
			throw new ServerFault("Incompatible conversation container type: " + subtreeContainer.type + ", uid: "
					+ subtreeContainer.uid);
		}

		if (ds.equals(context.getDataSource())) {
			throw new ServerFault("wrong datasource container.uid " + subtreeContainer.uid);
		}

		String recordsContainerUid = IMailReplicaUids.mboxRecords(replicatedMailboxUid);
		ContainerStore cs = new ContainerStore(context, ds, context.getSecurityContext());
		Container recordsContainer;
		try {
			recordsContainer = cs.get(recordsContainerUid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		MailboxRecordStore recordStore = new MailboxRecordStore(ds, recordsContainer, subtreeContainer);
		return new MailConversationActionsService(context, ds, subtreeContainer, replicatedMailboxUid, recordStore);
	}

}
