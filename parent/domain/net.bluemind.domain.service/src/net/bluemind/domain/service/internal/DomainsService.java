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

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Suppliers;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import jakarta.ws.rs.PathParam;
import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.core.validator.Validator;
import net.bluemind.directory.api.BaseDirEntry;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.directory.service.DirEntryHandlers;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomainUids;
import net.bluemind.domain.api.IDomains;
import net.bluemind.domain.api.IInCoreDomains;
import net.bluemind.domain.hook.IDomainHook;
import net.bluemind.domain.service.DefaultGroups;
import net.bluemind.domain.service.DomainNotFoundException;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.group.api.IGroup;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.persistence.MailboxStore;
import net.bluemind.resource.api.type.IResourceTypes;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.system.api.CertData.CertificateDomainEngine;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;

public class DomainsService implements IInCoreDomains, IDomains {
	private static final Logger logger = LoggerFactory.getLogger(DomainsService.class);
	private static final String DOMAIN_UPDATED = "domain.updated";

	private DomainStoreService store;
	private BmContext context;
	private DomainValidator validator = new DomainValidator();
	private Sanitizer sanitizer;
	private Validator extValidator;

	private RBACManager rbacManager;

	private DomainsCache domainsCache;

	private static final Supplier<List<IDomainHook>> hooks = Suppliers.memoize(() -> getHooks());

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
	public void create(String uid, Domain domain) {
		ItemValue<Domain> item = ItemValue.create(uid, domain);
		create(item);
	}

	private void create(ItemValue<Domain> item) {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);
		String uid = item.uid;
		Domain domain = item.value;
		ParametersValidator.notNullAndNotEmpty(uid);

		sanitizer.create(domain);

		if (!domain.name.equals(uid)) {
			throw new ServerFault("domain name should be equals to domain uid", ErrorCode.INVALID_PARAMETER);
		}
		validator.validate(store, domain);
		extValidator.create(domain);

		ItemValue<Domain> value = store.doOrFail(() -> {
			store.create(item.item(), item.value);

			// create domain container
			IContainers containers = context.provider().instance(IContainers.class);

			// create dir container
			containers.create(uid, ContainerDescriptor.create(uid, "directory of " + domain.name,
					context.getSecurityContext().getSubject(), "dir", uid, true));

			DirEntryHandlers.byKind(BaseDirEntry.Kind.DOMAIN).create(context, uid,
					DirEntry.create(null, uid, BaseDirEntry.Kind.DOMAIN, uid, domain.label, null, true, true, false));
			return store.get(uid, null);
		});

		for (IDomainHook hook : hooks.get()) {
			hook.onCreated(context, value);
		}

		// init settings
		IDomainSettings settingsApi = context.provider().instance(IDomainSettings.class, uid);
		Map<String, String> settings = settingsApi.get();
		settings.put(DomainSettingsKeys.ssl_certif_engine.name(), CertificateDomainEngine.DISABLED.name());
		settingsApi.set(settings);

		logger.info("Create user group and admin group for domain {}", uid);
		IGroup groups = context.su().provider().instance(IGroup.class, uid);

		DefaultGroups.userGroup((group, roles) -> {
			String userGroupUid = IDomainUids.userGroup(uid);

			groups.create(userGroupUid, group);
			groups.setRoles(userGroupUid, roles);
		});

		DefaultGroups.adminGroup((group, roles) -> {
			String adminGroupUid = IDomainUids.adminGroup(uid);

			groups.create(adminGroupUid, group);
			groups.setRoles(adminGroupUid, roles);
		});

		notify("domain.created", domain.name);
	}

	@Override
	public void update(String uid, Domain domain) {
		ItemValue<Domain> item = ItemValue.create(uid, domain);
		update(item);
	}

	private void update(ItemValue<Domain> item) {
		String uid = item.uid;
		Domain domain = item.value;
		rbacManager.forDomain(uid).check(BasicRoles.ROLE_ADMIN);

		ItemValue<Domain> currentDomain = store.get(uid, null);
		if (currentDomain == null) {
			throw new DomainNotFoundException(uid);
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
			store.update(item.item(), domain.label, domain);
			DirEntryHandlers.byKind(BaseDirEntry.Kind.DOMAIN).update(context, uid,
					DirEntry.create(null, uid, BaseDirEntry.Kind.DOMAIN, uid, domain.label, null, true, true, false));

			ItemValue<Domain> updated = store.get(uid, null);
			domainsCache.put(uid, updated);
			return updated;
		});

		for (IDomainHook hook : hooks.get()) {
			hook.onUpdated(context, currentDomain, value);
		}

		notify(DOMAIN_UPDATED, domain.name);
	}

	@Override
	public void delete(String uid) {
		rbacManager.forDomain(uid).check(BasicRoles.ROLE_ADMIN);
		ParametersValidator.notNullAndNotEmpty(uid);

		ItemValue<Domain> domainItem = store.get(uid, null);
		if (domainItem == null) {
			throw new DomainNotFoundException(uid);
		}

		IDirectory dir = context.provider().instance(IDirectory.class, uid);
		List<DirEntry> entries = dir.getEntries(uid);
		if (!entries.isEmpty()) {
			throw new ServerFault("Domain is not empty, use deleteDomainItems before call delete");
		}

		for (IDomainHook hook : hooks.get()) {
			hook.onBeforeDelete(context, domainItem);
		}
		store.doOrFail(() -> {
			store.delete(uid);
			DirEntryHandlers.byKind(BaseDirEntry.Kind.DOMAIN).delete(context, uid, uid);
			IContainers containers = context.provider().instance(IContainers.class);
			containers.delete(uid);
			domainsCache.invalidate(uid);
			return null;
		});
		for (IDomainHook hook : hooks.get()) {
			hook.onDeleted(context, domainItem);
		}

		notify("domain.deleted", uid);
	}

	@Override
	public TaskRef deleteDomainItems(String uid) {
		rbacManager.forDomain(uid).check(BasicRoles.ROLE_ADMIN);

		final ItemValue<Domain> domain = store.get(uid, null);
		if (domain == null) {
			throw new DomainNotFoundException(uid);
		}

		ITasksManager tasksMananger = context.provider().instance(ITasksManager.class);
		return tasksMananger.run(new BlockingServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) throws Exception {
				deepDelete(domain);
				for (IDomainHook hook : hooks.get()) {
					hook.onDomainItemsDeleted(context, domain);
				}
			}
		});
	}

	private void deepDelete(ItemValue<Domain> domain) {
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

			private int kindAsInt(BaseDirEntry.Kind kind) {
				switch (kind) {
				case USER:
					return 6;
				case GROUP:
					return 5;
				case RESOURCE:
					return 4;
				case MAILSHARE:
					return 3;
				case ADDRESSBOOK:
				case CALENDAR:
					return 2;
				case ORG_UNIT:
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
	public ItemValue<Domain> get(@PathParam("uid") String uid) {
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
		return filter(d);
	}

	@Override
	public List<ItemValue<Domain>> all() {
		if (RBACManager.forContext(context).can(BasicRoles.ROLE_MANAGE_DOMAIN)) {
			return store.all();
		} else {
			return Arrays.asList(store.get(context.getSecurityContext().getContainerUid(), null)).stream()
					.map(this::filter).toList();
		}
	}

	@Override
	public TaskRef setAliases(String uid, Set<String> aliases) {
		rbacManager.forDomain(uid).check(BasicRoles.ROLE_ADMIN);
		ParametersValidator.notNullAndNotEmpty(uid);

		final ItemValue<Domain> domainItem = get(uid);
		if (domainItem == null) {
			throw new DomainNotFoundException(uid);
		}

		final Set<String> previousAliases = domainItem.value.aliases;
		domainItem.value.aliases = aliases;
		validator.validate(store, domainItem.value);
		ITasksManager tasksMananger = context.provider().instance(ITasksManager.class);

		return tasksMananger.run(new BlockingServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) throws Exception {
				doSetAliases(domainItem, previousAliases, monitor);
				// TODO change topic name
				DomainsService.this.notify(DOMAIN_UPDATED, uid);
			}
		});
	}

	protected void doSetAliases(ItemValue<Domain> domainItem, Set<String> previousAliases, IServerTaskMonitor monitor) {

		monitor.begin(1d + hooks.get().size(), "update domain " + domainItem.uid + " aliases");

		boolean update = !previousAliases.equals(domainItem.value.aliases);
		if (update) {
			SetView<String> deletedAliases = Sets.difference(previousAliases, domainItem.value.aliases);
			if (!deletedAliases.isEmpty()) {
				try {
					MailboxStore mailboxStore = new MailboxStore(context.getDataSource(),
							new ContainerStore(context, context.getDataSource(), SecurityContext.SYSTEM)
									.get(domainItem.uid));
					for (String deletedAlias : deletedAliases) {
						if (mailboxStore.isUsedAlias(deletedAlias)) {
							monitor.end(false, "Alias " + deletedAlias + " is still in use",
									"Alias " + deletedAlias + " is still in use");
							return;
						}
					}
				} catch (SQLException e) {
					monitor.end(false, "SQL error occured: " + e.getMessage(), "");
					return;
				}
			}

			store.update(domainItem.uid, domainItem.value.label, domainItem.value);
			domainsCache.invalidate(domainItem.uid);
		}
		monitor.progress(1, "domain updated");

		if (!update) {
			return;
		}
		int i = 0;
		for (IDomainHook hook : hooks.get()) {
			hook.onAliasesUpdated(context, domainItem, previousAliases);
			monitor.progress(1, "calling hook (" + (i + 1) + " on " + hooks.get().size() + ")");
			i++;
		}
	}

	@Override
	public void setDefaultAlias(String uid, String defaultAlias) {
		rbacManager.forDomain(uid).check(BasicRoles.ROLE_ADMIN);
		ParametersValidator.notNullAndNotEmpty(uid);

		final ItemValue<Domain> currentDomainItem = get(uid);
		if (currentDomainItem == null) {
			throw new DomainNotFoundException(uid);
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

		for (IDomainHook hook : hooks.get()) {
			hook.onUpdated(context, currentDomainItem, updatedDomainItem);
		}
		// TODO change topic name
		notify(DOMAIN_UPDATED, domain.name);
	}

	@Override
	public ItemValue<Domain> findByNameOrAliases(String name) {
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
		return filter(domain);
	}

	private static List<IDomainHook> getHooks() {
		RunnableExtensionLoader<IDomainHook> loader = new RunnableExtensionLoader<>();
		return loader.loadExtensions("net.bluemind.domain", "domainHook", "hook", "class");
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
			logger.info("Notification for {} sent.", op);
		} catch (Exception e) {
			logger.warn("Failed notification: {}", e.getMessage(), e);
		}
	}

	@Override
	public void setRoles(String uid, Set<String> roles) {
		rbacManager.forDomain(uid).check(BasicRoles.ROLE_ADMIN);

		store.setRoles(uid, roles);
	}

	@Override
	public Set<String> getRoles(String uid) {
		rbacManager.forDomain(uid).check(BasicRoles.ROLE_ADMIN);
		return store.getRoles(uid);
	}

	@Override
	public void restore(ItemValue<Domain> item, boolean isCreate) {
		ItemValue<Domain> maybeExist = store.get(item.internalId, null);
		if (maybeExist != null && !maybeExist.uid.equals(item.uid)) {
			logger.warn("A domain with itemId {} exists but is not the expected object (db: {}, restore {})",
					item.internalId, maybeExist, item);
			if (isCreate) {
				create(item.uid, item.value);
			} else {
				update(item.uid, item.value);
			}
		} else {
			if (isCreate) {
				create(item);
			} else {
				update(item);
			}
		}
	}

	private ItemValue<Domain> filter(ItemValue<Domain> domain) {
		if (domain == null || domain.value == null) {
			return domain;
		}
		if (rbacManager.forDomain(domain.uid).can(BasicRoles.ROLE_ADMIN)) {
			return domain;
		} else {
			Domain filtered = domain.value.copy();
			filtered.properties = Collections.emptyMap();
			return ItemValue.create(domain.uid, filtered);
		}
	}

	@Override
	public void setProperties(String uid, Map<String, String> properties) throws ServerFault {
		rbacManager.forDomain(uid).check(BasicRoles.ROLE_ADMIN);
		ParametersValidator.notNullAndNotEmpty(uid);

		final ItemValue<Domain> domainItem = get(uid);
		if (domainItem == null) {
			throw new DomainNotFoundException(uid);
		}

		if (domainItem.value.properties == null) {
			domainItem.value.properties = new HashMap<>();
		}

		Map<String, String> oldProps = new HashMap<>();
		oldProps.putAll(domainItem.value.properties);

		domainItem.value.properties.putAll(properties);

		store.update(domainItem.uid, domainItem.value.label, domainItem.value);

		for (IDomainHook hook : hooks.get()) {
			hook.onPropertiesUpdated(context, domainItem, oldProps, properties);
		}
		domainsCache.invalidate(domainItem.uid);
	}

	@Override
	public String getExternalUrl(String domainUid) {
		rbacManager.forDomain(domainUid).check(BasicRoles.ROLE_ADMIN);

		return Optional.ofNullable(context.provider().instance(IDomainSettings.class, domainUid).get())
				.map(ds -> ds.get(DomainSettingsKeys.external_url.name()))
				.orElseGet(() -> Optional
						.ofNullable(context.su().provider().instance(ISystemConfiguration.class).getValues()
								.stringValue(SysConfKeys.external_url.name()))
						.orElseThrow(
								() -> new ServerFault("No external URL found for this domain!", ErrorCode.NOT_FOUND)));
	}
}
