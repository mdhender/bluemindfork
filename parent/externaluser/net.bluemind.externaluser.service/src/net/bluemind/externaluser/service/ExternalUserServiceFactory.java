/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.externaluser.service;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider.IServerSideServiceFactory;
import net.bluemind.directory.service.AbstractDirServiceFactory;
import net.bluemind.domain.api.Domain;
import net.bluemind.externaluser.api.IExternalUser;
import net.bluemind.externaluser.service.internal.ExternalUserService;

public class ExternalUserServiceFactory extends AbstractDirServiceFactory<IExternalUser>
		implements IServerSideServiceFactory<IExternalUser> {

	@Override
	public Class<IExternalUser> factoryClass() {
		return IExternalUser.class;
	}

	@Override
	protected IExternalUser instanceImpl(BmContext context, ItemValue<Domain> domainValue,
			Container externalUserContainer)
			throws ServerFault {
		return new ExternalUserService(context, domainValue, externalUserContainer);
	}

}
