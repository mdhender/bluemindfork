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
package net.bluemind.user.hook;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.user.api.User;

public class DefaultUserHook implements IUserHook {

	@Override
	public void onUserCreated(BmContext context, String domainUid, ItemValue<User> created) throws ServerFault {

	}

	@Override
	public void onUserUpdated(BmContext context, String domainUid, ItemValue<User> previous, ItemValue<User> current)
			throws ServerFault {

	}

	@Override
	public void onUserDeleted(BmContext context, String domainUid, ItemValue<User> deleted) throws ServerFault {

	}

	@Override
	public boolean handleGlobalVirt() {
		return false;
	}

	@Override
	public void beforeCreate(BmContext context, String domainUid, String uid, User user) throws ServerFault {

	}

	@Override
	public void beforeUpdate(BmContext context, String domainUid, String uid, User update, User previous)
			throws ServerFault {

	}

	@Override
	public void beforeDelete(BmContext context, String domainUid, String uid, User previous) throws ServerFault {

	}

}
