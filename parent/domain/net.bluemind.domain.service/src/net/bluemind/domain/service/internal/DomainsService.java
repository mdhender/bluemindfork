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
package net.bluemind.domain.service.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.PathParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.core.utils.UIDGenerator;
import net.bluemind.core.validator.Validator;
import net.bluemind.directory.api.BaseDirEntry;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.directory.service.DirEntryHandlers;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.domain.hook.IDomainHook;
import net.bluemind.domain.service.DefaultGroups;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.group.api.IGroup;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.resource.api.type.IResourceTypes;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class DomainsService implements IDomains {
	private static final Logger logger = LoggerFactory.getLogger(DomainsService.class);

	private DomainStoreService store;
	private BmContext context;
	private DomainValidator validator = new DomainValidator();
	private Sanitizer sanitizer;
	private Validator extValidator;

	private RBACManager rbacManager;

	private DomainsCache domainsCache;

	private static final List<IDomainHook> hooks = getHooks();

	protected static final Cache<String, ItemValue<Domain>> cache = CacheBuilder.newBuilder().recordStats()
			.expireAfterWrite(10, TimeUnit.MINUTES).build();

	public static class CacheRegistration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register(DomainsService.class, cache);
		}
	}

	public DomainsService(BmContext context, Container installationContainer) {
		this.context = context;
		this.store = new DomainStoreService(context.getDataSource(), context.getSecurityContext(),
				installationContainer);
		this.sanitizer = new Sanitizer(context);
		this.extValidator = new Validator(context);

		rbacManager = new RBACManager(context);
		domainsCache = DomainsCache.get(context);
	}

	@Override
	public void create(String uid, Domain domain) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);
		ParametersValidator.notNullAndNotEmpty(uid);

		sanitizer.create(domain);

		if (!domain.name.equals(uid)) {
			throw new ServerFault("domain name should be equals to domain uid", ErrorCode.INVALID_PARAMETER);
		}
		validator.validate(store, domain);
		extValidator.create(domain);

		ItemValue<Domain> value = store.doOrFail(() -> {
			store.create(uid, domain.label, domain);

			// create domain container
			IContainers containers = context.provider().instance(IContainers.class);

			// create dir container
			containers.create(uid, ContainerDescriptor.create(uid, "directory of " + domain.name,
					context.getSecurityContext().getSubject(), "dir", uid, true));

			DirEntryHandlers.byKind(BaseDirEntry.Kind.DOMAIN).create(context, uid,
					DirEntry.create(null, uid, BaseDirEntry.Kind.DOMAIN, uid, domain.label, null, true, true, false));
			return store.get(uid, null);
		});

		for (IDomainHook hook : hooks) {
			hook.onCreated(context, value);
		}

		// BMHIDDENSYSADMIN
		User u = new User();
		u.login = "bmhiddensysadmin";
		u.password = UUID.randomUUID().toString();
		u.routing = Routing.none;
		u.hidden = true;
		u.system = true;
		VCard card = new VCard();
		card.identification.name = Name.create("System", null, null, null, null, null);
		u.contactInfos = card;

		logger.info("Create user bmhiddensysadmin for domain {}", uid);
		context.su().provider().instance(IUser.class, uid).create(u.login, u);

		logger.info("Create user group and admin group for domain {}", uid);
		IGroup groups = context.su().provider().instance(IGroup.class, uid);

		DefaultGroups.userGroup((group, roles) -> {
			String userGroupUid = UIDGenerator.uid();

			groups.create(userGroupUid, group);
			groups.setRoles(userGroupUid, roles);
		});

		DefaultGroups.adminGroup((group, roles) -> {
			String adminGroupUid = UIDGenerator.uid();

			groups.create(adminGroupUid, group);
			groups.setRoles(adminGroupUid, roles);
		});

		notify("domain.created", domain.name);
	}

	@Override
	public void update(String uid, Domain domain) throws ServerFault {
		rbacManager.forDomain(uid).check(BasicRoles.ROLE_ADMIN);

		ItemValue<Domain> currentDomain = store.get(uid, null);
		if (currentDomain == null) {
			throw new ServerFault("Domain " + uid + " doesnt exists", ErrorCode.NOT_FOUND);
		}

		ParametersValidator.notNullAndNotEmpty(uid);

		sanitizer.update(currentDomain.value, domain);

		validator.validate(store, domain);
		extValidator.update(currentDomain.value, domain);

		// domain.name can't change
		if (!domain.name.equals(currentDomain.value.name)) {
			throw new ServerFault("Domain name can't be changed", ErrorCode.INVALID_PARAMETER);
		}

		if (domain.global != currentDomain.value.global) {
			throw new ServerFault("Domain global flag can't be changed", ErrorCode.INVALID_PARAMETER);
		}

		if (!domain.aliases.equals(currentDomain.value.aliases)) {
			throw new ServerFault("Domain aliases should be modified via setAliases method",
					ErrorCode.INVALID_PARAMETER);
		}
		if (!domain.defaultAlias.equals(currentDomain.value.defaultAlias)) {
			throw new ServerFault("Domain default alias should be modified via setDefaultAliases method",
					ErrorCode.INVALID_PARAMETER);
		}

		ItemValue<Domain> value = store.doOrFail(() -> {
			store.update(uid, domain.label, domain);
			DirEntryHandlers.byKind(BaseDirEntry.Kind.DOMAIN).update(context, uid,
					DirEntry.create(null, uid, BaseDirEntry.Kind.DOMAIN, uid, domain.label, null, true, true, false));

			ItemValue<Domain> updated = store.get(uid, null);
			domainsCache.put(uid, updated);
			return updated;
		});

		for (IDomainHook hook : hooks) {
			hook.onUpdated(context, currentDomain, value);
		}

		notify("domain.updated", domain.name);
	}

	@Override
	public void delete(String uid) throws ServerFault {
		rbacManager.forDomain(uid).check(BasicRoles.ROLE_ADMIN);
		ParametersValidator.notNullAndNotEmpty(uid);

		ItemValue<Domain> domainItem = store.get(uid, null);
		if (domainItem == null) {
			throw new ServerFault("Domain " + uid + " doesnt exists", ErrorCode.NOT_FOUND);
		}

		IDirectory dir = context.provider().instance(IDirectory.class, uid);
		List<DirEntry> entries = dir.getEntries(uid);
		if (!entries.isEmpty()) {
			throw new ServerFault("Domain is not empty, use deleteDomainItems before call delete");
		}

		for (IDomainHook hook : hooks) {
			hook.onBeforeDelete(context, domainItem);
		}
		store.doOrFail(() -> {
			store.delete(uid);
			DirEntryHandlers.byKind(DirEntry.Kind.DOMAIN).delete(context, uid, uid);
			IContainers containers = context.provider().instance(IContainers.class);
			containers.delete(uid);
			domainsCache.invalidate(uid);
			return null;
		});
		for (IDomainHook hook : hooks) {
			hook.onDeleted(context, domainItem);
		}

		notify("domain.deleted", uid);
	}

	@Override
	public TaskRef deleteDomainItems(String uid) throws ServerFault {
		rbacManager.forDomain(uid).check(BasicRoles.ROLE_ADMIN);

		final ItemValue<Domain> domain = store.get(uid, null);
		if (domain == null) {
			throw new ServerFault("Domain " + uid + " doesnt exists", ErrorCode.NOT_FOUND);
		}

		ITasksManager tasksMananger = context.provider().instance(ITasksManager.class);

		return tasksMananger.run(new IServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) throws Exception {
				deepDelete(domain);
			}
		});

	}

	private void deepDelete(ItemValue<Domain> domain) throws ServerFault {
		try {
			logger.info("Deleting domain mail filters of domain {}", domain.uid);
			context.provider().instance(IMailboxes.class, domain.uid).setDomainFilter(new MailFilter());
		} catch (ServerFault sf) {
			logger.warn("Failed to delete sieve filters for domain {}", domain.uid);
		}

		IDirectory dir = context.provider().instance(IDirectory.class, domain.uid);

		List<DirEntry> entries = dir.getEntries(domain.uid);
		// sort entries to delete
		// - groups
		// - users
		// etc..
		Collections.sort(entries, new Comparator<DirEntry>() {

			@Override
			public int compare(DirEntry o1, DirEntry o2) {
				int r = kindAsInt(o1.kind) - kindAsInt(o2.kind);
				if (r == 0) {
					return o1.path.compareTo(o2.path);
				} else {
					return -r;
				}

			}

			private int kindAsInt(DirEntry.Kind kind) {
				switch (kind) {
				case USER:
					return 6;
				case GROUP:
					return 5;
				case RESOURCE:
					return 4;
				case MAILSHARE:
					return 3;
				case ORG_UNIT:
					return 2;
				case ADDRESSBOOK:
				case CALENDAR:
					return 1;
				case DOMAIN:
				default:
					return 0;
				}
			}
		});
		for (DirEntry entry : entries) {
			try {
				TaskRef tr = dir.delete(entry.path);
				TaskUtils.wait(context.provider(), tr);
			} catch (ServerFault e) {
				if (e.getCode() != ErrorCode.NOT_FOUND) {
					throw e;
				}
			}
		}

		IResourceTypes resourceTypes = context.provider().instance(IResourceTypes.class, domain.uid);
		resourceTypes.getTypes().forEach(res -> resourceTypes.delete(res.identifier));
	}

	@Override
	public ItemValue<Domain> get(@PathParam("uid") String uid) throws ServerFault {
		if (!uid.equals(context.getSecurityContext().getContainerUid())) {
			rbacManager.forDomain(uid).check(BasicRoles.ROLE_ADMIN);
		}
		ItemValue<Domain> d = domainsCache.getIfPresent(uid);

		if (d == null) {
			d = store.get(uid, null);
			if (d != null) {
				domainsCache.put(uid, d);
				d = ItemValue.create(d, d.value.copy());
			}
		} else {
			d = ItemValue.create(d, d.value.copy());
		}
		return d;
	}

	@Override
	public List<ItemValue<Domain>> all() throws ServerFault {
		if (RBACManager.forContext(context).can(BasicRoles.ROLE_MANAGE_DOMAIN)) {
			return store.all();
		} else {
			return Arrays.asList(store.get(context.getSecurityContext().getContainerUid(), null));
		}
	}

	@Override
	public TaskRef setAliases(String uid, Set<String> aliases) throws ServerFault {
		rbacManager.forDomain(uid).check(BasicRoles.ROLE_ADMIN);
		ParametersValidator.notNullAndNotEmpty(uid);

		final ItemValue<Domain> domainItem = get(uid);
		if (domainItem == null) {
			throw new ServerFault("Domain " + uid + " doesnt exists", ErrorCode.NOT_FOUND);
		}

		final Set<String> previousAliases = domainItem.value.aliases;
		domainItem.value.aliases = aliases;
		validator.validate(store, domainItem.value);
		ITasksManager tasksMananger = context.provider().instance(ITasksManager.class);

		return tasksMananger.run(new IServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) throws Exception {
				doSetAliases(domainItem, previousAliases, monitor);
				DomainsService.this.notify("domain.updated", uid);
			}
		});
	}

	protected void doSetAliases(ItemValue<Domain> domainItem, Set<String> previousAliases, IServerTaskMonitor monitor)
			throws ServerFault {

		monitor.begin(1 + hooks.size(), "update domain " + domainItem.uid + " aliases");

		boolean update = !previousAliases.equals(domainItem.value.aliases);
		if (update) {
			store.update(domainItem.uid, domainItem.value.label, domainItem.value);
			domainsCache.invalidate(domainItem.uid);
		}
		monitor.progress(1, "domain updated");

		if (!update) {
			return;
		}
		int i = 0;
		for (IDomainHook hook : hooks) {
			hook.onAliasesUpdated(context, domainItem, previousAliases);
			monitor.progress(1, "calling hook (" + (i + 1) + " on " + hooks.size() + ")");
			i++;
		}
	}

	@Override
	public void setDefaultAlias(String uid, String defaultAlias) {
		rbacManager.forDomain(uid).check(BasicRoles.ROLE_ADMIN);
		ParametersValidator.notNullAndNotEmpty(uid);

		final ItemValue<Domain> currentDomainItem = get(uid);
		if (currentDomainItem == null) {
			throw new ServerFault("Domain " + uid + " doesnt exists", ErrorCode.NOT_FOUND);
		}
		final Domain domain = currentDomainItem.value.copy();

		domain.defaultAlias = defaultAlias;
		validator.validate(store, domain);

		ItemValue<Domain> updatedDomainItem = store.doOrFail(() -> {
			store.update(uid, domain.label, domain);
			DirEntryHandlers.byKind(BaseDirEntry.Kind.DOMAIN).update(context, uid,
					DirEntry.create(null, uid, BaseDirEntry.Kind.DOMAIN, uid, domain.label, null, true, true, false));

			ItemValue<Domain> updated = store.get(uid, null);
			domainsCache.put(uid, updated);
			return updated;
		});

		for (IDomainHook hook : hooks) {
			hook.onUpdated(context, currentDomainItem, updatedDomainItem);
		}
		notify("domain.updated", domain.name);
	}

	@Override
	public ItemValue<Domain> findByNameOrAliases(String name) throws ServerFault {
		rbacManager.checkNotAnoynmous();

		ItemValue<Domain> domain = domainsCache.getDomainOrAlias(name);
		if (domain == null) {
			domain = store.findByNameOrAliases(name);

			if (domain != null) {
				domainsCache.put(domain.uid, domain);
			} else {
				return null;
			}
		}

		if (!domain.uid.equals(context.getSecurityContext().getContainerUid())) {
			rbacManager.forDomain(domain.uid).check(BasicRoles.ROLE_ADMIN);
		}
		return domain;
	}

	private static List<IDomainHook> getHooks() {
		RunnableExtensionLoader<IDomainHook> loader = new RunnableExtensionLoader<IDomainHook>();
		List<IDomainHook> hooks = loader.loadExtensions("net.bluemind.domain", "domainHook", "hook", "class");
		return hooks;
	}

	private void notify(String op, String domainName) {
		if (MQ.getProducer(Topic.SYSTEM_NOTIFICATIONS) == null) {
			logger.warn("Skipping notification");
			return;
		}
		try {
			OOPMessage msg = MQ.newMessage();
			msg.putStringProperty("operation", op);
			msg.putStringProperty("domain", domainName);
			MQ.getProducer(Topic.SYSTEM_NOTIFICATIONS).send(msg);
			logger.info("Notification for " + op + " sent.");
		} catch (Exception e) {
			logger.warn("Failed notification: " + e.getMessage(), e);
		}
	}

	@Override
	public void setRoles(String uid, Set<String> roles) throws ServerFault {
		rbacManager.forDomain(uid).check(BasicRoles.ROLE_ADMIN);

		store.setRoles(uid, roles);
	}

	@Override
	public Set<String> getRoles(String uid) throws ServerFault {
		rbacManager.forDomain(uid).check(BasicRoles.ROLE_ADMIN);
		return store.getRoles(uid);
	}
}
