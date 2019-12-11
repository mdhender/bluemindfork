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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.deferredaction.service.internal;

import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.deferredaction.api.IDeferredActionContainerUids;
import net.bluemind.deferredaction.service.IDeferredActionMgmt;
import net.bluemind.user.api.User;
import net.bluemind.user.hook.DefaultUserHook;

public class DeferredActionUserHook extends DefaultUserHook {

	@Override
	public void onUserCreated(BmContext context, String domainUid, ItemValue<User> created) throws ServerFault {
		String owner = created.uid;
		String containerUid = IDeferredActionContainerUids.uidForUser(owner);

		ContainerDescriptor descriptor = ContainerDescriptor.create(containerUid, containerUid, owner,
				IDeferredActionContainerUids.TYPE, domainUid, true);
		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IContainers.class).create(containerUid,
				descriptor);
	}

	@Override
	public void beforeDelete(BmContext context, String domainUid, String uid, User previous) throws ServerFault {
		String containerUid = IDeferredActionContainerUids.uidForUser(uid);
		IContainers containerService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainers.class);

		if (containerService.getIfPresent(containerUid) != null) {
			try {
				IDeferredActionMgmt service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
						.instance(IDeferredActionMgmt.class, containerUid);
				service.prepareContainerDelete();
				containerService.delete(containerUid);
			} catch (Exception e) {
				LoggerFactory.getLogger(DeferredActionUserHook.class).warn("Cannot delete container {}", containerUid,
						e);
			}
		}
	}
}
