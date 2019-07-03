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

package net.bluemind.backend.cyrus;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public final class Sudo implements AutoCloseable {

	public final SecurityContext context;

	private Sudo(String userUid, String domainUid) {
		SecurityContext userContext = new SecurityContext(UUID.randomUUID().toString(), userUid,
				Arrays.<String>asList(), Arrays.<String>asList(), Collections.emptyMap(), domainUid, "en", "sudo",
				false);
		Sessions.get().put(userContext.getSessionId(), userContext);
		this.context = userContext;
	}

	@Override
	public void close() {
		Sessions.get().invalidate(context.getSessionId());
	}

	public static Sudo forLogin(String login, String domain) throws ServerFault {
		IUser u = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class, domain);
		ItemValue<User> theUser = u.byLogin(login);
		if (theUser == null) {
			throw ServerFault.notFound("UID for " + login + " in " + domain + " not found.");
		}

		return new Sudo(theUser.uid, domain);
	}

	public static Sudo forUser(ItemValue<User> user, String domain) {
		return new Sudo(user.uid, domain);
	}
}
