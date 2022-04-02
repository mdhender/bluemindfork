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
package net.bluemind.core.backup.continuous.mgmt.service.containers.mail;

import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.core.backup.continuous.mgmt.service.impl.ContainerSync;
import net.bluemind.core.backup.continuous.mgmt.service.impl.LoggedContainerDeltaSync;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.domain.api.Domain;

public class RecordsSync<O> extends LoggedContainerDeltaSync<O, MailboxRecord> {

	public RecordsSync(BmContext ctx, ContainerDescriptor container, ItemValue<ContainerHierarchyNode> node,
			ItemValue<DirEntryAndValue<O>> owner, ItemValue<Domain> domain) {
		super(ctx, container, node, owner, domain);
	}

	public static class SyncFactory implements ContainerSync.Factory {

		@Override
		public <U> ContainerSync forNode(BmContext ctx, ItemValue<ContainerHierarchyNode> node,
				ItemValue<DirEntryAndValue<U>> owner, ItemValue<Domain> domain) {
			IContainers contApi = ctx.provider().instance(IContainers.class);
			ContainerDescriptor container = contApi.get(node.value.containerUid);
			return new RecordsSync<U>(ctx, container, node, owner, domain);
		}

	}

	@Override
	protected ReadApis<MailboxRecord> initReadApi() {
		IDbMailboxRecords recsApi = ctx.provider().instance(IDbMailboxRecords.class,
				IMailReplicaUids.getUniqueId(node.value.containerUid));
		return new ReadApis<>(recsApi, recsApi);
	}

}
