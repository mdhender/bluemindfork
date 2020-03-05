/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.system.ldap.importation.hooks;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.system.ldap.importation.api.LdapConstants;
import net.bluemind.user.api.ChangePassword;
import net.bluemind.user.api.IPasswordUpdater;
import net.bluemind.user.api.User;

public class LdapPasswordUpdater implements IPasswordUpdater {

	@Override
	public boolean update(SecurityContext context, String domainUid, ItemValue<User> user, ChangePassword password)
			throws ServerFault {
		if (user.externalId != null && user.externalId.startsWith(LdapConstants.EXTID_PREFIX)) {
			throw new ServerFault("Operation forbidden. Password must be changed in LDAP.", ErrorCode.FORBIDDEN);
		}
		return false;
	}

}
