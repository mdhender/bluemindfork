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

import java.util.List;

import com.google.common.collect.Lists;

import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.ICalendarViewUids;
import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.core.backup.continuous.events.ContinuousContenairization;
import net.bluemind.core.backup.continuous.mgmt.api.BackupSyncOptions;
import net.bluemind.core.container.api.IRestoreDirEntryWithMailboxSupport;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.deferredaction.api.IDeferredActionContainerUids;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.exchange.mapi.api.MapiFolderContainer;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.resource.api.type.IResourceTypes;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;

public class ResourceSync extends DirEntryWithMailboxSync<ResourceDescriptor> {

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

	private static final List<String> TYPE_ORDER = Lists.newArrayList(//
			IMailboxAclUids.TYPE, //
			ICalendarUids.TYPE, //
			ICalendarViewUids.TYPE //
	);

	private static final List<String> SKIPPED_TYPES = Lists.newArrayList(//
			MapiFolderContainer.TYPE, //
			IDeferredActionContainerUids.TYPE//
	);

	public ResourceSync(BmContext ctx, BackupSyncOptions opts,
			IRestoreDirEntryWithMailboxSupport<ResourceDescriptor> getApi, DomainApis domainApis,
			DomainKafkaState state) {
		super(ctx, opts, getApi, domainApis, state);
	}

	@Override
	protected List<String> containerTypeOrder() {
		return TYPE_ORDER;
	}

	@Override
	protected List<String> containerTypeToSkip() {
		return SKIPPED_TYPES;
	}

	@Override
	protected void preSync(IBackupStoreFactory target, String domain,
			ItemValue<DirEntryAndValue<ResourceDescriptor>> entryAndValue) {
		ResourceDescriptor res = entryAndValue.value.value;

		IResourceTypes rtApi = ctx.provider().instance(IResourceTypes.class, domain);
		ResourceTypeDescriptor type = rtApi.get(res.typeIdentifier);
		// set the owner to the resource using it so it ends up in the partition before
		// the resource
		new ResTypeHook(target).save(domain, entryAndValue.uid, res.typeIdentifier, type, true);
	}

}
