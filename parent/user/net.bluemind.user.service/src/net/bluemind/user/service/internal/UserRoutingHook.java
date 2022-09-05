/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.user.service.internal;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.DomainSettingsHelper;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.user.api.User;
import net.bluemind.user.hook.DefaultUserHook;
import net.bluemind.user.hook.IUserHook;

public class UserRoutingHook extends DefaultUserHook implements IUserHook {

	@Override
	public void beforeCreate(BmContext context, String domainUid, String uid, User user) throws ServerFault {
		validateRouting(domainUid, user);
	}

	@Override
	public void beforeUpdate(BmContext context, String domainUid, String uid, User update, User previous)
			throws ServerFault {
		validateRouting(domainUid, update);
	}

	private void validateRouting(String domainUid, User update) {
		if (update.routing == Mailbox.Routing.external) {
			String splitRelayHost = DomainSettingsHelper.getSlaveRelayHost(getSettingsService(domainUid));
			if (splitRelayHost == null || splitRelayHost.trim().isEmpty()) {
				throw new ServerFault("Routing is external but no split relay is defined", ErrorCode.INVALID_HOST_NAME);
			}
		}
	}

	private IDomainSettings getSettingsService(String domainUid) throws ServerFault {
		IDomainSettings settings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		return settings;
	}

}
