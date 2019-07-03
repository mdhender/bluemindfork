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
package net.bluemind.eas.impl;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.bluemind.eas.backend.SessionPersistentState;
import net.bluemind.eas.dto.device.DeviceId;

public class SessionStates {

	private static final Cache<DeviceId, SessionPersistentState> states = CacheBuilder.newBuilder()
			.expireAfterAccess(1, TimeUnit.HOURS).build();

	public static SessionPersistentState get(DeviceId did) {
		SessionPersistentState mutable = states.getIfPresent(did);
		if (mutable == null) {
			mutable = new SessionPersistentState();
			states.put(did, mutable);
		}
		return mutable;
	}

}
