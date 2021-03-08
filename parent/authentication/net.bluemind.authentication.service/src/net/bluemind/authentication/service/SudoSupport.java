/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.authentication.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.authentication.api.ISudoSupport;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;

public class SudoSupport implements ISudoSupport {

	private static final Logger logger = LoggerFactory.getLogger(SudoSupport.class);
	private final BmContext context;

	public SudoSupport(BmContext context) {
		this.context = context;
	}

	public static class Factory implements ServerSideServiceProvider.IServerSideServiceFactory<ISudoSupport> {

		@Override
		public Class<ISudoSupport> factoryClass() {
			return ISudoSupport.class;
		}

		@Override
		public ISudoSupport instance(BmContext context, String... params) throws ServerFault {
			return new SudoSupport(context);
		}

	}

	@Override
	public void setOwner(String subject) {
		SecurityContext cur = context.getSecurityContext();
		cur.setOwnerPrincipal(subject);
		Sessions.get().put(cur.getSessionId(), cur);
		logger.info("Owner of {} updated to '{}'", cur, subject);

	}

}
