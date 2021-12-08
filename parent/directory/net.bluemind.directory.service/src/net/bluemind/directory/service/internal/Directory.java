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
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.Regex;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.DirEntryPermission;
import net.bluemind.core.container.service.internal.Permission;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirEntryPath;
import net.bluemind.directory.external.ExternalDirectories;
import net.bluemind.directory.external.IExternalDirectory;
import net.bluemind.directory.persistence.ManageableOrgUnit;
import net.bluemind.directory.service.DirEntryHandler;
import net.bluemind.directory.service.DirEntryHandlers;
import net.bluemind.domain.api.Domain;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.role.persistence.RoleStore;

public class Directory {

	private static final Logger logger = LoggerFactory.getLogger(Directory.class);

	private final BmContext context;
	private final DirEntryStoreService itemStore;
	private final String domainUid;
	private final RBACManager rbacManager;
	private final ItemValue<Domain> domain;
	private final DirEntriesCache cache;
	private final RoleStore roleStore;
	private final List<IExternalDirectory> externalDirSource;

	public Directory(BmContext context, Container dirContainer, ItemValue<Domain> domain) {
		this.domainUid = domain.uid;
		this.context = context;
		this.itemStore = new DirEntryStoreService(this.context, dirContainer, domain.uid);
		rbacManager = new RBACManager(context).forContainer(dirContainer);
		this.roleStore = new RoleStore(context.getDataSource(), dirContainer);
		this.domain = domain;
		cache = DirEntriesCache.get(context, domainUid);
		externalDirSource = new ExternalDirectories(domainUid).dirs();
	}

	public ItemValue<DirEntry> getRoot() throws ServerFault {
		checkReadAccess();
		return itemStore.get(domainUid, null);
	}

	public ItemValue<DirEntry> findByEntryUid(String entryUid) throws ServerFault {
		checkReadAccess();
		ItemValue<DirEntry> ret = cache.get(entryUid, () -> itemStore.get(entryUid, null));
		if (ret == null || ret.value == null) {
			for (IExternalDirectory ext : externalDirSource) {
				ret = ext.findByEntryUid(entryUid);
				if (ret != null) {
					logger.info("EXT Resolved {} with {} => {}", entryUid, ext, ret);
					break;
				}
			}
		}
		return ret;
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
		if (res.isEmpty()) {
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
				try (DirectoryXfer directoryXfer = new DirectoryXfer(context, domain, itemStore, entryUid, serverUid)) {
					directoryXfer.doXfer(entryUid, monitor);
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		});

	}

	public List<ItemValue<DirEntry>> getByRoles(List<String> roles) {
		checkReadAccess();
		List<String> itemsWithRoles;
		try {
			itemsWithRoles = new ArrayList<>(roleStore.getItemsWithRoles(roles));
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		return getMultiple(itemsWithRoles);
	}

}
