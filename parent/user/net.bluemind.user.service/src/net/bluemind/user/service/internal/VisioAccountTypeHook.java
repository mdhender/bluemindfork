/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.user.service.internal;

import java.util.HashSet;
import java.util.Set;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.BaseDirEntry.AccountType;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;
import net.bluemind.user.hook.DefaultUserHook;
import net.bluemind.user.hook.IUserHook;

public class VisioAccountTypeHook extends DefaultUserHook implements IUserHook {

	@Override
	public void onUserCreated(BmContext context, String domainUid, ItemValue<User> created) throws ServerFault {
		IUser user = context.getServiceProvider().instance(IUser.class, domainUid);
		Set<String> roles = new HashSet<>(user.getRoles(created.uid));
		if (created.value.accountType == AccountType.FULL) {
			if (!roles.contains("hasSimpleVideoconferencing")) {
				roles.add("hasSimpleVideoconferencing");
			}
		}
		user.setRoles(created.uid, roles);
	}

	@Override
	public void beforeUpdate(BmContext context, String domainUid, String uid, User update, User previous)
			throws ServerFault {
		IUser user = context.getServiceProvider().instance(IUser.class, domainUid);
		Set<String> roles = new HashSet<>(user.getRoles(uid));
		if (previous.accountType != AccountType.FULL_AND_VISIO && update.accountType == AccountType.FULL_AND_VISIO) {
			if (roles.contains("hasSimpleVideoconferencing")) { // role may have been removed in the meantime
				roles.remove("hasSimpleVideoconferencing");
			}
		}
		if (previous.accountType == AccountType.FULL_AND_VISIO && update.accountType == AccountType.FULL) {
			roles.add("hasSimpleVideoconferencing");
		}
		user.setRoles(uid, roles);
	}

}
