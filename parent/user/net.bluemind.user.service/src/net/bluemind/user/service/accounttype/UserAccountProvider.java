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

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.group.api.IGroup;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.system.api.IInstallation;
import net.bluemind.system.api.SubscriptionInformations;
import net.bluemind.system.api.SubscriptionInformations.InstallationIndicator;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public abstract class UserAccountProvider implements IUserAccountProvider {

	protected static final String HAS_SIMPLE_VISIO = "hasSimpleVideoconferencing";
	protected static final String HAS_FULL_VISIO = "hasFullVideoconferencing";

	@Override
	public Set<String> sanitizeRoles(BmContext bmContext, Set<String> roles, String domainName, ItemValue<User> user,
			List<String> groups) throws ServerFault {
		return getSanitizedRoles(bmContext, roles, domainName, user, groups);
	}

	protected boolean visioSubscriptionIsActive(BmContext bmContext) {
		IInstallation installationService = bmContext.su().getServiceProvider().instance(IInstallation.class);
		SubscriptionInformations subInfos = installationService.getSubscriptionInformations();

		Optional<InstallationIndicator> fullVisioIndicator = subInfos.indicator.stream()
				.filter(indicator -> indicator.kind == InstallationIndicator.Kind.FullVisioAccount).findFirst();

		return fullVisioIndicator.isPresent() && fullVisioIndicator.get().expiration != null
				&& Calendar.getInstance().getTime().before(fullVisioIndicator.get().expiration);
	}

	protected void commonVisioUpdateRoles(BmContext bmContext, String domainUid, String uid) {
		IUser userService = bmContext.getServiceProvider().instance(IUser.class, domainUid);
		Set<String> roles = new HashSet<>(userService.getRoles(uid));
		if (!roles.contains(HAS_SIMPLE_VISIO)) {
			roles.add(HAS_SIMPLE_VISIO);
			userService.setRoles(uid, roles);
		}
	}

	protected static Set<String> getSanitizedRoles(BmContext bmContext, Set<String> roles, String domainName,
			ItemValue<User> user, List<String> groups) {
		IGroup groupService = bmContext.su().provider().instance(IGroup.class, domainName);
		Set<String> sanitizeRoles = new HashSet<>(roles);

		for (String groupUid : groups) {
			sanitizeRoles.addAll(groupService.getRoles(groupUid));
		}

		if (user.value.routing == Routing.none && sanitizeRoles.contains("hasMail")) {
			LoggerFactory.getLogger(UserAccountProvider.class).warn(
					"user {}@{} has \"hasMail\" role but routing == none, remove \"hasMail\" role", user.uid,
					domainName);
			sanitizeRoles.remove("hasMail");
		}

		return sanitizeRoles;
	}
}
