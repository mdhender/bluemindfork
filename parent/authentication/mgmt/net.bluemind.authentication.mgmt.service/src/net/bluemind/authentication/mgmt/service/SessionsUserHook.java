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
package net.bluemind.authentication.mgmt.service;

import net.bluemind.authentication.mgmt.api.ISessionsMgmt;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.user.api.User;
import net.bluemind.user.hook.DefaultUserHook;

public class SessionsUserHook extends DefaultUserHook {
	@Override
	public void onUserDeleted(BmContext context, String domainUid, ItemValue<User> deleted) throws ServerFault {
		logout(context, domainUid, deleted);
	}

	@Override
	public void onUserUpdated(BmContext context, String domainUid, ItemValue<User> previous, ItemValue<User> current)
			throws ServerFault {
		if (!previous.value.archived && current.value.archived) {
			logout(context, domainUid, current);
		}
	}

	private void logout(BmContext context, String domainUid, ItemValue<User> user) throws ServerFault {
		ISessionsMgmt sessionApi = context.getServiceProvider().instance(ISessionsMgmt.class);
		sessionApi.logoutUser(user.value.login + '@' + domainUid);
	}
}