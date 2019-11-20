/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.authentication.service.internal;

import java.util.HashSet;
import java.util.Set;

import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.user.api.User;
import net.bluemind.user.hook.DefaultUserHook;

public class AuthContextUserHook extends DefaultUserHook {

	@Override
	public void onUserCreated(BmContext context, String domainUid, ItemValue<User> fresh) throws ServerFault {
		AuthContextCache.getInstance().getCache().invalidateAll(cacheKeys(context, domainUid, fresh));
	}

	@Override
	public void onUserUpdated(BmContext context, String domainUid, ItemValue<User> previous, ItemValue<User> current)
			throws ServerFault {
		Set<String> keys = cacheKeys(context, domainUid, previous);
		keys.addAll(cacheKeys(context, domainUid, current));
		AuthContextCache.getInstance().getCache().invalidateAll(keys);
	}

	@Override
	public void onUserDeleted(BmContext context, String domainUid, ItemValue<User> deleted) throws ServerFault {
		AuthContextCache.getInstance().getCache().invalidateAll(cacheKeys(context, domainUid, deleted));
	}

	private Set<String> cacheKeys(BmContext context, String domainUid, ItemValue<User> u) {
		ItemValue<Domain> domAliases = context.provider().instance(IDomains.class).get(domainUid);
		Set<String> expanded = new HashSet<>();
		expanded.add(u.value.login + "@" + domainUid);
		for (Email e : u.value.emails) {
			if (e.allAliases) {
				expanded.add(e.localPart() + "@" + domAliases.uid);
				domAliases.value.aliases.forEach(al -> expanded.add(e.localPart() + "@" + al));
			} else {
				expanded.add(e.address);
			}
		}
		return expanded;
	}

}
