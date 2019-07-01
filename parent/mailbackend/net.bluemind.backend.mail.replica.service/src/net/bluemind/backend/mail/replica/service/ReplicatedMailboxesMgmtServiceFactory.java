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

import net.bluemind.backend.mail.replica.api.IReplicatedMailboxesMgmt;
import net.bluemind.backend.mail.replica.service.internal.ReplicatedMailboxesMgmtService;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;

public class ReplicatedMailboxesMgmtServiceFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<IReplicatedMailboxesMgmt> {

	@Override
	public Class<IReplicatedMailboxesMgmt> factoryClass() {
		return IReplicatedMailboxesMgmt.class;
	}

	private IReplicatedMailboxesMgmt getService(BmContext context) {
		return new ReplicatedMailboxesMgmtService(context);
	}

	@Override
	public IReplicatedMailboxesMgmt instance(BmContext context, String... params) throws ServerFault {
		return getService(context);
	}

}
