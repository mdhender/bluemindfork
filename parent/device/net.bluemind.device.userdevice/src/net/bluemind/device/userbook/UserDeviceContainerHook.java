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
package net.bluemind.device.userbook;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.device.api.IDevice;
import net.bluemind.device.api.IDeviceUids;
import net.bluemind.user.api.User;
import net.bluemind.user.hook.DefaultUserHook;

public class UserDeviceContainerHook extends DefaultUserHook {

	private static final Logger logger = LoggerFactory.getLogger(UserDeviceContainerHook.class);

	@Override
	public void onUserCreated(BmContext context, String domainUid, ItemValue<User> created) {
		if (!created.value.system) {
			ContainerDescriptor descriptor = ContainerDescriptor.create(IDeviceUids.defaultUserDevices(created.uid),
					IDeviceUids.TYPE, created.uid, IDeviceUids.TYPE, domainUid, true);
			try {
				IContainers service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
						.instance(IContainers.class);
				service.create(descriptor.uid, descriptor);

				IContainerManagement cm = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
						.instance(IContainerManagement.class, descriptor.uid);
				cm.setAccessControlList(Arrays.asList(AccessControlEntry.create(created.uid, Verb.All)));

			} catch (ServerFault e) {
				logger.error("error during device container creation ", e);
			}
		}
	}

	@Override
	public void onUserDeleted(BmContext context, String domainUid, ItemValue<User> deleted) throws ServerFault {
		if (!deleted.value.system) {
			IDevice deviceService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(
					IDevice.class,

					deleted.uid);
			deviceService.deleteAll();

			IContainers service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IContainers.class);
			service.delete(IDeviceUids.defaultUserDevices(deleted.uid));

		}
	}

}
