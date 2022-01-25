/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.user.service;

import java.util.List;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.user.api.IInternalUserMailIdentities;
import net.bluemind.user.hook.identity.IUserMailIdentityHook;
import net.bluemind.user.service.internal.UserMailIdentities;

public class InternalUserMailIdentitiesFactory extends UserMailIdentitiesBaseFactory<IInternalUserMailIdentities> {

	@Override
	public Class<IInternalUserMailIdentities> factoryClass() {
		return IInternalUserMailIdentities.class;
	}

	@Override
	protected IInternalUserMailIdentities create(BmContext context, ItemValue<Domain> domainValue, Container container,
			String userUid, List<IUserMailIdentityHook> identityHooks) {
		return new UserMailIdentities(context, domainValue, container, userUid, identityHooks);
	}

}
