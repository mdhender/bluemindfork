/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.user.service.accounttype;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.directory.api.BaseDirEntry.AccountType;
import net.bluemind.domain.api.DomainSettingsKeys;

public class UserAccountFactory {

	public static IUserAccountProvider get(AccountType accountType) {
		switch (accountType) {
		case SIMPLE:
			return new UserAccountSimple();
		case FULL:
			return new UserAccountFull();
		case FULL_AND_VISIO:
			return new UserAccountFullVisio();
		default:
			throw new ServerFault("No supported implementation found");
		}
	}

	public static String getMaxSettingsKeyByAccountType(AccountType accountType) {
		switch (accountType) {
		case SIMPLE:
			return DomainSettingsKeys.domain_max_basic_account.name();
		case FULL_AND_VISIO:
			return DomainSettingsKeys.domain_max_fullvisio_accounts.name();
		case FULL:
		default:
			return DomainSettingsKeys.domain_max_users.name();
		}
	}

}
