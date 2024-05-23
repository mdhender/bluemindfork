/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.eas.http.tests.helpers;

import static org.junit.Assert.assertNotNull;

import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.config.Token;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.device.api.Device;
import net.bluemind.device.api.IDevice;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class CorePartnershipHelper {

	private CorePartnershipHelper() {

	}

	public static void addPartnership(String domainUid, String login, String devId) throws Exception {
		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IAuthentication authService = provider.instance(IAuthentication.class);
		authService.login("admin0@global.virt", Token.admin0(), "eas-verticle-tests");

		IUser userService = provider.instance(IUser.class, domainUid);
		ItemValue<User> user = userService.byLogin(login);

		IDevice deviceService = provider.instance(IDevice.class, user.uid);
		ItemValue<Device> device = deviceService.byIdentifier(devId);
		assertNotNull(device);
		deviceService.setPartnership(device.uid);
	}

}
