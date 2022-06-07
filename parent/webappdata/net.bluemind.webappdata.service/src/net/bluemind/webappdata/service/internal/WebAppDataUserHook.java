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
package net.bluemind.webappdata.service.internal;

import java.util.Arrays;

import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.user.api.IUserSubscription;
import net.bluemind.user.api.User;
import net.bluemind.user.hook.DefaultUserHook;
import net.bluemind.webappdata.api.IWebAppData;
import net.bluemind.webappdata.api.IWebAppDataUids;

public class WebAppDataUserHook extends DefaultUserHook {

	@Override
	public void onUserCreated(BmContext context, String domainUid, ItemValue<User> created) throws ServerFault {
		String owner = created.uid;
		String containerUid = IWebAppDataUids.containerUid(owner);

		ContainerDescriptor descriptor = ContainerDescriptor.create(containerUid, containerUid, owner,
				IWebAppDataUids.TYPE, domainUid, true);
		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IContainers.class).create(containerUid,
				descriptor);

		IUserSubscription userSubService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUserSubscription.class, domainUid);
		userSubService.subscribe(owner, Arrays.asList(ContainerSubscription.create(containerUid, true)));
	}

	@Override
	public void beforeDelete(BmContext context, String domainUid, String uid, User previous) throws ServerFault {
		String containerUid = IWebAppDataUids.containerUid(uid);
		IContainers containerService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainers.class);

		if (containerService.getIfPresent(containerUid) != null) {
			IWebAppData webAppDataService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IWebAppData.class, containerUid);
			try {
				webAppDataService.deleteAll();
				containerService.delete(containerUid);
			} catch (Exception e) {
				LoggerFactory.getLogger(WebAppDataUserHook.class).warn("Cannot delete container {}", containerUid, e);
			}
		}
	}
}
