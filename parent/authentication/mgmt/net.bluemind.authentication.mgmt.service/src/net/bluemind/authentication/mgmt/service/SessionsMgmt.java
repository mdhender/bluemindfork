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
package net.bluemind.authentication.mgmt.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;

import net.bluemind.authentication.mgmt.api.ISessionsMgmt;
import net.bluemind.authentication.mgmt.api.SessionEntry;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.role.api.BasicRoles;

public class SessionsMgmt implements ISessionsMgmt {
	private static final Logger logger = LoggerFactory.getLogger(SessionsMgmt.class);
	protected static final Splitter atSplitter = Splitter.on('@').trimResults().omitEmptyStrings();

	private BmContext context;

	public SessionsMgmt(BmContext context) {
		this.context = context;
	}

	@Override
	public void logoutUser(String uid) {
		RBACManager.forContext(context).check(BasicRoles.ROLE_SYSTEM_MANAGER);

		Sessions.get().asMap().values().stream().filter(sc -> sc.getSubject().equals(uid))
				.forEach(this::invalidateSession);
	}

	private void invalidateSession(SecurityContext sc) {
		if (sc.getSessionId() != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("logout user {} session {}", sc.getSubject(), sc.getSessionId());
			}

			Sessions.get().invalidate(sc.getSessionId());
			VertxPlatform.eventBus().publish("core.user.push.queue.removed", "client.session." + sc.getSessionId());
		}
	}

	@Override
	public List<SessionEntry> list(String domainUid) {
		RBACManager.forContext(context).check(BasicRoles.ROLE_SYSTEM_MANAGER);
		return Sessions.get().asMap().values().stream()
				.filter(v -> domainUid != null ? v.getContainerUid().equals(domainUid) : true)
				.map(sc -> SessionEntry.build(sc.getCreated(),
						Optional.ofNullable(context.provider().instance(IDirectory.class, sc.getContainerUid())
								.findByEntryUid(sc.getSubject())).map(de -> de.email).orElse(null),
						sc.getContainerUid(), sc.getSubject(), sc.getOrigin(), sc.getRemoteAddresses()))
				.collect(Collectors.toList());
	}
}
