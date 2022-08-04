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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.DirEntryPermission;
import net.bluemind.core.container.service.internal.Permission;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.core.validator.Validator;
import net.bluemind.directory.api.BaseDirEntry;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.IOrgUnits;
import net.bluemind.directory.api.OrgUnit;
import net.bluemind.directory.api.OrgUnitPath;
import net.bluemind.directory.api.OrgUnitQuery;
import net.bluemind.directory.persistence.ManageableOrgUnit;
import net.bluemind.directory.service.DirEventProducer;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.role.api.BasicRoles;

public class OrgUnits implements IOrgUnits {

	private OrgUnitContainerStoreService storeService;
	private RBACManager rbacManager;
	private Validator validator;
	private Sanitizer sanitizer;
	private DirEventProducer dirEventProducer;
	private BmContext context;
	private ItemValue<Domain> domain;

	public OrgUnits(BmContext context, ItemValue<Domain> domain, Container container) {
		this.context = context;
		this.domain = domain;
		this.storeService = new OrgUnitContainerStoreService(context, container, domain);
		rbacManager = new RBACManager(context).forContainer(container);
		sanitizer = new Sanitizer(context);
		validator = new Validator(context);
		dirEventProducer = new DirEventProducer(domain.uid, BaseDirEntry.Kind.ORG_UNIT.name(),
				VertxPlatform.eventBus());
	}

	@Override
	public ItemValue<OrgUnit> getComplete(String uid) {
		rbacManager.check(Verb.Read.name());
		return storeService.get(uid);
	}

	@Override
	public void create(String uid, OrgUnit value) {
		ItemValue<OrgUnit> orgUnitItem = ItemValue.create(uid, value);
		createWithItem(orgUnitItem);
	}

	private void createWithItem(ItemValue<OrgUnit> orgUnitItem) {
		String uid = orgUnitItem.uid;
		OrgUnit value = orgUnitItem.value;
		if (value.parentUid != null) {
			rbacManager.forOrgUnit(value.parentUid).check(BasicRoles.ROLE_MANAGE_OU);
		} else {
			rbacManager.check(BasicRoles.ROLE_MANAGE_OU);
		}

		if (value.parentUid != null && storeService.get(value.parentUid) == null) {
			throw new ServerFault("ou " + value.parentUid + " not found", ErrorCode.NOT_FOUND);
		}

		if (storeService.pathExists(value.name, value.parentUid)) {
			throw new ServerFault("ou " + value.parentUid + "/ " + value.name + " already exists",
					ErrorCode.ALREADY_EXISTS);

		}

		sanitizer.create(value);
		validator.create(value);

		storeService.create(orgUnitItem);
		dirEventProducer.changed(uid, storeService.getVersion());

	}

	@Override
	public void update(String uid, OrgUnit value) {
		ItemValue<OrgUnit> orgUnitItem = ItemValue.create(uid, value);
		updateWithItem(orgUnitItem);
	}

	private void updateWithItem(ItemValue<OrgUnit> orgUnitItem) {
		String uid = orgUnitItem.uid;
		OrgUnit value = orgUnitItem.value;
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_OU);
		ItemValue<OrgUnit> previous = storeService.get(uid);
		if (previous == null) {
			throw new ServerFault("ou " + uid + " not found", ErrorCode.NOT_FOUND);
		}
		if (!previous.value.name.equalsIgnoreCase(value.name) && storeService.pathExists(value.name, value.parentUid)) {
			throw new ServerFault("ou " + value.parentUid + "/ " + value.name + " already exists",
					ErrorCode.ALREADY_EXISTS);
		}

		sanitizer.update(previous.value, value);
		validator.update(previous.value, value);

		if (!StringUtils.equals(previous.value.parentUid, value.parentUid))

		{
			throw new ServerFault("Parent change is not allowed", ErrorCode.INVALID_PARAMETER);
		}
		storeService.update(orgUnitItem);
		dirEventProducer.changed(uid, storeService.getVersion());
	}

	@Override
	public void delete(String uid) {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_OU);
		ItemValue<OrgUnit> previous = storeService.get(uid);
		if (previous == null) {
			throw new ServerFault("ou " + uid + " not found", ErrorCode.NOT_FOUND);
		}

		if (storeService.hasChildren(uid)) {
			throw new ServerFault("ou " + previous.value.name + " has children", ErrorCode.INVALID_PARAMETER);
		}

		if (storeService.hasMembers(uid)) {
			throw new ServerFault("ou " + previous.value.name + " has members", ErrorCode.INVALID_PARAMETER);
		}
		if (storeService.hasAdministrator(uid)) {
			throw new ServerFault("ou " + previous.value.name + " has administrators", ErrorCode.INVALID_PARAMETER);
		}

		storeService.delete(uid);
		dirEventProducer.changed(uid, storeService.getVersion());
	}

	@Override
	public OrgUnitPath getPath(String uid) {
		rbacManager.check(Verb.Read.name());
		return storeService.getPath(uid);
	}

	@Override
	public List<ItemValue<OrgUnit>> getChildren(String uid) {
		return storeService.getChildren(uid);
	}

	@Override
	public List<OrgUnitPath> search(OrgUnitQuery query) {
		rbacManager.check(Verb.Read.name());
		if (query.managableKinds != null && !query.managableKinds.isEmpty()) {
			List<ManageableOrgUnit> manageableParentOu = getManageableDirEntries(query.managableKinds);
			if (manageableParentOu.stream().anyMatch(mp -> mp.ou == null)) {
				return storeService.search(query, null);
			} else {
				return storeService.search(query,
						manageableParentOu.stream().map(mp -> mp.ou).collect(Collectors.toList()));
			}
		} else {
			return storeService.search(query, null);
		}
	}

	@Override
	public void setAdministratorRoles(String uid, String dirUid, Set<String> roles) {
		ItemValue<OrgUnit> ou = storeService.get(uid);
		if (ou == null) {
			throw new ServerFault("ou " + uid + " not found", ErrorCode.NOT_FOUND);
		}
		Set<String> currentDirEntryRoles = storeService.getAdministratorRoles(uid, dirUid);

		Set<String> rolesToCheck = new HashSet<>(roles);
		rolesToCheck.removeAll(currentDirEntryRoles);
		if (!rbacManager.forOrgUnit(uid).canAll(rolesToCheck)) {
			throw new ServerFault("not enough roles for setting roles " + rolesToCheck, ErrorCode.PERMISSION_DENIED);
		}

		storeService.setAdministratorRoles(uid, dirUid, roles);
	}

	@Override
	public Set<String> getAdministratorRoles(String uid, String dirUid, List<String> groups) {
		if (!rbacManager.forEntry(uid).can(BasicRoles.ROLE_MANAGE_OU)
				&& !rbacManager.forEntry(dirUid).can(BasicRoles.ROLE_SELF, BasicRoles.ROLE_MANAGER)) {
			throw new ServerFault("Doesnt have roles to access adminstrator " + dirUid + " of OrgUnit " + uid,
					ErrorCode.PERMISSION_DENIED);
		}

		ItemValue<OrgUnit> ou = storeService.get(uid);
		if (ou == null) {
			throw new ServerFault("ou " + uid + " not found", ErrorCode.NOT_FOUND);
		}

		return storeService.getAdministratorRoles(uid, dirUid, groups);
	}

	@Override
	public Set<String> getAdministrators(String uid) {
		rbacManager.forEntry(uid).check(BasicRoles.ROLE_MANAGE_OU);

		ItemValue<OrgUnit> ou = storeService.get(uid);
		if (ou == null) {
			throw new ServerFault("ou " + uid + " not found", ErrorCode.NOT_FOUND);
		}
		return storeService.getAdministrators(uid);
	}

	@Override
	public List<OrgUnitPath> listByAdministrator(String administrator, List<String> groups) {
		// FIXME not sure about which role to check here
		rbacManager.forEntry(administrator).check(BasicRoles.ROLE_MANAGER, Verb.Read.name(), BasicRoles.ROLE_SELF);

		return storeService.listByAdministrator(administrator, groups);
	}

	private List<ManageableOrgUnit> getManageableDirEntries(Set<Kind> mkinds) {
		RBACManager rbacManager = RBACManager.forContext(context).forDomain(domain.uid);
		List<ManageableOrgUnit> ret = new ArrayList<>();
		for (Map.Entry<String, Set<String>> ouEntry : context.getSecurityContext().getRolesByOrgUnits().entrySet()) {
			Set<Permission> perms = rbacManager.forOrgUnit(ouEntry.getKey()).resolve();
			Set<Kind> kinds = perms.stream().filter((perm) -> perm instanceof DirEntryPermission)
					.map((perm) -> ((DirEntryPermission) perm).getKind()).collect(Collectors.toSet());
			for (Kind kind : mkinds) {
				if (kinds.contains(kind)) {
					ret.add(new ManageableOrgUnit(ouEntry.getKey(), kinds));
				}
			}
		}

		Set<Permission> perms = RBACManager.forContext(context).forDomain(domain.uid).resolve();
		Set<Kind> kinds = perms.stream().filter((perm) -> perm instanceof DirEntryPermission)
				.map((perm) -> ((DirEntryPermission) perm).getKind()).collect(Collectors.toSet());
		for (Kind kind : mkinds) {
			if (kinds.contains(kind)) {
				ret.add(new ManageableOrgUnit(null, kinds));
			}
		}

		return ret;
	}

	@Override
	public void removeAdministrator(String administrator) {
		RBACManager.forContext(context).can(BasicRoles.ROLE_ADMIN);

		storeService.removeAdministrator(administrator);
	}

	@Override
	public OrgUnit get(String uid) {
		ItemValue<OrgUnit> item = getComplete(uid);
		return item != null ? item.value : null;
	}

	@Override
	public void restore(ItemValue<OrgUnit> item, boolean isCreate) {
		if (isCreate) {
			createWithItem(item);
		} else {
			updateWithItem(item);
		}
	}

}
