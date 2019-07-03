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
package net.bluemind.core.container.service.internal;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.hooks.IAclHook;
import net.bluemind.core.container.hooks.IContainersHook;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ContainerModifiableDescriptor;
import net.bluemind.core.container.model.ItemDescriptor;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistance.AclStore;
import net.bluemind.core.container.persistance.ContainerPersonalSettingsStore;
import net.bluemind.core.container.persistance.ContainerSettingsStore;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.persistance.DataSourceRouter;
import net.bluemind.core.container.persistance.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.core.validator.Validator;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.user.persistance.UserSubscriptionStore;

public class ContainerManagement implements IContainerManagement {

	private AclStore aclStore;
	private SecurityContext securityContext;
	private Container container;
	private ContainerStore containerStore;
	private ItemStore itemStore;
	private ContainerPersonalSettingsStore containerPersonalSettingsStore;
	private ContainerSettingsStore containerSettingsStore;
	private BmContext context;
	private static final List<IAclHook> hooks = loadHooks();
	private static final List<IContainersHook> cHooks = loadContainerHooks();
	private Sanitizer sanitizer;
	private Validator validator;
	private RBACManager rbacManager;
	private UserSubscriptionStore userSubscriptionStore;
	private AccessControlEntryValidator aceValidator;

	private static final Logger logger = LoggerFactory.getLogger(ContainerManagement.class);

	private static List<IAclHook> loadHooks() {
		RunnableExtensionLoader<IAclHook> rel = new RunnableExtensionLoader<IAclHook>();
		List<IAclHook> aclHooks = rel.loadExtensions("net.bluemind.core.container.hooks", "aclhook", "acl_hook",
				"impl");
		return aclHooks;
	}

	private static List<IContainersHook> loadContainerHooks() {
		RunnableExtensionLoader<IContainersHook> rel = new RunnableExtensionLoader<IContainersHook>();
		List<IContainersHook> hooks = rel.loadExtensions("net.bluemind.core.container.hooks", "container", "hook",
				"impl");
		return hooks;
	}

	public ContainerManagement(BmContext context, Container container) throws ServerFault {
		this.container = container;
		this.context = context;
		securityContext = context.getSecurityContext();
		DataSource ds = DataSourceRouter.get(context, container.uid);

		itemStore = new ItemStore(ds, container, securityContext);
		containerStore = new ContainerStore(context, ds, securityContext);
		containerPersonalSettingsStore = new ContainerPersonalSettingsStore(ds, context.getSecurityContext(),
				container);
		containerSettingsStore = new ContainerSettingsStore(ds, container);
		aclStore = new AclStore(context, ds);
		sanitizer = new Sanitizer(context);
		validator = new Validator(context);

		rbacManager = new RBACManager(context).forContainer(container);

		try {
			Container domainContainer = new ContainerStore(null, context.getDataSource(), securityContext)
					.get(context.getSecurityContext().getContainerUid());
			userSubscriptionStore = new UserSubscriptionStore(securityContext, context.getDataSource(),
					domainContainer);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		aceValidator = new AccessControlEntryValidator(container.domainUid);
	}

	@Override
	public void setAccessControlList(List<AccessControlEntry> entries) throws ServerFault {

		if (!(container.owner.equals(securityContext.getSubject()) || rbacManager.can(Verb.Manage.name()))) {
			throw new ServerFault("container " + container.uid + " is not manageable", ErrorCode.PERMISSION_DENIED);
		}

		// validate mailboxacl, public sharing is forbidden
		aceValidator.validate(container, entries);

		List<AccessControlEntry> previous = aclStore.retrieveAndStore(container, entries);

		ContainerDescriptor descriptor = ContainerDescriptor.create(container.uid, container.name, container.owner,
				container.type, container.domainUid, false);

		for (IAclHook hook : hooks) {
			try {
				hook.onAclChanged(context, descriptor, Collections.unmodifiableList(previous),

						Collections.unmodifiableList(entries));
			} catch (Exception e) {
				logger.error("error executing hook on setACL (container {}@{})", container.uid, container.domainUid, e);
			}
		}

		eventProducer().changed(container.type, container.uid);
	}

	@Override
	public List<AccessControlEntry> getAccessControlList() throws ServerFault {
		rbacManager.check(Verb.Manage.name());
		try {
			return aclStore.get(container);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public ContainerDescriptor getDescriptor() throws ServerFault {
		rbacManager.check(Verb.Manage.name(), Verb.Read.name());

		return new Containers(context).asDescriptorForUser(container, context.getSecurityContext(),
				context.getSecurityContext().getSubject());
	}

	@Override
	public void update(ContainerModifiableDescriptor descriptor) throws ServerFault {
		rbacManager.check(Verb.Manage.name());

		ContainerDescriptor prev = getDescriptor();
		sanitizer.update(prev, descriptor);

		validator.update(prev, descriptor);
		try {
			containerStore.updateName(container.uid, descriptor.name);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		ContainerDescriptor cur = getDescriptor();
		for (IContainersHook ch : cHooks) {
			ch.onContainerUpdated(context, prev, cur);
		}

		eventProducer().changed(prev.type, container.uid);

	}

	@Override
	public List<String> subscribers() throws ServerFault {
		if (!securityContext.isDomainAdmin(container.domainUid)) {
			throw new ServerFault("only admin can call this method", ErrorCode.FORBIDDEN);
		}

		try {
			return new ContainerStore(null, context.getDataSource(), securityContext).listSubscriptions(container);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public List<ItemDescriptor> getAllItems() throws ServerFault {
		rbacManager.check(Verb.Read.name());

		try {
			return ItemDescriptor.get(itemStore.all());
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public List<ItemDescriptor> getFilteredItems(ItemFlagFilter filter) throws ServerFault {
		rbacManager.check(Verb.Read.name());

		try {
			return ItemDescriptor.get(itemStore.filtered(filter));
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public List<ItemDescriptor> getItems(List<String> uids) throws ServerFault {
		rbacManager.check(Verb.Read.name());

		try {
			return ItemDescriptor.get(itemStore.getMultiple(uids));
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void setPersonalSettings(Map<String, String> settings) throws ServerFault {
		containerPersonalSettingsStore.set(settings);
	}

	@Override
	public void setSettings(Map<String, String> settings) throws ServerFault {
		rbacManager.check(Verb.Manage.name());

		try {
			containerSettingsStore.setSettings(settings);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		ContainerDescriptor descriptor = ContainerDescriptor.create(container.uid, container.name, container.owner,
				container.type, container.domainUid, container.defaultContainer);
		for (IContainersHook hook : cHooks) {
			hook.onContainerSettingsChanged(context, descriptor);
		}

	}

	@Override
	public Map<String, String> getSettings() throws ServerFault {
		rbacManager.check(Verb.Read.name());

		try {
			return containerSettingsStore.getSettings();
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

	}

	private ContainersEventProducer eventProducer() {
		return new ContainersEventProducer(context.getSecurityContext(), VertxPlatform.eventBus());
	}

	@Override
	public void allowOfflineSync(String subject) throws ServerFault {
		new RBACManager(context).forDomain(container.domainUid).forEntry(subject)
				.check(BasicRoles.ROLE_MANAGE_USER_SUBSCRIPTIONS, BasicRoles.ROLE_SELF);

		try {
			if (!userSubscriptionStore.isSubscribed(subject, container)) {
				throw new ServerFault("No subscription for container " + container.uid);
			}
			userSubscriptionStore.allowSynchronization(subject, container, true);

			ContainerDescriptor descriptor = ContainerDescriptor.create(container.uid, container.name, container.owner,
					container.type, container.domainUid, false);
			for (IContainersHook hook : cHooks) {
				hook.onContainerOfflineSyncStatusChanged(context, descriptor, subject);
			}
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

	}

	@Override
	public void disallowOfflineSync(String subject) throws ServerFault {
		new RBACManager(context).forDomain(container.domainUid).forEntry(subject)
				.check(BasicRoles.ROLE_MANAGE_USER_SUBSCRIPTIONS, BasicRoles.ROLE_SELF);

		try {
			if (!userSubscriptionStore.isSubscribed(subject, container)) {
				throw new ServerFault("No subscription for container " + container.uid);
			}
			userSubscriptionStore.allowSynchronization(subject, container, false);

			ContainerDescriptor descriptor = ContainerDescriptor.create(container.uid, container.name, container.owner,
					container.type, container.domainUid, false);
			for (IContainersHook hook : cHooks) {
				hook.onContainerOfflineSyncStatusChanged(context, descriptor, subject);
			}
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public Count getItemCount() {
		rbacManager.check(Verb.Manage.name(), Verb.Read.name());
		try {
			return Count.of(itemStore.getItemCount());
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

}
