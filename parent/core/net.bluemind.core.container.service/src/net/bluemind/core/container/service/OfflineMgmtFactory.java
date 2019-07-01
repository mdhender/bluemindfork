/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.core.container.service;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IOfflineMgmt;
import net.bluemind.core.container.service.internal.OfflineMgmtService;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;

public class OfflineMgmtFactory implements ServerSideServiceProvider.IServerSideServiceFactory<IOfflineMgmt> {

	@Override
	public Class<IOfflineMgmt> factoryClass() {
		return IOfflineMgmt.class;
	}

	@Override
	public IOfflineMgmt instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length != 2) {
			throw new ServerFault("2 parameters expected, domainUid, ownerUid");
		}
		String domainUid = params[0];
		String ownerUid = params[1];

		OfflineMgmtService service = new OfflineMgmtService(context, ownerUid, domainUid);
		return service;
	}

}
