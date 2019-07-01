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
package net.bluemind.tag.hooks;

import java.util.Arrays;
import java.util.List;

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
import net.bluemind.tag.api.ITagUids;
import net.bluemind.tag.api.ITags;
import net.bluemind.user.api.User;
import net.bluemind.user.hook.DefaultUserHook;

/**
 * Create/Delete tags container on user creation/suppression
 *
 */
public class UserTagHook extends DefaultUserHook {

	private static Logger logger = LoggerFactory.getLogger(UserTagHook.class);

	@Override
	public void onUserCreated(BmContext context, String domainUid, ItemValue<User> created) {
		if (!created.value.system) {

			try {
				IContainers containers = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
						.instance(IContainers.class);

				ContainerDescriptor descriptor = new ContainerDescriptor();
				String containerUid = getTagsContainerUid(created);
				descriptor.uid = containerUid;
				descriptor.name = "tags of user " + created.displayName;
				descriptor.type = ITagUids.TYPE;
				descriptor.owner = created.uid;
				descriptor.domainUid = domainUid;
				containers.create(containerUid, descriptor);

				IContainerManagement cm = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
						.instance(IContainerManagement.class, containerUid);

				cm.setAccessControlList(Arrays.asList(AccessControlEntry.create(created.uid, Verb.All)));
			} catch (ServerFault e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	@Override
	public void onUserDeleted(BmContext context, String domainUid, ItemValue<User> previous) {
		if (!previous.value.system) {
			try {

				String containerUid = getTagsContainerUid(previous);

				ITags tagsService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITags.class,
						containerUid);
				List<String> tags = tagsService.allUids();
				tags.forEach(tag -> {
					tagsService.delete(tag);
				});

				IContainers cm = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
						.instance(IContainers.class);

				cm.delete(containerUid);
			} catch (ServerFault e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	private String getTagsContainerUid(ItemValue<User> user) {
		return ITagUids.defaultUserTags(user.uid);
	}

}