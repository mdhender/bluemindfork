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
package net.bluemind.core.container.service;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.internal.IInternalContainersFlatHierarchyMgmt;
import net.bluemind.core.container.service.internal.InternalContainersHierarchyMgmtService;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;

public class InternalContainersHierarchyMgmtFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<IInternalContainersFlatHierarchyMgmt> {

	@Override
	public Class<IInternalContainersFlatHierarchyMgmt> factoryClass() {
		return IInternalContainersFlatHierarchyMgmt.class;
	}

	@Override
	public IInternalContainersFlatHierarchyMgmt instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length != 2) {
			throw new ServerFault("2 parameters expected, domainUid, ownerUid");
		}
		String domainUid = params[0];
		String ownerUid = params[1];

		return new InternalContainersHierarchyMgmtService(context, ownerUid, domainUid);
	}

}
