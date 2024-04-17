/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.user.service.internal;

import org.apache.commons.lang3.StringUtils;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.user.api.ChangePassword;
import net.bluemind.user.api.IPasswordUpdater;
import net.bluemind.user.api.User;
import net.bluemind.user.persistence.security.HashAlgorithm;
import net.bluemind.user.persistence.security.HashFactory;
import net.bluemind.user.service.IInCoreUser;

public class DatabasePasswordUpdater implements IPasswordUpdater {
	@Override
	public boolean update(SecurityContext context, String domainUid, ItemValue<User> userItem, ChangePassword password)
			throws ServerFault {
		if (StringUtils.isBlank(password.currentPassword)) {
			setPassword(context, domainUid, userItem, password.newPassword);
		} else {
			changePassword(context, domainUid, userItem, password.currentPassword, password.newPassword);
		}

		return true;
	}

	private void changePassword(SecurityContext context, String domainUid, ItemValue<User> user, String currentPassword,
			String newPassword) throws ServerFault {
		UserService userService = (UserService) ServerSideServiceProvider.getProvider(context)
				.instance(IInCoreUser.class, domainUid);

		userService.passwordValidator.validate(newPassword);

		if (Boolean.FALSE.equals(userService.checkPassword(user, currentPassword))) {
			throw new ServerFault("Invalid password for user: " + user.uid, ErrorCode.AUTHENTICATION_FAIL);
		}

		userService.setPassword(user.uid, HashFactory.getDefault().create(newPassword), true);
		new UserEventProducer(domainUid, VertxPlatform.eventBus()).passwordUpdated(user.uid);
		// ysnp cache invalidation
		MQ.getProducer(Topic.CORE_SESSIONS)
				.send(new JsonObject().put("latd", user.value.login + "@" + domainUid).put("operation", "pwchange"));
	}

	private void setPassword(SecurityContext context, String domainUid, ItemValue<User> user, String newPassword)
			throws ServerFault {
		UserService userService = (UserService) ServerSideServiceProvider.getProvider(context)
				.instance(IInCoreUser.class, domainUid);

		userService.passwordValidator.validate(newPassword);

		try {
			if (context.getOrigin().equals("keycloak")
					&& Boolean.TRUE.equals(userService.checkPassword(user, newPassword))) {
				throw new ServerFault("New password must not be the current one", ErrorCode.OLD_PASSWORD_SAME_AS_NEW);
			}
		} catch (IllegalArgumentException iae) {
			// Ignore if user current password use unsupported hash
		}

		// we support setting the user password as a hash, directly
		// this is used for external user importers
		if (HashFactory.algorithm(newPassword) != HashAlgorithm.UNKNOWN) {
			userService.setPassword(user.uid, newPassword, true);
		} else {
			userService.setPassword(user.uid, HashFactory.getDefault().create(newPassword), true);
		}

		new UserEventProducer(domainUid, VertxPlatform.eventBus()).passwordUpdated(user.uid);
		// ysnp cache invalidation
		MQ.getProducer(Topic.CORE_SESSIONS)
				.send(new JsonObject().put("latd", user.value.login + "@" + domainUid).put("operation", "pwchange"));
	}

}
