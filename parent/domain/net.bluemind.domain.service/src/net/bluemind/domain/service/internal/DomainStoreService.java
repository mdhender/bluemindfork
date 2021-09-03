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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.directory.api.BaseDirEntry;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.persistence.DomainSettingsStore;
import net.bluemind.domain.persistence.DomainStore;
import net.bluemind.role.hook.IRoleHook;
import net.bluemind.role.hook.RoleEvent;
import net.bluemind.role.hook.RoleHooks;
import net.bluemind.role.persistence.RoleStore;

public class DomainStoreService extends ContainerStoreService<Domain> {

	private DomainSettingsStore domainSettingsStore;
	private DomainStore domainStore;
	private RoleStore roleStore;
	private List<IRoleHook> roleHooks;

	public DomainStoreService(DataSource pool, SecurityContext securityContext, Container container) {
		super(pool, securityContext, container, new DomainStore(pool));
		domainStore = new DomainStore(pool);
		domainSettingsStore = new DomainSettingsStore(pool, container);
		roleStore = new RoleStore(pool, container);
		roleHooks = RoleHooks.get();
	}

	@Override
	protected void deleteValue(Item item) throws ServerFault, SQLException {
		super.deleteValue(item);
		try {
			roleStore.set(item, new HashSet<>());
			domainSettingsStore.delete(item);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	protected void deleteValues() throws ServerFault {
		super.deleteValues();
		try {
			domainSettingsStore.deleteAll();
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public ItemValue<Domain> findByNameOrAliases(String name) throws ServerFault {
		String uid = null;
		try {
			uid = domainStore.findByNameOrAliases(name);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		if (uid == null) {
			return null;
		} else {
			return get(uid, null);
		}
	}

	public Set<String> getRoles(String uid) throws ServerFault {
		try {
			Item item = itemStore.get(uid);
			if (item != null) {
				return roleStore.get(item);
			} else {
				return Collections.emptySet();
			}
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public void setRoles(String uid, Set<String> roles) throws ServerFault {
		doOrFail(() -> {
			Item item = itemStore.get(uid);
			roleStore.set(item, roles);
			return null;
		});

		RoleEvent event = new RoleEvent(uid, uid, BaseDirEntry.Kind.DOMAIN, roles);
		roleHooks.forEach(hook -> hook.onRolesSet(event));

	}

}
