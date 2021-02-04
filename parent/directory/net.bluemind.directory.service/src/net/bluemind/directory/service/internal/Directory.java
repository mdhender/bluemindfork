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
package net.bluemind.directory.service.internal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.vertx.core.json.JsonObject;
import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.Regex;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainersFlatHierarchy;
import net.bluemind.core.container.api.IDataShardSupport;
import net.bluemind.core.container.api.IFlatHierarchyUids;
import net.bluemind.core.container.api.IOwnerSubscriptionUids;
import net.bluemind.core.container.api.IOwnerSubscriptions;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ContainerSyncStatus;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ContainerSettingsStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ContainerSyncStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.service.internal.DirEntryPermission;
import net.bluemind.core.container.service.internal.Permission;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirEntryPath;
import net.bluemind.directory.persistence.ManageableOrgUnit;
import net.bluemind.directory.service.DirEntryHandler;
import net.bluemind.directory.service.DirEntryHandlers;
import net.bluemind.directory.service.IInCoreDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.exchange.mapi.api.IMapiFolder;
import net.bluemind.exchange.mapi.api.IMapiFolderAssociatedInformation;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxMgmt;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tag.api.ITags;
import net.bluemind.todolist.api.ITodoList;

public class Directory {

	private static final Logger logger = LoggerFactory.getLogger(Directory.class);
	private BmContext context;
	private DirEntryStoreService itemStore;
	private String domainUid;
	private RBACManager rbacManager;
	private ItemValue<Domain> domain;
	private DirEntriesCache cache;

	public Directory(BmContext context, Container dirContainer, ItemValue<Domain> domain) {
		this.domainUid = domain.uid;
		this.context = context;
		this.itemStore = new DirEntryStoreService(this.context, dirContainer, domain.uid);
		rbacManager = new RBACManager(context).forContainer(dirContainer);
		this.domain = domain;
		cache = DirEntriesCache.get(context, domainUid);
	}

	public ItemValue<DirEntry> getRoot() throws ServerFault {
		checkReadAccess();
		return itemStore.get(domainUid, null);
	}

	public ItemValue<DirEntry> findByEntryUid(String entryUid) throws ServerFault {
		checkReadAccess();
		return cache.get(entryUid, () -> itemStore.get(entryUid, null));
	}

	public ItemValue<DirEntry> getEntry(String path) throws ServerFault {
		return findByEntryUid(IDirEntryPath.getEntryUid(path));
	}

	public List<ItemValue<DirEntry>> getEntries(String path) throws ServerFault {
		checkReadAccess();
		List<ItemValue<DirEntry>> res = itemStore.getEntries(path);
		return res.stream().filter(e -> !e.value.path.equals(path)).collect(Collectors.toList());
	}

	public TaskRef delete(String path) throws ServerFault {
		// write access will be tested in handler.entryDeleted
		checkReadAccess();

		List<ItemValue<DirEntry>> res = itemStore.getEntries(path);
		if (res.size() == 0) {
			throw new ServerFault("entry " + path + " doesnt exists", ErrorCode.NOT_FOUND);
		} else if (res.size() > 1) {
			throw new ServerFault("entry " + path + " has children", ErrorCode.INVALID_QUERY);
		}

		ItemValue<DirEntry> dir = res.get(0);
		DirEntryHandler handler = DirEntryHandlers.byKind(dir.value.kind);

		return handler.entryDeleted(context, domainUid, dir.value.entryUid);
	}

	public TaskRef deleteByEntryUid(String entryUid) throws ServerFault {
		checkReadAccess();
		ItemValue<DirEntry> dir = itemStore.get(entryUid, null);
		if (dir == null) {
			throw new ServerFault("entry " + entryUid + " doesnt exists", ErrorCode.NOT_FOUND);
		}
		DirEntryHandler handler = DirEntryHandlers.byKind(dir.value.kind);

		return handler.entryDeleted(context, domainUid, dir.value.entryUid);
	}

	public ContainerChangelog changelog(Long since) throws ServerFault {
		checkReadAccess();
		return itemStore.changelog(since, Long.MAX_VALUE);
	}

	public ContainerChangeset<String> changeset(Long since) throws ServerFault {
		return itemStore.changeset(since, Long.MAX_VALUE);
	}

	public List<ItemValue<DirEntry>> getMultiple(List<String> uids) throws ServerFault {
		checkReadAccess();
		return itemStore.getMultiple(uids);
	}

	public ListResult<ItemValue<DirEntry>> search(DirEntryQuery query) throws ServerFault {
		checkReadAccess();

		ParametersValidator.notNull(query);
		if (!Strings.isNullOrEmpty(query.emailFilter) && !Regex.EMAIL.validate(query.emailFilter)) {
			throw new ServerFault("emailFilter is not valid ", ErrorCode.INVALID_PARAMETER);
		}

		if (!Strings.isNullOrEmpty(query.emailFilter)) {
			String[] parts = query.emailFilter.split("@");
			if (!domain.uid.equals(parts[1]) && !domain.value.aliases.contains(parts[1])) {
				return ListResult.create(Collections.emptyList());
			}
		}

		if (query.entries != null) {
			int from = 0;
			int to = query.entries.size();
			if (query.from > 0) {
				from = query.from;
			}

			if (query.size > 0) {
				to = Math.min(from + query.size, query.entries.size());
			}

			List<String> uids = query.entries.subList(from, to);

			List<ItemValue<DirEntry>> values = itemStore.getMultiple(uids);
			return ListResult.create(values, query.entries.size());

		} else if (query.onlyManagable) {
			List<ManageableOrgUnit> manageable = getManageableDirEntries();
			return itemStore.searchManageable(query, manageable);
		} else {
			return itemStore.search(query);
		}
	}

	private List<ManageableOrgUnit> getManageableDirEntries() {
		RBACManager rbacManager = RBACManager.forContext(context).forDomain(domainUid);
		List<ManageableOrgUnit> ret = new ArrayList<>();
		for (Map.Entry<String, Set<String>> ouEntry : context.getSecurityContext().getRolesByOrgUnits().entrySet()) {
			Set<Permission> perms = rbacManager.forOrgUnit(ouEntry.getKey()).resolve();
			Set<Kind> kinds = perms.stream().filter((perm) -> perm instanceof DirEntryPermission)
					.map((perm) -> ((DirEntryPermission) perm).getKind()).collect(Collectors.toSet());
			if (!kinds.isEmpty()) {
				ret.add(new ManageableOrgUnit(ouEntry.getKey(), kinds));
			}
		}

		Set<Permission> perms = rbacManager.forDomain(domainUid).resolve();
		Set<Kind> kinds = perms.stream().filter((perm) -> perm instanceof DirEntryPermission)
				.map((perm) -> ((DirEntryPermission) perm).getKind()).collect(Collectors.toSet());
		if (!kinds.isEmpty()) {
			ret.add(new ManageableOrgUnit(null, kinds));
		}

		return ret;
	}

	public byte[] getEntryIcon(String entryUid) throws ServerFault {
		// FIXME anonymous can read icon ?
		// accessManager.checkReadAccess();
		ItemValue<DirEntry> itemValue = itemStore.get(entryUid, null);
		if (itemValue != null) {
			return DirEntryHandlers.byKind(itemValue.value.kind).getIcon(context, domainUid, itemValue.value.entryUid);
		} else {
			return null;
		}
	}

	public byte[] getIcon(String path) throws ServerFault {
		// FIXME anonymous can read icon ?
		// accessManager.checkReadAccess();
		List<ItemValue<DirEntry>> entries = itemStore.getEntries(path);
		if (entries.size() > 0) {
			return DirEntryHandlers.byKind(entries.get(0).value.kind).getIcon(context, domainUid,
					entries.get(0).value.entryUid);
		} else {
			return null;
		}

	}

	public Set<String> getRolesForDirEntry(String entryUid) throws ServerFault {
		return new HashSet<>(rbacManager.forEntry(entryUid).roles());
	}

	public Set<String> getRolesForOrgUnit(String ouUid) throws ServerFault {
		return new HashSet<>(rbacManager.forOrgUnit(ouUid).roles());
	}

	public ItemValue<VCard> getVCard(String uid) throws ServerFault {
		return itemStore.getVCard(uid);
	}

	public byte[] getEntryPhoto(String entryUid) throws ServerFault {
		return itemStore.getPhoto(entryUid);
	}

	public ItemValue<DirEntry> getByEmail(String email) throws ServerFault {
		checkReadAccess();
		if (!Regex.EMAIL.validate(email)) {
			throw new ServerFault("emailFilter is not valid ", ErrorCode.INVALID_PARAMETER);
		}

		email = email.toLowerCase();
		String domainPart = email.split("@")[1];
		boolean isDomainEmail = domain.value.aliases.contains(domainPart) || domainPart.equals(domain.value.name);
		return itemStore.getByEmail(email, isDomainEmail);
	}

	private void checkReadAccess() {
		rbacManager.check(Verb.Read.name(), BasicRoles.ROLE_ADMIN);
	}

	public TaskRef xfer(String entryUid, String serverUid) throws ServerFault {
		rbacManager.forEntry(entryUid).check(BasicRoles.ROLE_MANAGE_MAILBOX);

		logger.info("[{}] xfer to {}", entryUid, serverUid);

		return context.provider().instance(ITasksManager.class).run("xfer-" + entryUid + "@" + domainUid, monitor -> {
			try {
				doXfer(entryUid, serverUid, monitor);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		});

	}

	private void doXfer(String entryUid, String serverUid, IServerTaskMonitor monitor) {
		ItemValue<Server> destination = ServerSideServiceProvider.getProvider(context)
				.instance(IServer.class, "default").getComplete(serverUid);
		if (destination == null) {
			logger.error("fail to transfert data. entryUid {}, serverUid {}. Server not found", entryUid, serverUid);
			monitor.end(false, "destination server not found", "{}");
			return;
		}

		ItemValue<DirEntry> dirEntry = cache.get(entryUid, () -> itemStore.get(entryUid, null));
		if (dirEntry == null) {
			logger.error("fail to transfert data. entryUid {}, serverUid {}. DirEntry not found", entryUid, serverUid);
			monitor.end(false, "source entry not found", "{}");
			return;
		}

		if (dirEntry.value.kind != DirEntry.Kind.USER) {
			logger.error("fail to transfert data. entryUid {}, serverUid {}. Unsupported kind {}", entryUid, serverUid,
					dirEntry.value.kind);
			monitor.end(false, "source is not a user", "{}");
			return;
		}

		IServiceProvider sp = context.provider();
		// FIXME use sharding support pext for sizing
		monitor.begin(12, "moving 12 container types");

		xferContainer(monitor.subWork(1), dirEntry, serverUid, "addressbook", (String containerUid) -> {
			return sp.instance(IAddressBook.class, containerUid);
		});

		logger.info("Starting cal xfer for {}", entryUid);
		xferContainer(monitor.subWork(1), dirEntry, serverUid, "calendar", (String containerUid) -> {
			return sp.instance(ICalendar.class, containerUid);
		});
		logger.info("Ending cal xfer for {}", entryUid);

		xferContainer(monitor.subWork(1), dirEntry, serverUid, "todolist", (String containerUid) -> {
			return sp.instance(ITodoList.class, containerUid);
		});

		xferContainer(monitor.subWork(1), dirEntry, serverUid, "tags", (String containerUid) -> {
			return sp.instance(ITags.class, containerUid);
		});

		xferContainer(monitor.subWork(1), dirEntry, serverUid, "mailbox_records", (String containerUid) -> {
			return sp.instance(IDbMailboxRecords.class, IMailReplicaUids.uniqueId(containerUid));
		});

		xferContainer(monitor.subWork(1), dirEntry, serverUid, "mapi_folder", (String containerUid) -> {
			return sp.instance(IMapiFolder.class, containerUid);
		});

		xferContainer(monitor.subWork(1), dirEntry, serverUid, "mapi_fai", (String containerUid) -> {
			return sp.instance(IMapiFolderAssociatedInformation.class, containerUid.substring("mapi_fai_".length()));
		});

		xferContainer(monitor.subWork(1), dirEntry, serverUid, IFlatHierarchyUids.TYPE, (String containerUid) -> {
			return sp.instance(IContainersFlatHierarchy.class, domainUid, entryUid);
		});

		xferContainer(monitor.subWork(1), dirEntry, serverUid, IOwnerSubscriptionUids.TYPE, (String containerUid) -> {
			return sp.instance(IOwnerSubscriptions.class, domainUid, entryUid);
		});

		ItemValue<Mailbox> mailbox = sp.instance(IMailboxes.class, domainUid).getComplete(entryUid);

		if (mailbox != null) {
			xferContainer(monitor.subWork(1), dirEntry, serverUid, "replicated_mailboxes", (String containerUid) -> {
				CyrusPartition part = CyrusPartition.forServerAndDomain(serverUid, domainUid);
				return sp.instance(IDbReplicatedMailboxes.class, part.name, "user." + mailbox.value.name);
			});

			logger.info("[{}] xfer mailbox", entryUid);
			sp.instance(IMailboxMgmt.class, domainUid).move(mailbox, destination);
			monitor.progress(1, "mailbox moved.");
		} else {
			monitor.progress(2, "no mailbox to move.");
		}

		// update dataLocation
		dirEntry.value.dataLocation = serverUid;
		sp.instance(IInCoreDirectory.class, domainUid).update(entryUid, dirEntry.value);
		if (mailbox != null) {
			VertxPlatform.eventBus().publish("postfix.map.dirty", new JsonObject());
		}

		monitor.end(true, "user transfered", "{}");
		logger.info("Ending xfer of {} to {}", entryUid, serverUid);
	}

	private void xferContainer(IServerTaskMonitor mon, ItemValue<DirEntry> dirEntry, String destination,
			String containerType, Function<String, IDataShardSupport> fn) {
		try {

			ContainerStore dirContainerStore = new ContainerStore(context, context.getDataSource(),
					context.getSecurityContext());

			DataSource origDs = context.getMailboxDataSource(dirEntry.value.dataLocation);
			ContainerStore containerStoreOrig = new ContainerStore(null, origDs, context.getSecurityContext());

			List<Container> containers = containerStoreOrig.findByTypeAndOwner(containerType, dirEntry.uid);
			logger.info("[{}] xfer {} {} ({})", dirEntry.uid, containers.size(), containerType, containers);

			DataSource targetDs = context.getMailboxDataSource(destination);
			ContainerStore containerStoreTarget = new ContainerStore(null, targetDs, context.getSecurityContext());
			mon.begin(containers.size(), "processing " + containers.size() + " container(s)");
			for (Container c : containers) {
				IDataShardSupport service = fn.apply(c.uid);

				logger.info("[{}] xfer container {}", dirEntry.uid, c.uid);
				Container oldContainer = containerStoreOrig.get(c.uid);

				Container newContainer = containerStoreTarget.create(c);
				dirContainerStore.createContainerLocation(newContainer, destination);

				service.xfer(destination);

				dirContainerStore.invalidateCache(c.uid);
				DataSourceRouter.invalidateContainer(c.uid);

				// clear old container data

				// acl
				AclStore aclStoreOrig = new AclStore(context, origDs);
				List<AccessControlEntry> acls = aclStoreOrig.get(oldContainer);
				if (acls != null && !acls.isEmpty()) {
					AclStore aclStoreTarget = new AclStore(context, targetDs);
					aclStoreTarget.store(newContainer, acls);
					aclStoreOrig.deleteAll(oldContainer);
				}

				// sync status
				ContainerSyncStore containerSyncStoreOrig = new ContainerSyncStore(origDs, oldContainer);
				ContainerSyncStatus ss = containerSyncStoreOrig.getSyncStatus();
				if (ss != null) {
					ContainerSyncStore containerSyncStoreTarget = new ContainerSyncStore(targetDs, newContainer);
					containerSyncStoreTarget.setSyncStatus(ss);
					containerSyncStoreOrig.delete();
				}

				// settings
				ContainerSettingsStore containerSettingStoreOrig = new ContainerSettingsStore(origDs, oldContainer);
				Map<String, String> settings = containerSettingStoreOrig.getSettings();
				if (settings != null && !settings.isEmpty()) {
					ContainerSettingsStore containerSettingStoreTarget = new ContainerSettingsStore(targetDs,
							newContainer);
					containerSettingStoreTarget.setSettings(settings);
					containerSettingStoreOrig.delete();
				}

				// delete old container
				containerStoreOrig.delete(c.uid);
				mon.progress(1, c.uid + " tranferred.");
			}
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

}
