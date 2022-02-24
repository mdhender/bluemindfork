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
import java.util.Map;

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
		// ok
	}

	@Override
	public void onContainerUpdated(BmContext ctx, ContainerDescriptor prev, ContainerDescriptor cur)
			throws ServerFault {
		// ok
	}

	@Override
	public void onContainerDeleted(BmContext ctx, ContainerDescriptor cd) throws ServerFault {
		// ok
	}

	@Override
	public void onContainerSubscriptionsChanged(BmContext ctx, ContainerDescriptor cd, List<String> subs,
			List<String> unsubs) throws ServerFault {
		// ok
	}

	@Override
	public void onContainerOfflineSyncStatusChanged(BmContext ctx, ContainerDescriptor cd, String subject) {
		// ok
	}

	@Override
	public void onContainerSettingsChanged(BmContext ctx, ContainerDescriptor cd) throws ServerFault {
		ContainerDescriptor metaDesc = metadataDescriptor(cd, cd.owner);
		Map<String, String> settings = ctx.provider().instance(IContainerManagement.class, cd.uid).getSettings();
		ContainerMetadata cm = ContainerMetadata.forSettings(cd.uid, settings);
		ItemValue<ContainerMetadata> metaItem = metadataItem(cd, cm);
		DefaultBackupStore.store().<ContainerMetadata>forContainer(metaDesc).store(metaItem);
		logger.info("Saved settings for {}", cd.uid);
	}

	@Override
	public void onAclChanged(BmContext context, ContainerDescriptor cd, List<AccessControlEntry> previous,
			List<AccessControlEntry> current) {
		ContainerDescriptor metaDesc = metadataDescriptor(cd, cd.owner);
		ContainerMetadata cm = ContainerMetadata.forAcls(cd.uid, current);
		ItemValue<ContainerMetadata> metaItem = metadataItem(cd, cm);
		DefaultBackupStore.store().<ContainerMetadata>forContainer(metaDesc).store(metaItem);
		logger.info("Saved acls for {}", cd.uid);
	}

	private ContainerDescriptor metadataDescriptor(ContainerDescriptor descriptor, String owner) {
		return ContainerDescriptor.create(descriptor.owner + "_containers_meta",
				"containers meta of " + descriptor.owner, owner, "containers_meta", descriptor.domainUid, false);
	}

	private ItemValue<ContainerMetadata> metadataItem(ContainerDescriptor descriptor, ContainerMetadata metadata) {
		ItemValue<ContainerMetadata> metadataItem = ItemValue.create(descriptor.uid, metadata);
		metadataItem.internalId = metadataItem.uid.hashCode();
		return metadataItem;
	}

}
