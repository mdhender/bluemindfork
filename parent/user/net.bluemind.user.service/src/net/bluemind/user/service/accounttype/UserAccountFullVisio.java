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

import java.util.List;
import java.util.Set;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.user.api.User;

public class UserAccountFullVisio extends UserAccountProvider {

	@Override
	public Set<String> sanitizeRoles(BmContext bmContext, Set<String> roles, String domainName, ItemValue<User> user,
			List<String> groups) throws ServerFault {
		Set<String> sanitizeRoles = getSanitizedRoles(bmContext, roles, domainName, user, groups);
		if (visioSubscriptionIsActive(bmContext)) {
			sanitizeRoles.add(HAS_SIMPLE_VISIO);
			sanitizeRoles.add(HAS_FULL_VISIO);
		} else {
			if (sanitizeRoles.contains(HAS_SIMPLE_VISIO)) {
				sanitizeRoles.remove(HAS_SIMPLE_VISIO);
			}
		}
		return sanitizeRoles;
	}

	@Override
	public void updateRoles(BmContext context, String domainUid, String uid) {
		commonVisioUpdateRoles(context, domainUid, uid);
	}

}
