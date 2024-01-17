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
package net.bluemind.eas.impl.vertx.compat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.dto.device.DeviceId;
import net.bluemind.eas.dto.user.MSUser;
import net.bluemind.eas.exception.ActiveSyncException;
import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.impl.Backends;
import net.bluemind.eas.impl.SessionStates;

public final class SessionWrapper {

	private static final Logger logger = LoggerFactory.getLogger(SessionWrapper.class);

	public static BackendSession wrap(AuthorizedDeviceQuery query) {
		if (logger.isDebugEnabled()) {
			logger.debug("wrapping session for {} / {}", query.loginAtDomain(), query.deviceIdentifier());
		}
		try {
			MSUser user = Backends.dataAccess().getUser(query.loginAtDomain(), query.sid());
			DeviceId devId = new DeviceId(user.getLoginAtDomain(), query.deviceIdentifier(), query.deviceType(),
					query.partnershipId());
			BackendSession session = new BackendSession(user, devId, query.protocolVersion());
			DeviceId did = session.getDeviceId();
			session.setMutableState(SessionStates.get(did));
			if (query.policyKey() != null) {
				session.setPolicyKey(Long.toString(query.policyKey()));
			}
			session.setRequest(query.request());
			Backends.dataAccess().initInternalState(session);

			return session;
		} catch (ActiveSyncException e) {
			logger.error(e.getMessage(), e);
			throw Throwables.propagate(e);
		}
	}

}
