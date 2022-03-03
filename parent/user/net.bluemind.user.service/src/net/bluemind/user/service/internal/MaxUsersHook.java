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
package net.bluemind.user.service.internal;

import java.util.Arrays;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.BaseDirEntry.AccountType;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.user.api.User;
import net.bluemind.user.hook.DefaultUserHook;
import net.bluemind.user.hook.IUserHook;
import net.bluemind.user.service.accounttype.UserAccountFactory;

public class MaxUsersHook extends DefaultUserHook implements IUserHook {

	private static final Logger logger = LoggerFactory.getLogger(MaxUsersHook.class);

	@Override
	public void beforeCreate(BmContext context, String domainUid, String uid, User user) throws ServerFault {

		String maxAccountSettingsKey = UserAccountFactory.getMaxSettingsKeyByAccountType(user.accountType);

		Map<String, String> settings = context.su().provider().instance(IDomainSettings.class, domainUid).get();

		if ((user.accountType == AccountType.SIMPLE || user.accountType == AccountType.FULL_AND_VISIO)
				&& !settings.containsKey(maxAccountSettingsKey)) {
			logger.error("Unable to create {} account for domain {}", user.accountType.name(), domainUid);
			throw new ServerFault("Unable to create " + user.accountType.name() + " account for domain " + domainUid);
		}

		String val = settings.get(maxAccountSettingsKey);
		if (!hasLimit(val)) {
			return;
		}

		int max = Integer.parseInt(val);
		if (max == 0) {
			logger.error("Unable to create {} account for domain {}", user.accountType.name(), domainUid);
			throw new ServerFault("Unable to create " + user.accountType.name() + " account for domain " + domainUid);
		}

		IDirectory service = context.su().provider().instance(IDirectory.class, domainUid);
		DirEntryQuery query = new DirEntryQuery();
		query.kindsFilter = Arrays.asList(Kind.USER);
		query.accountTypeFilter = user.accountType;
		ListResult<ItemValue<DirEntry>> users = service.search(query);
		if (users.total >= max) {
			logger.error("Maximum {} accounts allowed ({}) for domain {} reached. Unable to create new one.",
					user.accountType.name(), max, domainUid);
			throw new ServerFault("Maximum " + user.accountType.name() + " accounts allowed (" + max + ") for domain "
					+ domainUid + " reached. Unable to create new one.", ErrorCode.FORBIDDEN);
		}
	}

	private boolean hasLimit(String val) {
		return null != val && !val.isEmpty();
	}

	//
	@Override
	public boolean handleGlobalVirt() {
		return false;
	}

}
