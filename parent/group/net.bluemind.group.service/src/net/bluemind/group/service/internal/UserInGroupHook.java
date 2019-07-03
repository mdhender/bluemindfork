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
package net.bluemind.group.service.internal;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.group.api.IGroup;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;
import net.bluemind.user.hook.DefaultUserHook;

public class UserInGroupHook extends DefaultUserHook {

	@Override
	public void onUserUpdated(BmContext context, String domainUid, ItemValue<User> previous, ItemValue<User> current)
			throws ServerFault {
		if (previous.value.archived != current.value.archived) {
			IGroup groups = context.su().provider().instance(IGroup.class, domainUid);
			IUser userGroups = context.su().provider().instance(IUser.class, domainUid);
			for (String g : userGroups.memberOfGroups(current.uid)) {
				groups.touch(g);
			}

		}

	}

}
