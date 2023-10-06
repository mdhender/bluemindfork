/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlogs.ILogMapperProvider;
import net.bluemind.core.container.api.internal.IAccessControlList;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;

public class AclService implements IAccessControlList {
	private final AclStore aclStore;
	private final AuditLogService<AccessControlEntry, AccessControlEntry> auditLog;
	private final Container container;

	public AclService(BmContext ctx, SecurityContext sc, DataSource pool, BaseContainerDescriptor desc) {
		container = Container.create(desc.uid, desc.type, desc.name, desc.owner, desc.domainUid, desc.defaultContainer);
		container.id = desc.internalId;
		ILogMapperProvider<AccessControlEntry> mapper = new AccessControlEntryAuditLogMapper(container);
		aclStore = new AclStore(ctx, pool);
		auditLog = new ValueAuditLogService<>(sc, desc, mapper);
		auditLog.setType("containeracl");
	}

	public AclService(BmContext ctx, SecurityContext sc, DataSource pool, Container cont) {
		BaseContainerDescriptor desc = BaseContainerDescriptor.create(cont.uid, cont.name, cont.owner, cont.type,
				cont.domainUid, cont.defaultContainer);
		desc.internalId = cont.id;
		container = cont;
		ILogMapperProvider<AccessControlEntry> mapper = new AccessControlEntryAuditLogMapper(container);
		aclStore = new AclStore(ctx, pool);
		auditLog = new ValueAuditLogService<>(sc, desc, mapper);
		auditLog.setType("containeracl");
	}

	@Override
	public void store(final List<AccessControlEntry> entries) {
		try {
			List<AccessControlEntry> compacted = compact(entries);
			compacted.forEach(auditLog::logCreate);
			aclStore.store(container, compacted);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void add(final List<AccessControlEntry> entries) {
		try {
			List<AccessControlEntry> compacted = compact(entries);
			compacted.forEach(auditLog::logCreate);
			aclStore.add(container, compacted);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public List<AccessControlEntry> get() {
		try {
			return expand(addOwnerRights(aclStore.get(container)));
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void deleteAll() {
		try {
			List<AccessControlEntry> entries = aclStore.get(container);
			entries.forEach(auditLog::logDelete);
			aclStore.deleteAll(container);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public List<AccessControlEntry> retrieveAndStore(List<AccessControlEntry> entries) {
		try {
			List<AccessControlEntry> compacted = compact(entries);
			List<AccessControlEntry> oldEntries = aclStore.retrieveAndStore(container, compacted);
			compacted.stream().filter(e -> !oldEntries.contains(e)).forEach(e -> auditLog.logUpdate(e, null));
			oldEntries.stream().filter(e -> !entries.contains(e)).forEach(auditLog::logDelete);
			return expand(addOwnerRights(oldEntries));
		} catch (ServerFault e) {
			throw ServerFault.sqlFault(e);

		}
	}
	
	private List<AccessControlEntry> expand(List<AccessControlEntry> list) {
		return list.stream()
				.flatMap(
						acl -> Verb.expand(acl.verb).stream().map(verb -> AccessControlEntry.create(acl.subject, verb)))
				.distinct().collect(Collectors.toList());
	}

	private List<AccessControlEntry> compact(List<AccessControlEntry> list) {
		Map<String, List<Verb>> aclBySubject = list.stream().collect(Collectors.groupingBy(
				AccessControlEntry::getSubject, Collectors.mapping(AccessControlEntry::getVerb, Collectors.toList())));
		return aclBySubject.keySet().stream().flatMap(subject -> Verb.compact(aclBySubject.get(subject)).stream()
				.map(verb -> AccessControlEntry.create(subject, verb))).collect(Collectors.toList());
	}

	private List<AccessControlEntry> addOwnerRights(List<AccessControlEntry> list) {
		if (isOwnedByAUser()) {
			Set<AccessControlEntry> extended = new HashSet<>(list);
			extended.add(AccessControlEntry.create(container.owner, Verb.All));
			return new ArrayList<>(extended);
		}
		return list;
	}

	private boolean isOwnedByAUser() {
		IDirectory directoryService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDirectory.class, container.domainUid);
		DirEntry owner = directoryService.findByEntryUid(container.owner);
		return owner != null && owner.kind.equals(Kind.USER);
	}

}
