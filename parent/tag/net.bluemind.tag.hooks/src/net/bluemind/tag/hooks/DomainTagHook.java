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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.hook.DomainHookAdapter;
import net.bluemind.tag.api.ITagUids;

public class DomainTagHook extends DomainHookAdapter {
	private static final Logger logger = LoggerFactory.getLogger(DomainTagHook.class);

	@Override
	public void onCreated(BmContext context, ItemValue<Domain> created) throws ServerFault {
		if (!created.value.global) {

			try {
				IContainers containers = context.su().provider().instance(IContainers.class);

				ContainerDescriptor descriptor = new ContainerDescriptor();
				String containerUid = getTagsContainerUid(created);
				descriptor.uid = containerUid;
				descriptor.name = "tags of domain " + created.displayName;
				descriptor.type = ITagUids.TYPE;
				descriptor.owner = created.uid;
				descriptor.domainUid = created.uid;
				containers.create(containerUid, descriptor);

				IContainerManagement cm = context.su().provider().instance(IContainerManagement.class, containerUid);

				// public
				cm.setAccessControlList(Arrays.asList(AccessControlEntry.create(created.uid, Verb.Read)));
			} catch (ServerFault e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	@Override
	public void onDeleted(BmContext context, ItemValue<Domain> previous) throws ServerFault {
	}

	@Override
	public void onBeforeDelete(BmContext context, ItemValue<Domain> domain) throws ServerFault {
		if (!domain.value.global) {
			try {
				String containerUid = getTagsContainerUid(domain);
				IContainers cm = context.su().provider().instance(IContainers.class);
				cm.delete(containerUid);
			} catch (ServerFault e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	private String getTagsContainerUid(ItemValue<Domain> domain) {
		return "tags_" + domain.uid;
	}

}
