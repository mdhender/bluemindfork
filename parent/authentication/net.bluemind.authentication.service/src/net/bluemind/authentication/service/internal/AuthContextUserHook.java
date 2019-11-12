/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.authentication.service.internal;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.user.api.User;
import net.bluemind.user.hook.DefaultUserHook;

public class AuthContextUserHook extends DefaultUserHook {

	@Override
	public void onUserCreated(BmContext context, String domainUid, ItemValue<User> fresh) throws ServerFault {
		AuthContextCache.getInstance().getCache().invalidate(fresh.value.login + "@" + domainUid);
	}

	@Override
	public void onUserUpdated(BmContext context, String domainUid, ItemValue<User> previous, ItemValue<User> current)
			throws ServerFault {
		AuthContextCache.getInstance().getCache().invalidate(previous.value.login + "@" + domainUid);
	}

	@Override
	public void onUserDeleted(BmContext context, String domainUid, ItemValue<User> deleted) throws ServerFault {
		AuthContextCache.getInstance().getCache().invalidate(deleted.value.login + "@" + domainUid);
	}

}
