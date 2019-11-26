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
package net.bluemind.hsm.service;

import java.sql.SQLException;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.hsm.api.IHSM;
import net.bluemind.hsm.service.internal.HSMService;

public class HSMServiceFactory implements ServerSideServiceProvider.IServerSideServiceFactory<IHSM> {

	@Override
	public Class<IHSM> factoryClass() {
		return IHSM.class;
	}

	@Override
	public IHSM instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 1) {
			throw new ServerFault("wrong number of instance parameters");
		}

		String domainUid = params[0];

		ContainerStore containerStore = new ContainerStore(context, context.getDataSource(),
				context.getSecurityContext());

		ItemValue<Domain> domainValue = context.su().provider().instance(IDomains.class).get(domainUid);

		if (domainValue == null) {
			throw new ServerFault("domain " + domainUid + " not found", ErrorCode.NOT_FOUND);
		}

		String mboxesContainerUid = domainUid;
		Container mboxesContainer = null;

		try {
			mboxesContainer = containerStore.get(mboxesContainerUid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		if (mboxesContainer == null) {
			throw new ServerFault("container " + mboxesContainerUid + " not found");
		}

		return new HSMService(context, mboxesContainer, domainValue);
	}

}
