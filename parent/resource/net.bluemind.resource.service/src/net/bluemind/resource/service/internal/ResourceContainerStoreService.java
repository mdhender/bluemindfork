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
package net.bluemind.resource.service.internal;

import java.util.List;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirectoryContainerType;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.directory.service.DirEntryHandler;
import net.bluemind.directory.service.DirValueStoreService;
import net.bluemind.domain.api.Domain;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.resource.persistance.ResourceStore;

public class ResourceContainerStoreService extends DirValueStoreService<ResourceDescriptor> {

	public static class ResourceDirEntryAdapter implements DirEntryAdapter<ResourceDescriptor> {

		@Override
		public DirEntry asDirEntry(String domainUid, String uid, ResourceDescriptor rd) {
			return DirEntry.create(rd.orgUnitUid, domainUid + "/resources/" + uid, DirEntry.Kind.RESOURCE, uid,
					rd.label, rd.defaultEmailAddress(), rd.hidden, rd.system, rd.archived, rd.dataLocation);

		}
	}

	private ResourceStore resourceStore;

	public ResourceContainerStoreService(BmContext context, ItemValue<Domain> domain, Container container) {
		super(context, context.getDataSource(), context.getSecurityContext(), domain, container,
				DirectoryContainerType.TYPE, DirEntry.Kind.RESOURCE,
				new ResourceStore(context.getDataSource(), container), new ResourceDirEntryAdapter(),
				new ResourceVCardAdapter(), new ResourceMailboxAdapter());
		this.resourceStore = new ResourceStore(context.getDataSource(), container);
	}

	@Override
	protected byte[] getDefaultImage() {
		return DirEntryHandler.EMPTY_PNG;
	}

	@Override
	protected void decorate(Item item, ItemValue<DirEntryAndValue<ResourceDescriptor>> value) throws ServerFault {
		super.decorate(item, value);
		if (value.value.mailbox != null && value.value.value != null) {
			value.value.value.emails = value.value.mailbox.emails;
			value.value.value.orgUnitUid = value.value.entry.orgUnitUid;
		}
	}

	public List<String> findByType(String typeUid) {
		return doOrFail(() -> {
			return resourceStore.findByType(typeUid);
		});
	}

}
