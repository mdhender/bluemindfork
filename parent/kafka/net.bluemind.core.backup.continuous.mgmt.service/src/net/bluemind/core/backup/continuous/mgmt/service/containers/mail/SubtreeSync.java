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

import java.util.List;

import org.slf4j.event.Level;

import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.backup.continuous.mgmt.service.impl.ContainerSync;
import net.bluemind.core.backup.continuous.mgmt.service.impl.LoggedContainerDeltaSync;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.domain.api.Domain;

public class SubtreeSync<O> extends LoggedContainerDeltaSync<O, MailboxReplica> {

	public SubtreeSync(BmContext ctx, ContainerDescriptor container, ItemValue<ContainerHierarchyNode> node,
			ItemValue<DirEntryAndValue<O>> owner, ItemValue<Domain> domain) {
		super(ctx, container, node, owner, domain);
	}

	public static class SyncFactory implements ContainerSync.Factory {

		@Override
		public <U> ContainerSync forNode(BmContext ctx, ItemValue<ContainerHierarchyNode> node,
				ItemValue<DirEntryAndValue<U>> owner, ItemValue<Domain> domain) {
			IContainers contApi = ctx.provider().instance(IContainers.class);
			ContainerDescriptor container = contApi.get(node.value.containerUid);
			return new SubtreeSync<U>(ctx, container, node, owner, domain);
		}

	}

	@Override
	protected ReadApis<MailboxReplica> initReadApi() {
		IDbByContainerReplicatedMailboxes subtreeApi = ctx.provider().instance(IDbByContainerReplicatedMailboxes.class,
				node.value.containerUid);
		return new ReadApis<>(subtreeApi, subtreeApi);
	}

	private static record SortNode(ItemVersion iv, String sortKey) {

	}

	@Override
	protected List<ItemVersion> sortItems(IServerTaskMonitor mon, List<ItemVersion> toSync) {
		return toSync.stream().map(iv -> {
			ItemValue<MailboxReplica> mr = crudApi.getCompleteById(iv.id);
			return new SortNode(iv, mr == null || mr.value == null ? null : mr.value.fullName);
		}).filter(sn -> {
			boolean validKey = sn.sortKey != null;
			if (!validKey) {
				mon.log("Failed to fetch mailbox replica for id {}", Level.WARN, sn.iv.id);
			}
			return validKey;
		}).sorted((sn1, sn2) -> sn1.sortKey().compareTo(sn2.sortKey())).map(sn -> sn.iv).toList();
	}

}
