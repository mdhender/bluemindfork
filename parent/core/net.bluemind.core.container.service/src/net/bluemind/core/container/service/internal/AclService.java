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
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlogs.ILogMapperProvider;
import net.bluemind.core.container.api.internal.IAccessControlList;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;

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
	public void store(final List<AccessControlEntry> entries) throws ServerFault, SQLException {
		if (auditLog != null) {
			entries.forEach(e -> auditLog.logCreate(e));
		}
		aclStore.store(container, entries);
	}

	@Override
	public void add(final List<AccessControlEntry> entries) throws SQLException {
		if (auditLog != null) {
			entries.forEach(e -> auditLog.logCreate(e));
		}
		aclStore.add(container, entries);
	}

	@Override
	public List<AccessControlEntry> get() throws SQLException {
		return aclStore.get(container);
	}

	@Override
	public void deleteAll() throws SQLException {
		List<AccessControlEntry> entries = aclStore.get(container);
		if (auditLog != null) {
			entries.forEach(auditLog::logDelete);
		}
		aclStore.deleteAll(container);
	}

	@Override
	public List<AccessControlEntry> retrieveAndStore(List<AccessControlEntry> entries) throws ServerFault {
		try {
			List<AccessControlEntry> oldEntries = aclStore.retrieveAndStore(container, entries);
			List<AccessControlEntry> addedEntries = entries.stream().filter(e -> !oldEntries.contains(e))
					.collect(Collectors.toList());
			List<AccessControlEntry> removedEntries = oldEntries.stream().filter(e -> !entries.contains(e))
					.collect(Collectors.toList());
			if (auditLog != null) {
				addedEntries.forEach(e -> auditLog.logUpdate(e, null));
				removedEntries.forEach(auditLog::logDelete);
			}
			return oldEntries;
		} catch (ServerFault e) {
			throw ServerFault.sqlFault(e);

		}
	}

}
