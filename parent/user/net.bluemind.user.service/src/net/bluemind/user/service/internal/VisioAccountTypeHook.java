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

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.BaseDirEntry.AccountType;
import net.bluemind.user.api.User;
import net.bluemind.user.hook.DefaultUserHook;
import net.bluemind.user.hook.IUserHook;
import net.bluemind.user.service.accounttype.UserAccountFactory;

public class VisioAccountTypeHook extends DefaultUserHook implements IUserHook {

	@Override
	public void onUserCreated(BmContext context, String domainUid, ItemValue<User> created) throws ServerFault {
		UserAccountFactory.get(created.value.accountType).updateRoles(context, domainUid, created.uid);
	}

	@Override
	public void beforeUpdate(BmContext context, String domainUid, String uid, User update, User previous)
			throws ServerFault {
		UserAccountFactory.get(update.accountType).updateRoles(context, domainUid, uid);
	}

	@Override
	public void onAccountTypeUpdated(BmContext context, String domainUid, String uid, AccountType update)
			throws ServerFault {
		UserAccountFactory.get(update).updateRoles(context, domainUid, uid);
	}

}
