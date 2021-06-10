/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.core.backup.continuous.events;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.backup.continuous.DefaultBackupStore;
import net.bluemind.core.backup.continuous.dto.ContainerMetadata;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.hooks.IAclHook;
import net.bluemind.core.container.hooks.IContainersHook;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.rest.BmContext;

public class ContainersContinuousHook implements IContainersHook, IAclHook {

	private static final Logger logger = LoggerFactory.getLogger(ContainersContinuousHook.class);

	@Override
	public void onContainerCreated(BmContext ctx, ContainerDescriptor cd) throws ServerFault {
		// ensure we have a queue for empty containers
//		DefaultBackupStore.get().forContainer(cd).delete(0L);
//		Bubble.owner(cd);
	}

	@Override
	public void onContainerUpdated(BmContext ctx, ContainerDescriptor prev, ContainerDescriptor cur)
			throws ServerFault {
		Bubble.owner(cur);
	}

	@Override
	public void onContainerDeleted(BmContext ctx, ContainerDescriptor cd) throws ServerFault {
		Bubble.owner(cd);
	}

	@Override
	public void onContainerSubscriptionsChanged(BmContext ctx, ContainerDescriptor cd, List<String> subs,
			List<String> unsubs) throws ServerFault {
		Bubble.owner(cd);
	}

	@Override
	public void onContainerOfflineSyncStatusChanged(BmContext ctx, ContainerDescriptor cd, String subject) {
		Bubble.owner(cd);
	}

	@Override
	public void onContainerSettingsChanged(BmContext ctx, ContainerDescriptor cd) throws ServerFault {
		ContainerDescriptor metaDesc = ContainerDescriptor.create(cd.owner + "_containers_meta",
				"containers meta of " + cd.owner, cd.owner, "containers_meta", cd.domainUid, false);
		ContainerMetadata cm = new ContainerMetadata();
		cm.containerUid = cd.uid;
		cm.type = ContainerMetadata.MetaType.Setting;
		IContainerManagement mgmApi = ctx.provider().instance(IContainerManagement.class, cd.uid);
		cm.settings = mgmApi.getSettings();
		ItemValue<ContainerMetadata> metaItem = ItemValue.create(cd.uid + "_settings", cm);
		metaItem.internalId = metaItem.uid.hashCode();
		DefaultBackupStore.get().<ContainerMetadata>forContainer(metaDesc).store(metaItem);
		logger.info("Saved settings for {}", cd.uid);
		Bubble.owner(cd);
	}

	@Override
	public void onAclChanged(BmContext context, ContainerDescriptor cd, List<AccessControlEntry> previous,
			List<AccessControlEntry> current) {
		ContainerDescriptor metaDesc = ContainerDescriptor.create(cd.owner + "_containers_meta",
				"containers meta of " + cd.owner, cd.owner, "containers_meta", cd.domainUid, false);
		ContainerMetadata cm = new ContainerMetadata();
		cm.containerUid = cd.uid;
		cm.type = ContainerMetadata.MetaType.Acl;
		cm.acls = current;
		ItemValue<ContainerMetadata> metaItem = ItemValue.create(cd.uid + "_acls", cm);
		metaItem.internalId = metaItem.uid.hashCode();
		DefaultBackupStore.get().<ContainerMetadata>forContainer(metaDesc).store(metaItem);
		logger.info("Saved acls for {}", cd.uid);
		Bubble.owner(cd);
	}

}
