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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.backup.continuous.dto.ContainerMetadata;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.hooks.IAclHook;
import net.bluemind.core.container.hooks.IContainersHook;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.rest.BmContext;

public class ContainersContinuousHook implements IContainersHook, IAclHook {

	private final ContainerMetadataContinuousBackup metadataBackup = new ContainerMetadataContinuousBackup();

	public class ContainerMetadataContinuousBackup implements ContinuousContenairization<ContainerMetadata> {
		@Override
		public String type() {
			return "containers_meta";
		}
	}

	@Override
	public void onContainerCreated(BmContext ctx, ContainerDescriptor cd) throws ServerFault {
		ContainerMetadata cm = ContainerMetadata.forAclsAndSettings(cd, Collections.emptyList(),
				Collections.emptyMap());
		metadataBackup.save(cd.domainUid, cd.owner, cd.uid, cm, true);
	}

	@Override
	public void onContainerUpdated(BmContext ctx, ContainerDescriptor prev, ContainerDescriptor cur)
			throws ServerFault {
		// ok
	}

	@Override
	public void onContainerDeleted(BmContext ctx, ContainerDescriptor cd) throws ServerFault {
		ContainerMetadata cm = ContainerMetadata.forAclsAndSettings(cd, Collections.emptyList(),
				Collections.emptyMap());
		metadataBackup.delete(cd.domainUid, cd.owner, cd.uid, cm);
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
		IContainerManagement cmApi = ctx.su().provider().instance(IContainerManagement.class, cd.uid);
		Map<String, String> settings = cmApi.getSettings();
		List<AccessControlEntry> curAcls = cmApi.getAccessControlList();
		ContainerMetadata cm = ContainerMetadata.forAclsAndSettings(cd, curAcls, settings);
		metadataBackup.save(cd.domainUid, cd.owner, cd.uid, cm, false);
	}

	@Override
	public void onAclChanged(BmContext ctx, ContainerDescriptor cd, List<AccessControlEntry> previous,
			List<AccessControlEntry> current) {
		IContainerManagement cmApi = ctx.su().provider().instance(IContainerManagement.class, cd.uid);
		Map<String, String> settings = cmApi.getSettings();
		ContainerMetadata cm = ContainerMetadata.forAclsAndSettings(cd, current, settings);
		metadataBackup.save(cd.domainUid, cd.owner, cd.uid, cm, false);
	}

}
