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
package net.bluemind.user.service;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.rest.BmContext;
import net.bluemind.user.api.IInternalUserSubscription;
import net.bluemind.user.service.internal.UserSubscriptionService;

public class UserSubscriptionFactory extends UserSubscriptionBaseFactory<IInternalUserSubscription> {

	@Override
	public Class<IInternalUserSubscription> factoryClass() {
		return IInternalUserSubscription.class;
	}

	@Override
	protected IInternalUserSubscription create(BmContext context, Container domain) {
		return new UserSubscriptionService(context, domain);
	}

}
