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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.backup.continuous.mgmt.service.impl;

import java.util.Set;

import com.google.common.collect.Sets;

import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.core.backup.continuous.events.ContinuousContenairization;
import net.bluemind.core.backup.continuous.mgmt.api.BackupSyncOptions;
import net.bluemind.core.container.api.IRestoreDirEntryWithMailboxSupport;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.resource.api.type.IResourceTypes;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;

public class ResourceSync extends DirEntryWithMailboxSync<ResourceDescriptor> {

	private Set<String> createdResourceTypes = Sets.newHashSet("default");

	private static class ResTypeHook implements ContinuousContenairization<ResourceTypeDescriptor> {

		private final IBackupStoreFactory target;

		public ResTypeHook(IBackupStoreFactory target) {
			this.target = target;
		}

		@Override
		public IBackupStoreFactory targetStore() {
			return target;
		}

		@Override
		public String type() {
			return "resourceTypes";
		}

	}

	public ResourceSync(BmContext ctx, BackupSyncOptions opts,
			IRestoreDirEntryWithMailboxSupport<ResourceDescriptor> getApi, DomainApis domainApis) {
		super(ctx, opts, getApi, domainApis);
	}

	@Override
	protected void preSync(IBackupStoreFactory target, String domain,
			ItemValue<DirEntryAndValue<ResourceDescriptor>> entryAndValue) {
		ResourceDescriptor res = entryAndValue.value.value;
		if (!createdResourceTypes.contains(res.typeIdentifier)) {
			IResourceTypes rtApi = ctx.provider().instance(IResourceTypes.class, domain);
			ResourceTypeDescriptor type = rtApi.get(res.typeIdentifier);
			new ResTypeHook(target).save(domain, domain, res.typeIdentifier, type, true);
		}
	}

}
