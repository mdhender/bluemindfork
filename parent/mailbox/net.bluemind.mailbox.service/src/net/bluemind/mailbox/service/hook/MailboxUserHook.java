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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.mailbox.service.hook;

import java.util.Arrays;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.user.api.IUserSubscription;
import net.bluemind.user.api.User;
import net.bluemind.user.hook.DefaultUserHook;

public class MailboxUserHook extends DefaultUserHook {

	@Override
	public void onUserCreated(BmContext context, String domainUid, ItemValue<User> created) throws ServerFault {
		IUserSubscription userSubService = ServerSideServiceProvider.getProvider(context.getSecurityContext())
				.instance(IUserSubscription.class, domainUid);
		userSubService.subscribe(created.uid,
				Arrays.asList(ContainerSubscription.create(IMailboxAclUids.uidForMailbox(created.uid), true)));

	}

}
