/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.user.service.internal;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Functions;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.api.ContainerSubscriptionDescriptor;
import net.bluemind.core.container.api.IOwnerSubscriptions;
import net.bluemind.core.container.hooks.IContainersHook;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.i18n.labels.I18nLabels;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.user.api.IInternalUserSubscription;
import net.bluemind.user.persistence.UserSubscriptionStore;

public class UserSubscriptionService implements IInternalUserSubscription {

	private static final Logger logger = LoggerFactory.getLogger(UserSubscriptionService.class);

	private BmContext context;
	private String domainUid;
	private UserSubscriptionStore store;
	private static final List<IContainersHook> cHooks = loadContainerHooks();

	private static List<IContainersHook> loadContainerHooks() {
		RunnableExtensionLoader<IContainersHook> rel = new RunnableExtensionLoader<>();
		return rel.loadExtensions("net.bluemind.core.container.hooks", "container", "hook", "impl");
	}

	public UserSubscriptionService(BmContext context, Container container) {
		this.context = context;
		domainUid = container.uid;
		store = new UserSubscriptionStore(context.getSecurityContext(), context.getDataSource(), container);
	}

	@Override
	public List<ContainerSubscriptionDescriptor> listSubscriptions(String subject, String type) throws ServerFault {
		RBACManager rbac = new RBACManager(context).forDomain(domainUid).forEntry(subject);
		rbac.check(BasicRoles.ROLE_MANAGE_USER_SUBSCRIPTIONS, BasicRoles.ROLE_SELF);
		String lang = context.getSecurityContext().getLang();
		List<ContainerSubscriptionDescriptor> allSubs = Collections.emptyList();

		try {
			allSubs = context.provider().instance(IOwnerSubscriptions.class, domainUid, subject).list().stream()
					.map(iv -> ContainerSubscriptionDescriptor.copyOf(iv.value)
							.withName(I18nLabels.getInstance().translate(lang, iv.value.name)))
					.filter(sub -> type == null || sub.containerType.equals(type)).collect(Collectors.toList());
			List<String> uniqueOwners = allSubs.stream().map(csm -> csm.owner).distinct().collect(Collectors.toList());
			IDirectory dirApi = context.su().provider().instance(IDirectory.class, domainUid);
			Map<String, DirEntry> resolvedOwners = dirApi.getMultiple(uniqueOwners).stream().filter(Objects::nonNull)
					.collect(Collectors.toMap(iv -> iv.uid, iv -> iv.value));
			allSubs.forEach(sub -> {
				DirEntry de = resolvedOwners.get(sub.owner);
				if (de != null) {
					sub.ownerDisplayName = de.displayName;
					sub.ownerDirEntryPath = de.path;
				}
			});
		} catch (ServerFault sf) {
			if (sf.getCode() == ErrorCode.NOT_FOUND) {
				logger.warn("Something is missing to list subsciptions of {}: {}", subject, sf.getMessage());
			} else {
				throw sf;
			}
		}
		return allSubs;

	}

	@Override
	public void subscribe(String subject, List<ContainerSubscription> subscriptions) throws ServerFault {
		for (ContainerSubscription sub : subscriptions) {
			Container container = subscriptionToContainer(sub);
			ContainerDescriptor descriptor = containerAndSubscriptionToDescriptor(container, sub);
			subscribe(container, descriptor, subject, false);
		}
	}

	@Override
	public void subscribe(String subject, ContainerDescriptor descriptor) throws ServerFault {
		Container container = Container.create(descriptor.uid, descriptor.type, descriptor.name, descriptor.owner,
				descriptor.domainUid, descriptor.defaultContainer);
		subscribe(container, descriptor, subject, true);
	}

	private void subscribe(Container container, ContainerDescriptor descriptor, String subject, boolean disableHook) {
		new RBACManager(context).forDomain(container.domainUid).forEntry(subject)
				.check(BasicRoles.ROLE_MANAGE_USER_SUBSCRIPTIONS, BasicRoles.ROLE_SELF);
		try {
			if (!store.isSubscribed(subject, container)) {
				createSubscription(container, descriptor, subject, disableHook);
			} else {
				updateSubscription(container, descriptor, subject, disableHook);
			}
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	private void createSubscription(Container container, ContainerDescriptor descriptor, String subject,
			boolean disableHook) throws SQLException {
		store.subscribe(subject, container);
		store.allowSynchronization(subject, container, descriptor.offlineSync);
		store.updateAutomount(subject, container, descriptor.automount);
		if (!disableHook) {
			cHooks.forEach(hook -> hook.onContainerSubscriptionsChanged(context, descriptor, Arrays.asList(subject),
					Collections.emptyList()));
		}
	}

	private void updateSubscription(Container container, ContainerDescriptor descriptor, String subject,
			boolean disableHook) throws SQLException {
		if (store.isSyncAllowed(subject, container) != descriptor.offlineSync) {
			store.allowSynchronization(subject, container, descriptor.offlineSync);
			if (!disableHook) {
				cHooks.forEach(hook -> hook.onContainerOfflineSyncStatusChanged(context, descriptor, subject));
			}
		}
	}

	@Override
	public void unsubscribe(String subject, List<String> containers) throws ServerFault {
		for (String uid : containers) {
			DataSource ds = DataSourceRouter.get(context, uid);
			ContainerStore containerStore = new ContainerStore(context, ds, context.getSecurityContext());
			Container container;
			try {
				container = containerStore.get(uid);
			} catch (SQLException e) {
				throw ServerFault.sqlFault(e);
			}

			if (container == null) {
				// unsub anyway
				try {
					store.unsubscribe(subject, uid);
				} catch (SQLException e) {
					throw ServerFault.sqlFault(e);
				}
				continue;
			}

			ContainerDescriptor descriptor = ContainerDescriptor.create(container.uid, container.name, container.owner,
					container.type, container.domainUid, container.defaultContainer);
			unsubscribe(container, descriptor, subject, false);
		}
	}

	@Override
	public void unsubscribe(String subject, ContainerDescriptor descriptor) throws ServerFault {
		Container container = Container.create(descriptor.uid, descriptor.type, descriptor.name, descriptor.owner,
				descriptor.domainUid, descriptor.defaultContainer);
		unsubscribe(container, descriptor, subject, true);
	}

	private void unsubscribe(Container container, ContainerDescriptor descriptor, String subject, boolean disableHook) {
		if (container.defaultContainer && container.owner.equals(subject)) {
			logger.info("do not unsub default container id {}, type {}, name {}", container.id, container.type,
					container.name);
			return;
		}
		new RBACManager(context).forDomain(container.domainUid).forEntry(subject)
				.check(BasicRoles.ROLE_MANAGE_USER_SUBSCRIPTIONS, BasicRoles.ROLE_SELF);
		try {
			store.unsubscribe(subject, container.uid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		if (!disableHook) {
			for (IContainersHook hook : cHooks) {
				hook.onContainerSubscriptionsChanged(context, descriptor, Collections.<String>emptyList(),
						Arrays.asList(subject));
			}
		}
	}

	@Override
	public List<String> subscribers(String containerUid) {
		try {
			return store.subscribers(containerUid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void updateAutomount(String subject, List<ContainerSubscription> subscriptions) {
		Map<ContainerSubscription, Container> subscriptionContainers = subscriptions.stream()
				.collect(Collectors.toMap(Functions.identity(), subscription -> {
					try {
						Container container = subscriptionToContainer(subscription);
						new RBACManager(context).forDomain(container.domainUid).forEntry(subject)
								.check(BasicRoles.ROLE_MANAGE_USER_SUBSCRIPTIONS, BasicRoles.ROLE_SELF);
						if (!store.isSubscribed(subject, container)) {
							throw new ServerFault("No subscription for container " + container.uid);
						}
						return container;
					} catch (SQLException e) {
						throw ServerFault.sqlFault(e);
					}
				}));

		subscriptionContainers.forEach((subscription, container) -> {
			try {
				store.updateAutomount(subject, container, subscription.automount);
				for (IContainersHook hook : cHooks) {
					ContainerDescriptor descriptor = containerAndSubscriptionToDescriptor(container, subscription);
					hook.onContainerAutomountChanged(context, descriptor, subject);
				}
			} catch (SQLException e) {
				throw ServerFault.sqlFault(e);
			}
		});
	}

	private Container subscriptionToContainer(ContainerSubscription subscription) {
		Container container;

		DataSource ds = DataSourceRouter.get(context, subscription.containerUid);
		ContainerStore containerStore = new ContainerStore(context, ds, context.getSecurityContext());

		try {
			container = containerStore.get(subscription.containerUid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		if (container == null) {
			throw ServerFault.notFound("Failed to subscribe. Container not found : " + subscription.containerUid);
		}

		return container;
	}

	private ContainerDescriptor containerAndSubscriptionToDescriptor(Container container, ContainerSubscription subscription) {
		ContainerDescriptor descriptor = ContainerDescriptor.create(container.uid, container.name, container.owner,
				container.type, container.domainUid, container.defaultContainer);
		descriptor.offlineSync = subscription.offlineSync;
		descriptor.automount = subscription.automount;
		return descriptor;
	}

}
