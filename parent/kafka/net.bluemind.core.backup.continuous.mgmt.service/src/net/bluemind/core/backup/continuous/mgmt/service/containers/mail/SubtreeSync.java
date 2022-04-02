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

import java.util.HashSet;
import java.util.Set;

import net.bluemind.backend.cyrus.internal.CyrusUniqueIds;
import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.backup.continuous.mgmt.service.impl.ContainerSync;
import net.bluemind.core.backup.continuous.mgmt.service.impl.ContainerUidsMapping;
import net.bluemind.core.backup.continuous.mgmt.service.impl.LoggedContainerDeltaSync;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.service.common.DefaultFolder;

public class SubtreeSync<O> extends LoggedContainerDeltaSync<O, MailboxReplica> {

	private Set<String> defaultFolders;

	public SubtreeSync(BmContext ctx, ContainerDescriptor container, ItemValue<ContainerHierarchyNode> node,
			ItemValue<DirEntryAndValue<O>> owner, ItemValue<Domain> domain) {
		super(ctx, container, node, owner, domain);
		defaultFolders = new HashSet<>();
		if (owner.value.mailbox.type.sharedNs) {
			defaultFolders.add(owner.value.mailbox.name);
			for (String child : DefaultFolder.MAILSHARE_FOLDERS_NAME) {
				defaultFolders.add(owner.value.mailbox.name + "/" + child);
			}
		} else {
			defaultFolders.add("INBOX");
			for (String child : DefaultFolder.USER_FOLDERS_NAME) {
				defaultFolders.add(child);
			}
		}
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

	@Override
	protected ItemValue<MailboxReplica> remap(IServerTaskMonitor contMon, ItemValue<MailboxReplica> item) {
		if (defaultFolders.contains(item.value.fullName)) {
			String freshUniqueId = CyrusUniqueIds
					.forMailbox(domain.uid, ItemValue.create(owner, owner.value.mailbox), item.value.name).toString();
			ContainerUidsMapping.map(contMon, IMailReplicaUids.mboxRecords(item.uid),
					IMailReplicaUids.mboxRecords(freshUniqueId));
			item.uid = freshUniqueId;
		}
		return item;
	}

}
