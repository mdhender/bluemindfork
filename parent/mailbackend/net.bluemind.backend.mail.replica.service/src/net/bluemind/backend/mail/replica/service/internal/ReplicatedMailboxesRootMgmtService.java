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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import net.bluemind.backend.mail.replica.persistence.ConversationStore;
import net.bluemind.backend.mail.replica.persistence.InternalConversation;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordStore;
import net.bluemind.backend.mail.replica.persistence.MailboxReplicaStore;
import net.bluemind.backend.mail.replica.service.internal.hooks.DeletedDataMementos;
import net.bluemind.backend.mail.replica.service.internal.repair.RecordsResyncTask;
import net.bluemind.backend.mail.replica.utils.SubtreeContainer;
import net.bluemind.core.api.fault.ServerFault;
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
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
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
		String conversationSubtreeUid = IMailReplicaUids.conversationSubtreeUid(domainUid, ownerUid);
		ReplicatedMailboxesContainers contStatus = getContainers(containerUid, conversationSubtreeUid, contApi);
		if (contStatus.isContainerMissing()) {
			logger.info("Create missing root {}", containerUid);
			createContainers(root, domainUid, containerUid, conversationSubtreeUid, ownerUid, contApi);
		} else {
			ContainerModifiableDescriptor cmd = new ContainerModifiableDescriptor();
			cmd.defaultContainer = true;
			cmd.name = subtreeName(root);
			contApi.update(containerUid, cmd);
			logger.info("Name of {} updated to {}", containerUid, cmd.name);
		}
	}

	private void createContainers(MailboxReplicaRootDescriptor root, String domainUid, String rootContainerUid,
			String conversationContainerUid, String ownerUid, IContainers contApi) {
		try {
			lock.writeLock().lock();
			ReplicatedMailboxesContainers containers = getContainers(rootContainerUid, conversationContainerUid,
					contApi);
			if (!containers.rootContainer.isPresent()) {
				ContainerDescriptor toCreate = ContainerDescriptor.create(rootContainerUid, subtreeName(root), ownerUid,
						IMailReplicaUids.REPLICATED_MBOXES, domainUid, true);
				toCreate.domainUid = domainUid;

				contApi.create(toCreate.uid, toCreate);
			}
			if (!containers.conversationContainer.isPresent()) {
				createConversationContainer(conversationContainerUid, domainUid, root, ownerUid, contApi);
			}
		} finally {
			EmitReplicationEvents.mailboxRootCreated(root);
			lock.writeLock().unlock();
		}
	}

	private void createConversationContainer(String conversationContainerUid, String domainUid,
			MailboxReplicaRootDescriptor root, String ownerUid, IContainers containerService) {
		ContainerDescriptor conversationContainerDescriptor = ContainerDescriptor.create(conversationContainerUid,
				conversationSubtreeName(root), ownerUid, IMailReplicaUids.REPLICATED_CONVERSATIONS, domainUid, true);
		containerService.create(conversationContainerDescriptor.uid, conversationContainerDescriptor);
	}

	private ReplicatedMailboxesContainers getContainers(String rootUid, String conversationUid, IContainers contApi) {
		try {
			lock.readLock().lock();
			return ReplicatedMailboxesContainers.getContainers(rootUid, conversationUid, contApi);
		} finally {
			lock.readLock().unlock();
		}
	}

	private String subtreeName(MailboxReplicaRootDescriptor root) {
		return root.ns.name() + "/" + root.name.replace('^', '.');
	}

	private String conversationSubtreeName(MailboxReplicaRootDescriptor root) {
		return subtreeName(root) + "_conversations";
	}

	private String owner(String namespace, String mailboxName, String domainUid, String defaultOwner) {
		String owner = defaultOwner;
		String toSearch = mailboxName.replace('^', '.');
		if (Namespace.valueOf(namespace) == Namespace.users) {
			IUser userApi = context.provider().instance(IUser.class, domainUid);
			ItemValue<User> found = userApi.byLogin(toSearch);
			if (found != null) {
				owner = found.uid;
			} else {
				logger.warn("Login '{}' not found in domain '{}'", mailboxName, domainUid);
			}
		} else {
			IMailshare shareApi = context.provider().instance(IMailshare.class, domainUid);
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
		IContainers contApi = context.provider().instance(IContainers.class);
		ContainerModifiableDescriptor cm = new ContainerModifiableDescriptor();
		cm.defaultContainer = true;
		cm.name = subtreeName(rename.to);
		logger.info("Renaming subtree from {} to {}", subtreeName(rename.from), cm.name);
		contApi.update(rename.subtreeUid, cm);
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
		} else {
			logger.warn("Owner ns: {}, mbox: {} not found.", namespace, mailboxName);
		}
	}

	@Override
	public TaskRef resync(String mailboxUid) {

		IMailboxes mboxApi = context.provider().instance(IMailboxes.class, partition.domainUid);
		ItemValue<Mailbox> lookup = mboxApi.getComplete(mailboxUid);
		if (lookup == null) {
			throw ServerFault.notFound("'" + mailboxUid + "' missing in domain " + partition.domainUid);
		}
		RecordsResyncTask resync = new RecordsResyncTask(context, partition, lookup);
		return context.provider().instance(ITasksManager.class).run(resync);
	}

	private void reset(Function<Lookup, List<Container>> lookup, DataSource ds) {
		logger.info("Reset of replicated mail data for partition {}", partition);
		IServiceProvider prov = ServerSideServiceProvider.getProvider(context);
		IContainers containersApi = prov.instance(IContainers.class);
		ContainerStore contStore = new ContainerStore(context, ds, context.getSecurityContext());

		List<Container> recordsContainers = lookup.apply(new Lookup(IMailReplicaUids.MAILBOX_RECORDS, contStore));
		logger.info("Found {} mailbox_records containers", recordsContainers.size());
		Set<String> cacheCleanups = new HashSet<>();

		for (Container cont : recordsContainers) {
			cacheCleanups.add(IMailReplicaUids.getUniqueId(cont.uid));
			MailboxRecordStore store = new MailboxRecordStore(ds, cont);
			ContainerStoreService<MailboxRecord> storeService = new ContainerStoreService<>(ds,
					context.getSecurityContext(), cont, store);
			logger.info("remove mailbox_records container {}", cont.uid);
			storeService.prepareContainerDelete();
			containersApi.delete(cont.uid);
		}

		List<Container> mboxReplicaContainers = lookup.apply(new Lookup(IMailReplicaUids.REPLICATED_MBOXES, contStore));
		logger.info("Found {} subtrees to clear (type: {})", mboxReplicaContainers.size(),
				IMailReplicaUids.REPLICATED_MBOXES);
		for (Container cont : mboxReplicaContainers) {
			MailboxReplicaStore store = new MailboxReplicaStore(ds, cont, partition.domainUid);
			ContainerStoreService<MailboxReplica> storeService = new ContainerStoreService<>(ds,
					context.getSecurityContext(), cont, store);
			logger.info("remove mailbox_replica container {}", cont.uid);
			storeService.prepareContainerDelete();
			containersApi.delete(cont.uid);
		}

		List<Container> conversationsContainers = lookup
				.apply(new Lookup(IMailReplicaUids.REPLICATED_CONVERSATIONS, contStore));
		logger.info("Found {} replicated_conversations containers", recordsContainers.size());
		for (Container container : conversationsContainers) {
			ConversationStore store = new ConversationStore(ds, container);
			ContainerStoreService<InternalConversation> storeService = new ContainerStoreService<>(ds,
					context.getSecurityContext(), container, store);
			logger.info("remove replicated_conversations container {}", container.uid);
			storeService.prepareContainerDelete();
			containersApi.delete(container.uid);
		}
		cacheCleanups.forEach(MboxReplicasCache::invalidate);
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

	private static class ReplicatedMailboxesContainers {
		final Optional<ContainerDescriptor> rootContainer;
		final Optional<ContainerDescriptor> conversationContainer;

		public ReplicatedMailboxesContainers(ContainerDescriptor rootContainer,
				ContainerDescriptor conversationContainer) {
			this.rootContainer = Optional.ofNullable(rootContainer);
			this.conversationContainer = Optional.ofNullable(conversationContainer);
		}

		boolean isContainerMissing() {
			return !rootContainer.isPresent() || !conversationContainer.isPresent();
		}

		static ReplicatedMailboxesContainers getContainers(String rootUid, String conversationUid,
				IContainers contApi) {
			return new ReplicatedMailboxesContainers(contApi.getIfPresent(rootUid),
					contApi.getIfPresent(conversationUid));
		}
	}

}
