/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.replica.service;

import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.api.utils.Subtree;
import net.bluemind.backend.mail.replica.persistence.MailboxReplicaStore;
import net.bluemind.backend.mail.replica.service.internal.MailboxReplicaFlagProvider;
import net.bluemind.backend.mail.replica.utils.SubtreeContainer;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.container.service.internal.ContainerStoreService.IWeightSeedProvider;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;

public abstract class AbstractReplicatedMailboxesServiceFactory<T>
		implements ServerSideServiceProvider.IServerSideServiceFactory<T> {

	protected static final Logger logger = LoggerFactory.getLogger(AbstractReplicatedMailboxesServiceFactory.class);
	private static final MailboxReplicaFlagProvider flagProvider = new MailboxReplicaFlagProvider();
	private static final MailboxReplicaWeightSeedProvider weightSeedProvider = new MailboxReplicaWeightSeedProvider();

	protected AbstractReplicatedMailboxesServiceFactory() {
	}

	protected T getService(BmContext context, CyrusPartition partition, MailboxReplicaRootDescriptor mailboxRoot) {
		if (logger.isDebugEnabled()) {
			logger.debug("Replicated mailboxes for {}", mailboxRoot.fullName());
		}
		Subtree sub = SubtreeContainer.mailSubtreeUid(context, partition.domainUid, mailboxRoot);
		String uid = sub.subtreeUid();
		DataSource ds = DataSourceRouter.get(context, uid);
		String datalocation = DataSourceRouter.location(context, uid);
		ContainerStore containerStore = new ContainerStore(context, ds, context.getSecurityContext());

		try {
			Container foldersContainer = containerStore.get(uid);
			if (foldersContainer == null) {
				throw ServerFault.notFound("Container " + uid + " is missing.");
			}
			MailboxReplicaStore mboxReplicaStore = new MailboxReplicaStore(ds, foldersContainer, partition.domainUid);
			mailboxRoot.dataLocation = Optional.ofNullable(datalocation).orElse(partition.serverUid);
			ContainerStoreService<MailboxReplica> storeService = new ContainerStoreService<>(ds,
					context.getSecurityContext(), foldersContainer, mboxReplicaStore, flagProvider, weightSeedProvider,
					seed -> seed);
			storeService = disableChangelogIfSystem(context, foldersContainer, storeService);
			return create(mailboxRoot, foldersContainer, context, mboxReplicaStore, storeService, containerStore);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	private <W> ContainerStoreService<W> disableChangelogIfSystem(BmContext context, Container cont,
			ContainerStoreService<W> storeService) {
		try {
			DirEntry owner = context.su().provider().instance(IDirectory.class, cont.domainUid)
					.findByEntryUid(cont.owner);
			if (owner.system) {
				storeService = storeService.withoutChangelog();
			}
		} catch (Exception e) {
			// some junit might fail on missing on domains_bluemind-noid missing
		}
		return storeService;
	}

	protected abstract T create(MailboxReplicaRootDescriptor root, Container cont, BmContext context,
			MailboxReplicaStore mboxReplicaStore, ContainerStoreService<MailboxReplica> storeService,
			ContainerStore containerStore);

	@Override
	public T instance(BmContext context, String... params) {
		if (params == null || params.length < 2) {
			throw new ServerFault("wrong number of instance parameters");
		}
		CyrusPartition partition = CyrusPartition.forName(params[0]);
		String root = params[1];
		if (logger.isDebugEnabled()) {
			logger.debug("params[0]: {}, params[1]: {}", params[0], params[1]);
		}
		MailboxReplicaRootDescriptor rootDesc = null;
		if (root.startsWith("user.")) {
			rootDesc = MailboxReplicaRootDescriptor.create(Namespace.users, root.substring("user.".length()));
		} else {
			rootDesc = MailboxReplicaRootDescriptor.create(Namespace.shared, root);
		}
		return getService(context, partition, rootDesc);
	}

	private static class MailboxReplicaWeightSeedProvider implements IWeightSeedProvider<MailboxReplica> {

		private static final Set<String> PRIORITY_FOLDERS = Sets.newHashSet("Sent", "Drafts", "Trash", "Outbox");

		@Override
		public long weightSeed(MailboxReplica mailbox) {
			if (mailbox.fullName.equals("INBOX")) {
				return 3l;
			} else if (PRIORITY_FOLDERS.contains(mailbox.fullName)) {
				return 2l;
			} else if (mailbox.parentUid == null) {
				return 1l;
			} else {
				return 0l;
			}
		};
	}

}
