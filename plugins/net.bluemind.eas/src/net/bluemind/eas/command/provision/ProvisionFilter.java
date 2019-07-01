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
package net.bluemind.eas.command.provision;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eas.config.global.GlobalConfig;
import net.bluemind.eas.http.AuthenticatedEASQuery;
import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.http.IEasRequestFilter;
import net.bluemind.eas.impl.Responder;
import net.bluemind.eas.impl.vertx.compat.VertxResponder;

/**
 * Checks the policy key & command to see if provision dialog is required
 */
public class ProvisionFilter implements IEasRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(ProvisionFilter.class);

	@Override
	public int priority() {
		return 2;
	}

	@Override
	public void filter(AuthenticatedEASQuery query, FilterChain next) {
		next.filter(query);
	}

	@Override
	public void filter(AuthorizedDeviceQuery query, FilterChain next) {
		if (skipProvision(query)) {
			next.filter(query);
		} else {
			logger.info("[{}] Provisioning is needed. (method {}, command {}, policyKey {})", query.loginAtDomain(),
					query.request().method(), query.command(), query.policyKey());
			Responder resp = new VertxResponder(query.request(), query.request().response());
			if (query.protocolVersion() < 14) {
				resp.sendStatus(449);
			} else {
				ProvisionHelper.forceProvisionProto14(query.command(), resp);
			}
		}
	}

	private boolean skipProvision(AuthorizedDeviceQuery query) {
		// No provision for Ping and Autodiscover command
		return GlobalConfig.DISABLE_POLICIES || "Ping".equals(query.command()) || "Autodiscover".equals(query.command())
				|| "OPTIONS".equals(query.request().method()) || Policies.hasValidPolicy(query);
	}
}