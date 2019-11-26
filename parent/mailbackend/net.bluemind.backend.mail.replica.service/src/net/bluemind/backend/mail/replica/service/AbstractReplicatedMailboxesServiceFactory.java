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

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.api.utils.Subtree;
import net.bluemind.backend.mail.replica.persistence.MailboxReplicaStore;
import net.bluemind.backend.mail.replica.service.internal.MailboxReplicaFlagProvider;
import net.bluemind.backend.mail.replica.service.internal.hooks.DeletedDataMementos;
import net.bluemind.backend.mail.replica.utils.SubtreeContainer;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;

public abstract class AbstractReplicatedMailboxesServiceFactory<T>
		implements ServerSideServiceProvider.IServerSideServiceFactory<T> {

	protected static final Logger logger = LoggerFactory.getLogger(AbstractReplicatedMailboxesServiceFactory.class);
	private static final MailboxReplicaFlagProvider flagProvider = new MailboxReplicaFlagProvider();

	protected AbstractReplicatedMailboxesServiceFactory() {
	}

	protected T getService(BmContext context, CyrusPartition partition, MailboxReplicaRootDescriptor mailboxRoot) {
		logger.debug("Replicated mailboxes for {}", mailboxRoot.fullName());
		Subtree sub = DeletedDataMementos.cachedSubtree(context, partition.domainUid, mailboxRoot);
		if (sub == null) {
			sub = SubtreeContainer.mailSubtreeUid(context, partition.domainUid, mailboxRoot);
		} else {
			return createNoopService(mailboxRoot, partition.domainUid);
		}
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
			mailboxRoot.dataLocation = datalocation;
			ContainerStoreService<MailboxReplica> storeService = new ContainerStoreService<>(ds,
					context.getSecurityContext(), foldersContainer, "mbox_replica", mboxReplicaStore, flagProvider,
					(v) -> 0L, seed -> seed);
			return create(mailboxRoot, foldersContainer, context, mboxReplicaStore, storeService, containerStore);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	protected abstract T create(MailboxReplicaRootDescriptor root, Container cont, BmContext context,
			MailboxReplicaStore mboxReplicaStore, ContainerStoreService<MailboxReplica> storeService,
			ContainerStore containerStore);

	protected T createNoopService(MailboxReplicaRootDescriptor mailboxRoot, String domainUid) {
		throw new UnsupportedOperationException(this.getClass().getName()
				+ " does not provide a noop implementation for " + mailboxRoot.name + "@" + domainUid);
	}

	@Override
	public T instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 2) {
			throw new ServerFault("wrong number of instance parameters");
		}
		CyrusPartition partition = CyrusPartition.forName(params[0]);
		String root = params[1];
		if (logger.isDebugEnabled()) {
			logger.debug("params[0]: " + params[0] + ", params[1]: " + params[1]);
		}
		MailboxReplicaRootDescriptor rootDesc = null;
		if (root.startsWith("user.")) {
			rootDesc = MailboxReplicaRootDescriptor.create(Namespace.users, root.substring("user.".length()));
		} else {
			rootDesc = MailboxReplicaRootDescriptor.create(Namespace.shared, root);
		}
		return getService(context, partition, rootDesc);
	}

}
