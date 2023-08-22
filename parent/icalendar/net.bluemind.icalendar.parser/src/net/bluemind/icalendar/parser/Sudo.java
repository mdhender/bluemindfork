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

package net.bluemind.icalendar.parser;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public final class Sudo implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(Sudo.class);

	private final ItemValue<User> user;
	public final SecurityContext context;

	private Sudo(ItemValue<User> user, String domainContainerUid) throws ServerFault {
		this.user = user;
		logger.debug("[{}] sudo login of uid {}", domainContainerUid, user.uid);
		SecurityContext userContext = new SecurityContext(UUID.randomUUID().toString(), user.uid,
				Arrays.<String>asList(), Arrays.<String>asList(), Collections.emptyMap(), domainContainerUid, "en",
				"Sudo", false);
		Sessions.get().put(userContext.getSessionId(), userContext);
		this.context = userContext;
	}

	public static Sudo byEmail(String email, String domain) {
		return sudo(Optional.empty(), email, domain, (service, input) -> service.byEmail(email));
	}

	public static Sudo byUid(String uid, String domain) {
		return sudo(Optional.empty(), uid, domain, (service, input) -> service.getComplete(uid));
	}

	public static Sudo byEmail(String email, String domain, IServiceProvider provider) {
		return sudo(Optional.of(provider), email, domain, (service, input) -> service.byEmail(email));
	}

	public static Sudo byUid(String uid, String domain, IServiceProvider provider) {
		return sudo(Optional.of(provider), uid, domain, (service, input) -> service.getComplete(uid));
	}

	private static Sudo sudo(Optional<IServiceProvider> provider, String input, String domain,
			BiFunction<IUser, String, ItemValue<User>> lookup) {
		IUser service = provider.orElse(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM))
				.instance(IUser.class, domain);
		ItemValue<User> user = lookup.apply(service, input);
		if (user == null) {
			throw ServerFault.notFound(input + " in " + domain + " not found.");
		}
		return new Sudo(user, domain);

	}

	public ItemValue<User> getUser() {
		return user;
	}

	@Override
	public void close() {
		Sessions.get().invalidate(context.getSessionId());
	}

}
