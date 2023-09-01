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

import javax.sql.DataSource;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlogs.ILogMapperProvider;
import net.bluemind.core.container.api.internal.IAccessControlList;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ChangeLogEntry.Type;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;

public class AclService implements IAccessControlList {

	private final AclStore aclStore;
	private AuditLogService<AccessControlEntry> auditLog;
	Container container;

	public AclService(BmContext ctx, SecurityContext sc, DataSource pool, BaseContainerDescriptor desc) {
		container = Container.create(desc.uid, desc.type, desc.name, desc.owner, desc.domainUid, desc.defaultContainer);
		container.id = desc.internalId;
		aclStore = new AclStore(ctx, pool);
		ILogMapperProvider<AccessControlEntry> mapper = new AccessControlEntryAuditLogMapper();
		auditLog = new AuditLogService<>(sc, desc, mapper);
	}

	public AclService(BmContext ctx, SecurityContext sc, DataSource pool, Container cont) {
		BaseContainerDescriptor desc = BaseContainerDescriptor.create(cont.uid, cont.name, cont.owner, cont.type,
				cont.domainUid, cont.defaultContainer);
		desc.internalId = cont.id;
		container = cont;
		aclStore = new AclStore(ctx, pool);
		ILogMapperProvider<AccessControlEntry> mapper = new AccessControlEntryAuditLogMapper();
		auditLog = new AuditLogService<>(sc, desc, mapper);
	}

	@Override
	public void store(final List<AccessControlEntry> entries) throws ServerFault, SQLException {
		if (auditLog != null) {
			entries.forEach(e -> auditLog.log(null, null, e, Type.Created));
		}
		aclStore.store(container, entries);
	}

	@Override
	public void add(final List<AccessControlEntry> entries) throws SQLException {
		if (auditLog != null) {
			entries.forEach(e -> auditLog.log(null, null, e, Type.Created));
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
			entries.forEach(e -> auditLog.log(null, null, e, Type.Deleted));
		}
		aclStore.deleteAll(container);
	}

	@Override
	public List<AccessControlEntry> retrieveAndStore(List<AccessControlEntry> entries) throws ServerFault {
		return aclStore.retrieveAndStore(container, entries);
	}

}
