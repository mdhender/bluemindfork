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

package net.bluemind.addressbook.adapter;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public final class Sudo implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(Sudo.class);

	public final SecurityContext context;

	public Sudo(String uid, String domainContainerUid) throws ServerFault {
		IUser u = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				domainContainerUid);
		ItemValue<User> theUser = u.getComplete(uid);
		if (theUser == null) {
			throw ServerFault.notFound("UID for " + uid + " in " + domainContainerUid + " not found.");
		}
		logger.debug("[{}] sudo login {} has uid {}", domainContainerUid, uid, theUser.uid);
		SecurityContext userContext = new SecurityContext(UUID.randomUUID().toString(), theUser.uid,
				Arrays.<String>asList(), Arrays.<String>asList(), Collections.emptyMap(), domainContainerUid, "en",
				"Sudo", false);
		Sessions.get().put(userContext.getSessionId(), userContext);
		this.context = userContext;
	}

	@Override
	public void close() {
		Sessions.get().invalidate(context.getSessionId());
	}

}
