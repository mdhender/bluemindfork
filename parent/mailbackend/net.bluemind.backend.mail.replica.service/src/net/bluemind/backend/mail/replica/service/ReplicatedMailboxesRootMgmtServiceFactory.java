/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.replica.api.IReplicatedMailboxesRootMgmt;
import net.bluemind.backend.mail.replica.service.internal.ReplicatedMailboxesRootMgmtService;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;

public class ReplicatedMailboxesRootMgmtServiceFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<IReplicatedMailboxesRootMgmt> {

	@Override
	public Class<IReplicatedMailboxesRootMgmt> factoryClass() {
		return IReplicatedMailboxesRootMgmt.class;
	}

	private IReplicatedMailboxesRootMgmt getService(BmContext context, String partition) {
		CyrusPartition part = CyrusPartition.forName(partition);
		return new ReplicatedMailboxesRootMgmtService(context, part);
	}

	@Override
	public IReplicatedMailboxesRootMgmt instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 1) {
			throw new ServerFault("wrong number of instance parameters");
		}
		return getService(context, params[0]);
	}

}
