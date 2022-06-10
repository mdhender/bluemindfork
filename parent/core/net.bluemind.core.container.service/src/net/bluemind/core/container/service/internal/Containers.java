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

import static net.bluemind.core.container.service.internal.ReadOnlyMode.checkWritable;
import static net.bluemind.core.jdbc.JdbcAbstractStore.doOrContinue;
import static net.bluemind.core.jdbc.JdbcAbstractStore.doOrFail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.authentication.api.incore.IInCoreAuthentication;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.hooks.IContainersHook;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ContainerModifiableDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ChangelogStore;
import net.bluemind.core.container.persistence.ContainerPersonalSettingsStore;
import net.bluemind.core.container.persistence.ContainerSettingsStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ContainerSyncStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.sharding.Sharding;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.core.validator.Validator;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.i18n.labels.I18nLabels;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.user.persistence.UserSubscriptionStore;

public class Containers implements IContainers {

	private static final Set<String> DATA_CONTAINER_TYPES = Sharding.containerTypes();
	private final SecurityContext securityContext;
	private final Sanitizer sanitizer;
	private final BmContext context;
	private Validator validator;

	private static final List<IContainersHook> cHooks = loadContainerHooks();
	private static final Logger logger = LoggerFactory.getLogger(Containers.class);

	private static List<IContainersHook> loadContainerHooks() {
		RunnableExtensionLoader<IContainersHook> rel = new RunnableExtensionLoader<>();
		return rel.loadExtensions("net.bluemind.core.container.hooks", "container", "hook", "impl");
	}

	public Containers(BmContext context) {
		this.securityContext = context.getSecurityContext();
		this.context = context;
		sanitizer = new Sanitizer(context);
		validator = new Validator(context);
	}

	@Override
	public void create(String uid, ContainerDescriptor descriptor) throws ServerFault {
		checkWritable();

		sanitizer.create(descriptor);
		validator.create(descriptor);

		if (!securityContext.isDomainGlobal()) {
			if (descriptor.domainUid == null) {
				throw new ServerFault("Only admin0 can create container without domain", ErrorCode.FORBIDDEN);
			} else if (!securityContext.isDomainAdmin(descriptor.domainUid)
					&& !securityContext.getSubject().equals(descriptor.owner)) {
				throw new ServerFault("User cannot create container without being the owner", ErrorCode.FORBIDDEN);
			}

		}
		Container container = Container.create(uid, descriptor.type, descriptor.name, descriptor.owner,
				descriptor.domainUid, descriptor.defaultContainer);
		container.readOnly = descriptor.readOnly;

		DataSource ds = null;
		ContainerStore directoryDataStore = new ContainerStore(context, context.getDataSource(), securityContext);

		String loc = null;
		if (!"global.virt".equals(container.domainUid) && DATA_CONTAINER_TYPES.contains(descriptor.type)
				&& !pfDirEntry(descriptor.owner, descriptor.domainUid)) {
			DirEntry entry = context.su().provider().instance(IDirectory.class, container.domainUid)
					.findByEntryUid(descriptor.owner);

			if (entry != null) {
				// domain OR domain AB
				if (entry.kind == Kind.DOMAIN || (entry.kind == Kind.ADDRESSBOOK && descriptor.defaultContainer
						&& descriptor.uid.equals("addressbook_" + container.domainUid))) {
					ds = context.getDataSource();
				} else if (entry.dataLocation != null) {
					loc = entry.dataLocation;
					ds = context.getMailboxDataSource(entry.dataLocation);
				} else {
					throw new ServerFault("Failed to create container " + container + " : no datalocation for owner "
							+ entry.entryUid);
				}
			} else {
				throw new ServerFault("Failed to create container " + container + " : owner not found");
			}

		} else {
			ds = context.getDataSource();
		}

		if (ds == null) {
			throw new ServerFault("Failed to create container " + container + " : Null datasource");
		}

		final String location = loc;
		final ContainerStore cs = new ContainerStore(null, ds, securityContext);
		JdbcAbstractStore.doOrFail(() -> cs.create(container));
		JdbcAbstractStore.doOrFail(() -> {
			directoryDataStore.createOrUpdateContainerLocation(container, location);
			return null;
		});

		for (IContainersHook ch : cHooks) {
			doOrContinue("onContainerCreated", () -> {
				ch.onContainerCreated(context, descriptor);
				return null;
			});
		}
	}

	private static final Map<String, String> domainToPF = new ConcurrentHashMap<>();

	/**
	 * This is similar to PublicFolders#mailboxGuid but we don't want to depend on
	 * that.
	 * 
	 * @param owner
	 * @param domainUid
	 * @return
	 */
	private boolean pfDirEntry(String owner, String domainUid) {
		return owner != null && domainUid != null && owner.equals(
				domainToPF.computeIfAbsent(domainUid, dom -> UUID.nameUUIDFromBytes(dom.getBytes()).toString()));
	}

	@Override
	public void delete(String uid) throws ServerFault {
		checkWritable();
		DataSource dataSource = DataSourceRouter.get(context, uid);
		ContainerStore containerStore = new ContainerStore(context, dataSource, securityContext);

		Container container = doOrContinue("onContainerDeleted", () -> containerStore.get(uid));
		if (container == null) {
			logger.warn("container {} not found, cannot delete it", uid);
			return;
		}

		RBACManager.forContext(context).forContainer(container).check(Verb.Manage.name());

		ContainerDescriptor prev = asDescriptor(container, null);

		// t_container_sub lives in directory db
		ContainerStore dirContStore = new ContainerStore(context, context.getDataSource(), securityContext);
		List<String> subs = doOrFail(() -> dirContStore.listSubscriptions(container));

		// We call the hooks before otherwise we loose the infos to retry hooks
		// if something fails
		if (!subs.isEmpty()) {
			logger.info("Removing {} subscription(s) to {}", subs.size(), uid);
			for (IContainersHook ch : cHooks) {
				ch.onContainerSubscriptionsChanged(context, prev, Collections.emptyList(), subs);
			}
		}

		JdbcAbstractStore.doOrFail(() -> {
			dirContStore.deleteAllSubscriptions(container);
			return null;
		});

		ContainerPersonalSettingsStore personalSettingsStore = new ContainerPersonalSettingsStore(dataSource,
				context.getSecurityContext(), container);
		ContainerSettingsStore settingsStore = new ContainerSettingsStore(dataSource, container);
		ContainerSyncStore syncStore = new ContainerSyncStore(dataSource, container);
		AclStore aclStore = new AclStore(context, dataSource);

		ContainerStore directoryDataStore = new ContainerStore(context, context.getDataSource(), securityContext);

		JdbcAbstractStore.doOrFail(() -> {
			new ChangelogStore(dataSource, container).deleteLog();
			personalSettingsStore.deleteAll();
			settingsStore.delete();
			syncStore.suspendSync();
			aclStore.deleteAll(container);
			directoryDataStore.deleteContainerLocation(container);
			containerStore.deleteKnownIdUid(container.id, uid);
			return null;
		});

		for (IContainersHook ch : cHooks) {
			doOrContinue("onContainerDeleted", () -> {
				ch.onContainerDeleted(context, prev);
				return null;
			});
		}

		DataSourceRouter.invalidateContainer(uid);

		eventProducer().changed(prev.type, uid);
	}

	@Override
	public void update(String uid, ContainerModifiableDescriptor descriptor) throws ServerFault {
		checkWritable();
		DataSource dataSource = DataSourceRouter.get(context, uid);
		ContainerStore containerStore = new ContainerStore(context, dataSource, securityContext);

		Container container = doOrFail(() -> {
			Container c = containerStore.get(uid);
			ContainerDescriptor prev = ContainerDescriptor.create(c.uid, c.name, c.owner, c.type, c.domainUid, false);

			sanitizer.update(prev, descriptor);
			validator.update(prev, descriptor);

			RBACManager.forContext(context).forContainer(c).check(Verb.Manage.name());

			containerStore.updateName(uid, descriptor.name);

			return c;
		});

		ContainerDescriptor prev = ContainerDescriptor.create(container.uid, container.name, container.owner,
				container.type, container.domainUid, false);

		ContainerDescriptor cur = ContainerDescriptor.create(container.uid, descriptor.name, container.owner,
				container.type, container.domainUid, false);
		cur.deleted = descriptor.deleted;

		for (IContainersHook ch : cHooks) {
			doOrContinue("onContainerUpdated", () -> {
				ch.onContainerUpdated(context, prev, cur);
				return null;
			});
		}

		eventProducer().changed(prev.type, uid);
	}

	@Override
	public List<ContainerDescriptor> getContainers(List<String> containerIds) throws ServerFault {
		RBACManager.forContext(context).checkNotAnoynmous();

		// FIXME perm check is missing
		List<ContainerDescriptor> ret = new ArrayList<>(containerIds.size());
		containerIds.forEach(uid -> {
			DataSource dataSource = DataSourceRouter.get(context, uid);
			ContainerStore containerStore = new ContainerStore(context, dataSource, securityContext);
			Optional.ofNullable(doOrFail(() -> containerStore.get(uid)))
					.ifPresent(c -> ret.add(asDescriptor(c, securityContext)));
		});

		return ret;
	}

	@Override
	public List<BaseContainerDescriptor> getContainersLight(List<String> containerIds) throws ServerFault {
		RBACManager.forContext(context).checkNotAnoynmous();

		// FIXME perm check is missing
		List<BaseContainerDescriptor> ret = new ArrayList<>(containerIds.size());
		Map<String, IDirectory> dirApiByDomain = new HashMap<>();

		containerIds.forEach(uid -> {
			DataSource dataSource = DataSourceRouter.get(context, uid);
			ContainerStore containerStore = new ContainerStore(context, dataSource, securityContext);
			Optional.ofNullable(doOrFail(() -> containerStore.get(uid))).ifPresent(c -> {
				IDirectory dirApi = c.domainUid != null
						? dirApiByDomain.computeIfAbsent(c.domainUid,
								dom -> context.su().provider().instance(IDirectory.class, dom))
						: null;
				ret.add(asDescriptorLight(c, securityContext, dirApi));
			});
		});

		return ret;
	}

	@Override
	public List<ContainerDescriptor> all(ContainerQuery query) throws ServerFault {
		RBACManager.forContext(context).checkNotAnoynmous();

		List<ContainerDescriptor> ret = new ArrayList<>();

		ret.addAll(allContainers(context.getAllMailboxDataSource(), query, this::isSharded));
		ret.addAll(allContainers(Arrays.asList(context.getDataSource()), query, c -> true));
		return dedup(ret);
	}

	private List<ContainerDescriptor> allContainers(Collection<DataSource> dataSources, ContainerQuery query,
			Predicate<ContainerDescriptor> filter) {
		return dataSources.stream().flatMap(ds -> {
			List<ContainerDescriptor> containers = new ArrayList<>();
			ContainerStore containerStore = new ContainerStore(context, ds, securityContext);
			if (query.owner != null && query.type != null) {
				// FIXME not everybody should be able to call this
				containers.addAll(asDescriptors(
						doOrFail(() -> containerStore.findByTypeOwnerReadOnly(query.type, query.owner, query.readonly)),
						securityContext));
			} else {
				containers.addAll(
						asDescriptors(doOrFail(() -> containerStore.findAccessiblesByType(query)), securityContext));
			}
			return containers.stream().filter(filter);
		}).collect(Collectors.toList());
	}

	private boolean isSharded(BaseContainerDescriptor container) {
		return isShardedType(container.type);
	}

	private boolean isShardedContainer(Container container) {
		return isShardedType(container.type);
	}

	private boolean isShardedType(String type) {
		return Sharding.containerTypes().contains(type);
	}

	private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
		Set<Object> seen = ConcurrentHashMap.newKeySet();
		return t -> seen.add(keyExtractor.apply(t));
	}

	private static <W extends BaseContainerDescriptor> List<W> dedup(List<W> orig) {
		return orig.stream().filter(distinctByKey(c -> c.uid)).collect(Collectors.toList());
	}

	@Override
	public List<BaseContainerDescriptor> allLight(ContainerQuery query) throws ServerFault {
		RBACManager.forContext(context).checkNotAnoynmous();

		List<Container> ret = new ArrayList<>();
		ret.addAll(queryContainer(query, context.getAllMailboxDataSource(), this::isShardedContainer, query.readonly,
				securityContext));
		ret.addAll(queryContainer(query, Arrays.asList(context.getDataSource()), c -> true, query.readonly,
				securityContext));

		return dedup(asDescriptorsLight(ret, securityContext));
	}

	@Override
	public List<ContainerDescriptor> allForUser(String domainUid, String userUid, ContainerQuery query)
			throws ServerFault {

		if (!context.getSecurityContext().isDomainAdmin(domainUid)) {
			throw new ServerFault("You have to be domain admin to call this method", ErrorCode.FORBIDDEN);
		}

		SecurityContext sc = context.provider().instance(IInCoreAuthentication.class).buildContext(domainUid, userUid);

		List<Container> ret = new ArrayList<>();
		ret.addAll(queryContainer(query, context.getAllMailboxDataSource(), this::isShardedContainer, null, sc));
		ret.addAll(queryContainer(query, Arrays.asList(context.getDataSource()), c -> true, null, sc));

		return dedup(asDescriptors(ret, sc));
	}

	private List<Container> queryContainer(ContainerQuery query, Collection<DataSource> dataSources,
			Predicate<Container> filter, Boolean readOnly, SecurityContext ctx) {
		List<Container> containers = new ArrayList<>();
		dataSources.forEach(ds -> {
			try {
				ContainerStore containerStore = new ContainerStore(context, ds, ctx);
				if (query.owner != null) {
					// FIXME not everybody should be able to call this
					containers.addAll(
							doOrFail(() -> containerStore.findByTypeOwnerReadOnly(query.type, query.owner, readOnly)));
				} else {
					containers.addAll(doOrFail(() -> containerStore.findAccessiblesByType(query)));
				}
			} catch (Exception e) {
				logger.warn("Fail to fetch containers on datasource {}", ds);
			}

		});
		return containers.stream().filter(filter).collect(Collectors.toList());
	}

	@Override
	public ContainerDescriptor getForUser(String domainUid, String userUid, String uid) throws ServerFault {

		if (!context.getSecurityContext().isDomainAdmin(domainUid)) {
			throw new ServerFault("You have to be domain admin to call this method", ErrorCode.FORBIDDEN);
		}

		SecurityContext userContext = context.provider().instance(IInCoreAuthentication.class).buildContext(domainUid,
				userUid);

		ContainerStore suContainerStore = new ContainerStore(context, DataSourceRouter.get(context, uid), userContext);
		Container c = doOrFail(() -> suContainerStore.get(uid));
		if (c == null) {
			throw new ServerFault("Container '" + uid + "' not found", ErrorCode.NOT_FOUND);
		}
		return asDescriptor(c, userContext);

	}

	public List<ContainerDescriptor> asDescriptors(List<Container> containers, SecurityContext sc) throws ServerFault {
		List<ContainerDescriptor> ret = new ArrayList<>(containers.size());
		for (Container c : containers) {
			ret.add(asDescriptor(c, sc));
		}
		return ret;
	}

	private List<BaseContainerDescriptor> asDescriptorsLight(List<Container> containers, SecurityContext sc)
			throws ServerFault {
		List<BaseContainerDescriptor> ret = new ArrayList<>(containers.size());
		Map<String, IDirectory> dirApiByDomain = new HashMap<>();
		for (Container c : containers) {
			IDirectory dirApi = c.domainUid != null
					? dirApiByDomain.computeIfAbsent(c.domainUid,
							dom -> context.su().provider().instance(IDirectory.class, dom))
					: null;
			ret.add(asDescriptorLight(c, sc, dirApi));
		}
		return ret;
	}

	BaseContainerDescriptor asDescriptorLight(Container c, SecurityContext sc, IDirectory dirApi) throws ServerFault {
		DataSource dataSource = DataSourceRouter.get(context, c.uid);

		if (logger.isDebugEnabled()) {
			logger.debug("c: {}, context: {}", c, context);
		}
		String label = I18nLabels.getInstance().translate(sc.getLang(), c.name);
		BaseContainerDescriptor descriptor = BaseContainerDescriptor.create(c.uid, label, c.owner, c.type, c.domainUid,
				c.defaultContainer);

		if (descriptor.owner != null && dirApi != null) {
			try {
				DirEntry entry = dirApi.findByEntryUid(descriptor.owner);
				if (entry != null) {
					descriptor.ownerDisplayname = entry.displayName;
					descriptor.ownerDirEntryPath = entry.path;
				}
			} catch (Exception e) {
				logger.warn("error loading entry {}@{}", descriptor.owner, descriptor.domainUid, e);
			}
		}

		descriptor.readOnly = c.readOnly;
		descriptor.datalocation = getDatalocation(dataSource);

		ContainerSettingsStore settingsStore = new ContainerSettingsStore(dataSource, c);
		doOrFail(() -> {
			descriptor.settings = settingsStore.getSettings();
			if (descriptor.settings == null) {
				descriptor.settings = Collections.emptyMap();
			}
			return null;
		});
		return descriptor;
	}

	ContainerDescriptor asDescriptor(Container c, SecurityContext sc) throws ServerFault {
		DataSource dataSource = DataSourceRouter.get(context, c.uid);
		String label = c.name;
		if (sc != null) {
			label = I18nLabels.getInstance().translate(sc.getLang(), c.name);
		}
		ContainerDescriptor descriptor = ContainerDescriptor.create(c.uid, label, c.owner, c.type, c.domainUid,
				c.defaultContainer);
		descriptor.internalId = c.id;
		descriptor.datalocation = getDatalocation(dataSource);

		if (sc != null) {
			RBACManager aclForContainer = RBACManager.forSecurityContext(sc).forContainer(c);
			descriptor.verbs = aclForContainer.resolve().stream().filter(p -> p instanceof ContainerPermission)
					.map(p -> Verb.valueOf(p.asRole())).collect(Collectors.toSet());
			descriptor.writable = descriptor.verbs.stream().anyMatch(verb -> verb.can(Verb.Write));

			ContainerStore cs = new ContainerStore(context, context.getDataSource(), securityContext);
			Container dom = doOrFail(() -> cs.get(sc.getContainerUid()));
			UserSubscriptionStore userSubscriptionStore = new UserSubscriptionStore(context.getSecurityContext(),
					context.getDataSource(), dom);

			descriptor.offlineSync = doOrFail(() -> userSubscriptionStore.isSyncAllowed(sc.getSubject(), c));

		}

		if (descriptor.owner != null && descriptor.domainUid != null) {
			try {
				DirEntry entry = context.su().provider().instance(IDirectory.class, c.domainUid)
						.findByEntryUid(descriptor.owner);
				if (entry != null) {
					descriptor.ownerDisplayname = entry.displayName;
					descriptor.ownerDirEntryPath = entry.path;
				}
			} catch (Exception e) {
				logger.warn("error loading entry {}@{}", descriptor.owner, descriptor.domainUid, e);
			}
		}

		descriptor.readOnly = c.readOnly;

		ContainerSettingsStore settingsStore = new ContainerSettingsStore(dataSource, c);
		ContainerPersonalSettingsStore personalSettingsStore = new ContainerPersonalSettingsStore(dataSource,
				context.getSecurityContext(), c);
		doOrFail(() -> {
			descriptor.settings = settingsStore.getSettings();
			if (descriptor.settings == null) {
				descriptor.settings = new HashMap<>();
			}
			Map<String, String> psettings = personalSettingsStore.get();
			if (psettings != null) {
				descriptor.settings.putAll(psettings);
			}
			return null;
		});
		return descriptor;
	}

	private String getDatalocation(DataSource dataSource) {
		return ServerSideServiceProvider.mailboxDataSource.entrySet().stream()
				.filter(entry -> dataSource == entry.getValue()).map(Map.Entry::getKey).findFirst().orElse(null);
	}

	public ContainerDescriptor asDescriptorForUser(Container c, SecurityContext sc, String userUid) throws ServerFault {
		ContainerStore containerStore = new ContainerStore(context, context.getDataSource(), securityContext);
		Container dom = doOrFail(() -> containerStore.get(c.domainUid));

		ContainerDescriptor ret = asDescriptor(c, sc);

		UserSubscriptionStore userSubscriptionStore = new UserSubscriptionStore(context.getSecurityContext(),
				context.getDataSource(), dom);

		ret.offlineSync = doOrFail(() -> userSubscriptionStore.isSyncAllowed(userUid, c));
		return ret;
	}

	@Override
	public ContainerDescriptor get(String uid) throws ServerFault {
		RBACManager.forContext(context).checkNotAnoynmous();
		// FIXME we should check that, at least container.domainUid ==
		// user.domainUid
		// or domain admin

		DataSource dataSource = DataSourceRouter.get(context, uid);
		ContainerStore containerStore = new ContainerStore(context, dataSource, securityContext);
		Container c = doOrFail(() -> containerStore.get(uid));
		if (c == null) {
			throw ServerFault.notFound("Container '" + uid + "' not found");
		}
		ContainerDescriptor descriptor = asDescriptor(c, securityContext);
		return descriptor;

	}

	@Override
	public void setAccessControlList(String uid, List<AccessControlEntry> entries) throws ServerFault {
		IContainerManagement mgmt = context.provider().instance(IContainerManagement.class, uid);
		mgmt.setAccessControlList(entries);
	}

	@Override
	public ContainerDescriptor getIfPresent(String uid) throws ServerFault {
		RBACManager.forContext(context).checkNotAnoynmous();
		DataSource dataSource = DataSourceRouter.get(context, uid);
		ContainerStore containerStore = new ContainerStore(context, dataSource, securityContext);

		Container c = doOrFail(() -> containerStore.get(uid));
		if (c == null) {
			return null;
		} else {
			ContainerDescriptor descriptor = asDescriptor(c, securityContext);
			return descriptor;
		}
	}

	private ContainersEventProducer eventProducer() {
		return new ContainersEventProducer(context.getSecurityContext(), VertxPlatform.eventBus());
	}

}
