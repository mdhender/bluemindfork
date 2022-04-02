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
package net.bluemind.core.backup.continuous.mgmt.service.containers.addressbook;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.backup.continuous.mgmt.service.impl.ContainerSync;
import net.bluemind.core.backup.continuous.mgmt.service.impl.LoggedContainerDeltaSync;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.domain.api.Domain;

public class BookSync<O> extends LoggedContainerDeltaSync<O, VCard> {

	public BookSync(BmContext ctx, ContainerDescriptor container, ItemValue<ContainerHierarchyNode> node,
			ItemValue<DirEntryAndValue<O>> owner, ItemValue<Domain> domain) {
		super(ctx, container, node, owner, domain);
	}

	public static class SyncFactory implements ContainerSync.Factory {

		@Override
		public <U> ContainerSync forNode(BmContext ctx, ItemValue<ContainerHierarchyNode> node,
				ItemValue<DirEntryAndValue<U>> owner, ItemValue<Domain> domain) {
			IContainers contApi = ctx.provider().instance(IContainers.class);
			ContainerDescriptor container = contApi.get(node.value.containerUid);
			return new BookSync<U>(ctx, container, node, owner, domain);
		}

	}

	@Override
	protected ReadApis<VCard> initReadApi() {
		IAddressBook calApi = ctx.provider().instance(IAddressBook.class, node.value.containerUid);
		return new ReadApis<>(calApi, calApi);
	}

}
