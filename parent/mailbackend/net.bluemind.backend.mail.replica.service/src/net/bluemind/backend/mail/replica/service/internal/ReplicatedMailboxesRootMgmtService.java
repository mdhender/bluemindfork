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
package net.bluemind.backend.mail.replica.service.internal;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.IReplicatedMailboxesRootMgmt;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.MailboxReplicaRootUpdate;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.api.utils.Subtree;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordStore;
import net.bluemind.backend.mail.replica.persistence.MailboxReplicaStore;
import net.bluemind.backend.mail.replica.service.internal.hooks.DeletedDataMementos;
import net.bluemind.backend.mail.replica.utils.SubtreeContainer;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.api.IFlatHierarchyUids;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ContainerModifiableDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class ReplicatedMailboxesRootMgmtService implements IReplicatedMailboxesRootMgmt {

	private static final Logger logger = LoggerFactory.getLogger(ReplicatedMailboxesRootMgmtService.class);
	private BmContext context;
	private CyrusPartition partition;
	private static ReadWriteLock lock = new ReentrantReadWriteLock();

	public ReplicatedMailboxesRootMgmtService(BmContext context, CyrusPartition partition) {
		this.context = context;
		this.partition = partition;
	}

	@Override
	public void create(MailboxReplicaRootDescriptor root) {
		String domainUid = partition.domainUid;
		Subtree sub = DeletedDataMementos.cachedSubtree(context, domainUid, root);
		if (sub != null) {
			logger.warn("******** We DON'T want to create a new root {} for the deleted mailbox of {}", root.fullName(),
					sub.ownerUid);
			return;
		}
		sub = SubtreeContainer.mailSubtreeUid(context, domainUid, root);
		String containerUid = sub.subtreeUid();
		String ownerUid = sub.ownerUid;
		IContainers contApi = context.provider().instance(IContainers.class);
		if (getRootContainer(containerUid, contApi) == null) {
			createRootContainer(root, domainUid, containerUid, ownerUid, contApi);
		} else if (logger.isDebugEnabled()) {
			logger.debug("Container {} exists for {}", containerUid, root.fullName());
		}
	}

	private void createRootContainer(MailboxReplicaRootDescriptor root, String domainUid, String containerUid,
			String ownerUid, IContainers contApi) {
		try {
			lock.writeLock().lock();
			if (getRootContainer(containerUid, contApi) == null) {
				logger.info("Create missing root {}", containerUid);
				ContainerDescriptor toCreate = ContainerDescriptor.create(containerUid, subtreeName(root), ownerUid,
						IMailReplicaUids.REPLICATED_MBOXES, domainUid, true);
				toCreate.domainUid = domainUid;

				contApi.create(toCreate.uid, toCreate);
				EmitReplicationEvents.mailboxRootCreated(root);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	private ContainerDescriptor getRootContainer(String containerUid, IContainers contApi) {
		try {
			lock.readLock().lock();
			return contApi.getIfPresent(containerUid);
		} finally {
			lock.readLock().unlock();
		}
	}

	private String subtreeName(MailboxReplicaRootDescriptor root) {
		return root.ns.name() + "/" + root.name.replace('^', '.');
	}

	private String owner(String namespace, String mailboxName, String domainUid, String defaultOwner) {
		String owner = defaultOwner;
		if (Namespace.valueOf(namespace) == Namespace.users) {
			IUser userApi = context.provider().instance(IUser.class, domainUid);
			ItemValue<User> found = userApi.byLogin(mailboxName);
			if (found != null) {
				owner = found.uid;
			} else {
				logger.warn("Login '{}' not found in domain '{}'", mailboxName, domainUid);
			}
		} else {
			IMailshare shareApi = context.provider().instance(IMailshare.class, domainUid);
			String toSearch = mailboxName.replace('^', '.');
			Optional<ItemValue<Mailshare>> found = shareApi.allComplete().stream()
					.filter(it -> it.value.name.equals(toSearch)).findFirst();
			if (found.isPresent()) {
				owner = found.get().uid;
			} else {
				logger.warn("Mailshare {} not found", toSearch);
				IResources resApi = context.provider().instance(IResources.class, domainUid);
				ResourceDescriptor asRes = resApi.get(toSearch);
				if (asRes != null) {
					owner = toSearch;
					logger.info("owner matched a resource {}", toSearch);
				} else {
					logger.warn("Resource {} not found", toSearch);
				}
			}
		}
		return owner;
	}

	@Override
	public void update(MailboxReplicaRootUpdate rename) {
		String domainUid = partition.domainUid;
		Subtree sub = SubtreeContainer.mailSubtreeUid(context, domainUid, rename.from);
		IContainers contApi = context.provider().instance(IContainers.class);
		ContainerModifiableDescriptor cm = new ContainerModifiableDescriptor();
		cm.defaultContainer = true;
		cm.name = subtreeName(rename.to);
		logger.info("Renaming subtree from {} to {}", subtreeName(rename.from), cm.name);
		contApi.update(sub.subtreeUid(), cm);
	}

	@Override
	public void delete(String namespace, String mailboxName) {
		String owner = owner(namespace, mailboxName, partition.domainUid, null);
		if (owner != null) {
			List<DataSource> allDs = new LinkedList<>();
			allDs.add(context.getDataSource());
			allDs.add(DataSourceRouter.get(context, IFlatHierarchyUids.getIdentifier(owner, partition.domainUid)));
			for (DataSource ds : allDs) {
				logger.info("Deleting replicated stuff for ns: {}, box: {} on ds {}", namespace, mailboxName, ds);
				try {
					reset((lookup -> {
						try {
							return lookup.store.findByTypeAndOwner(lookup.containerType, owner);
						} catch (SQLException e) {
							throw new ServerFault(e);
						}
					}), ds);
				} catch (Exception e) {
					logger.error("Reset error: {}", e.getMessage(), e);
				}
			}
			CacheRegistry.get().invalidateAll();
		} else {
			logger.warn("Owner ns: {}, mbox: {} not found.", namespace, mailboxName);
		}
	}

	private void reset(Function<Lookup, List<Container>> lookup, DataSource ds) {
		logger.info("Reset of replicated mail data for partition {}", partition);
		IServiceProvider prov = ServerSideServiceProvider.getProvider(context);
		IContainers containersApi = prov.instance(IContainers.class);
		ContainerStore contStore = new ContainerStore(context, ds, context.getSecurityContext());
		List<Container> recordsContainers = lookup.apply(new Lookup(IMailReplicaUids.MAILBOX_RECORDS, contStore));

		logger.info("Found {} mailbox_records containers", recordsContainers.size());

		for (Container cont : recordsContainers) {
			MailboxRecordStore store = new MailboxRecordStore(ds, cont);
			ContainerStoreService<MailboxRecord> storeService = new ContainerStoreService<>(ds,
					context.getSecurityContext(), cont, "mail", store);
			logger.info("Clearing {}", cont.uid);
			storeService.deleteAll();
			containersApi.delete(cont.uid);
		}

		List<Container> mboxReplicaContainers = lookup.apply(new Lookup(IMailReplicaUids.REPLICATED_MBOXES, contStore));
		logger.info("Found {} subtrees to clear (type: {})", mboxReplicaContainers.size(),
				IMailReplicaUids.REPLICATED_MBOXES);
		for (Container cont : mboxReplicaContainers) {
			MailboxReplicaStore store = new MailboxReplicaStore(ds, cont, partition.domainUid);
			ContainerStoreService<MailboxReplica> storeService = new ContainerStoreService<>(ds,
					context.getSecurityContext(), cont, "mbox_replica", store);
			logger.info("Clearing {}", cont.uid);
			storeService.deleteAll();
			containersApi.delete(cont.uid);
		}

		logger.info("Cleanup of {} complete.", partition);
	}

	private static class Lookup {
		private final String containerType;
		private final ContainerStore store;

		public Lookup(String containerType, ContainerStore store) {
			this.containerType = containerType;
			this.store = store;
		}

	}

}
