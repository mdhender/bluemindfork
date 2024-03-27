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
package net.bluemind.eas.sift;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.sentry.Sentry;
import net.bluemind.eas.http.AuthenticatedEASQuery;
import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.http.IEasRequestFilter;
import net.bluemind.eas.utils.EasLogUser;

public class PerUserLog implements IEasRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(PerUserLog.class);
	private static final String ANONYMOUS = "anonymous";

	@Override
	public int priority() {
		return 0;
	}

	@Override
	public void filter(AuthenticatedEASQuery query, FilterChain next) {
		if (logger.isDebugEnabled()) {
			EasLogUser.logDebugAsUser(query.loginAtDomain(), logger, "Sifting to {}", query.loginAtDomain());
		}
		io.sentry.protocol.User sentryUser = new io.sentry.protocol.User();
		sentryUser.setUsername(query.loginAtDomain());
		Sentry.setUser(sentryUser);
		next.filter(query);
	}

	@Override
	public void filter(AuthorizedDeviceQuery query, FilterChain next) {
		io.sentry.protocol.User sentryUser = new io.sentry.protocol.User();
		sentryUser.setUsername(query.loginAtDomain());
		Sentry.setUser(sentryUser);
		next.filter(query);
	}

}
