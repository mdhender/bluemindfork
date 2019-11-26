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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.OrgUnit;
import net.bluemind.directory.api.OrgUnitPath;
import net.bluemind.directory.api.OrgUnitQuery;
import net.bluemind.directory.persistence.OrgUnitStore;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.directory.service.DirValueStoreService;
import net.bluemind.directory.service.NullMailboxAdapter;
import net.bluemind.directory.service.NullVCardAdapter;
import net.bluemind.domain.api.Domain;

public class OrgUnitContainerStoreService extends DirValueStoreService<OrgUnit> {

	public static class OUDirEntryAdapter implements DirEntryAdapter<OrgUnit> {

		@Override
		public DirEntry asDirEntry(String domainUid, String uid, OrgUnit ou) {
			return DirEntry.create(ou.parentUid, domainUid + "/ou/" + uid, DirEntry.Kind.ORG_UNIT, uid, ou.name, null,
					false, false, false);

		}
	}

	private OrgUnitStore orgUnitStore;
	private ItemStore genericItemStore;

	public OrgUnitContainerStoreService(BmContext context, Container container, ItemValue<Domain> domain) {
		super(context, context.getDataSource(), context.getSecurityContext(), domain, container, "dir", Kind.ORG_UNIT,
				new OrgUnitStore(context.getDataSource(), container), new OUDirEntryAdapter(), new NullVCardAdapter<>(),
				new NullMailboxAdapter<>());
		this.orgUnitStore = new OrgUnitStore(context.getDataSource(), container);
		this.genericItemStore = new ItemStore(context.getDataSource(), container, context.getSecurityContext());
	}

	@Override
	protected byte[] getDefaultImage() {
		return null;
	}

	public OrgUnitPath getPath(String uid) {
		return doOrFail(() -> {
			Item item = itemStore.get(uid);
			if (item == null) {
				return null;
			} else {
				return orgUnitStore.getPath(item);
			}
		});
	}

	@Override
	protected void decorate(Item item, ItemValue<DirEntryAndValue<OrgUnit>> value) throws ServerFault {
		super.decorate(item, value);
		value.value.entry.orgUnitPath = getPath(item.uid);
	}

	public List<OrgUnitPath> search(OrgUnitQuery query, List<String> limitToOu) {
		return doOrFail(() -> {
			List<String> res = orgUnitStore.search(query.query, limitToOu);
			if (query.size != -1) {
				res = res.subList(query.from, Math.min(query.from + query.size, res.size() - query.from));
			} else {
				res = res.subList(query.from, res.size() - query.from);
			}
			return itemStore.getMultiple(res).stream().map(item -> doOrFail(() -> orgUnitStore.getPath(item)))
					.collect(Collectors.toList());
		});
	}

	public boolean pathExists(String name, String parent) {
		return doOrFail(() -> {
			return orgUnitStore.pathExists(name, parent);
		});
	}

	public void setAdministratorRoles(String uid, String dirUid, Set<String> roles) {
		doOrFail(() -> {
			Item ouItem = itemStore.get(uid);
			Item adminItem = genericItemStore.get(dirUid);

			if (ouItem == null) {
				throw new ServerFault("ou not found ", ErrorCode.NOT_FOUND);
			}

			if (adminItem == null) {
				throw new ServerFault("direntry not found ", ErrorCode.NOT_FOUND);
			}

			orgUnitStore.setAdminRoles(ouItem, adminItem, roles);
			return null;
		});

	}

	public Set<String> getAdministratorRoles(String uid, String dirUid) {
		return getAdministratorRoles(uid, dirUid, Collections.emptyList());
	}

	public Set<String> getAdministratorRoles(String uid, String dirUid, List<String> groups) {
		return doOrFail(() -> {
			Item ouItem = itemStore.get(uid);
			List<String> uids = new ArrayList<>(groups);
			uids.add(dirUid);
			List<Item> adminItems = genericItemStore.getMultiple(uids);

			if (ouItem == null) {
				throw new ServerFault("ou not found ", ErrorCode.NOT_FOUND);
			}

			if (adminItems.isEmpty()) {
				throw new ServerFault("direntry not found ", ErrorCode.NOT_FOUND);
			}

			return orgUnitStore.getAdminRoles(ouItem, adminItems);
		});
	}

	public Set<String> getAdministrators(String uid) {
		return doOrFail(() -> {
			Item ouItem = itemStore.get(uid);

			if (ouItem == null) {
				throw new ServerFault("ou not found ", ErrorCode.NOT_FOUND);
			}

			return orgUnitStore.getAdministrators(ouItem);
		});
	}

	public List<OrgUnitPath> listByAdministrator(String administrator, List<String> groups) {
		return doOrFail(() -> {
			List<String> administrators = new ArrayList<>(groups);
			administrators.add(administrator);
			List<Item> items = genericItemStore.getMultiple(administrators);

			if (items.isEmpty()) {
				throw new ServerFault("direntry not found ", ErrorCode.NOT_FOUND);
			}

			List<String> res = orgUnitStore.listByAdministrator(items);
			return itemStore.getMultiple(res).stream().map(item -> doOrFail(() -> orgUnitStore.getPath(item)))
					.collect(Collectors.toList());
		});
	}

	public boolean hasChildren(String ouUid) {
		return doOrFail(() -> {
			Item adminItem = genericItemStore.get(ouUid);

			if (adminItem == null) {
				throw new ServerFault("direntry not found ", ErrorCode.NOT_FOUND);
			}
			return orgUnitStore.hasChilds(adminItem);
		});

	}

	public List<ItemValue<OrgUnit>> getChildren(String ouUid) {
		return doOrFail(() -> {
			Item adminItem = genericItemStore.get(ouUid);

			if (adminItem == null) {
				throw new ServerFault("direntry not found ", ErrorCode.NOT_FOUND);
			}
			return getMultiple(orgUnitStore.getChildren(adminItem)).stream().map(dir -> {
				return ItemValue.create(dir, dir.value.value);
			}).collect(Collectors.toList());
		});

	}

	public boolean hasAdministrator(String ouUid) {
		return doOrFail(() -> {
			Item adminItem = genericItemStore.get(ouUid);

			if (adminItem == null) {
				throw new ServerFault("direntry not found ", ErrorCode.NOT_FOUND);
			}
			return orgUnitStore.hasAdministrator(adminItem);
		});
	}

	public boolean hasMembers(String ouUid) {
		return doOrFail(() -> {
			Item adminItem = genericItemStore.get(ouUid);

			if (adminItem == null) {
				throw new ServerFault("direntry not found ", ErrorCode.NOT_FOUND);
			}
			return orgUnitStore.hasMembers(adminItem);
		});
	}

	public void removeAdministrator(String administrator) {
		doOrFail(() -> {
			orgUnitStore.removeAdministrator(administrator);
			return null;
		});

	}

}
