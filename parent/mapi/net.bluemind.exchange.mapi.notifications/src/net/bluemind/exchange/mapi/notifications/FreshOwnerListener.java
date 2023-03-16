/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.exchange.mapi.notifications;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import net.bluemind.user.api.User;
import net.bluemind.user.hook.DefaultUserHook;

/**
 * We use this listener to disable notifications logic for new created users.
 * This is intended to speed up big directories imports.
 *
 */
public class FreshOwnerListener extends DefaultUserHook {

	public static final Cache<String, Object> FRESH_OWNERS = buildCache();
	private static final Object CONST_VALUE = new Object();

	private static final Map<String, Object> VIEW = FRESH_OWNERS.asMap();

	public static final boolean isFreshOwner(String uid) {
		return System.getProperty("mapi.notification.fresh") == null && VIEW.containsKey(uid);
	}

	private static Cache<String, Object> buildCache() {
		return Caffeine.newBuilder().expireAfterWrite(2, TimeUnit.SECONDS).build();
	}

	@Override
	public void beforeCreate(net.bluemind.core.rest.BmContext context, String domainUid, String uid, User user)
			throws net.bluemind.core.api.fault.ServerFault {
		FRESH_OWNERS.put(uid, CONST_VALUE);
	};

}
