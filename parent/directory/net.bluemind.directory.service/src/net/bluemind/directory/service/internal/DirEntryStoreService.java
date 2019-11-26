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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.persistence.VCardStore;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.persistence.ChangelogStore.LogEntry;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.BaseDirEntry.AccountType;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.persistence.DirEntryStore;
import net.bluemind.directory.persistence.ManageableOrgUnit;
import net.bluemind.directory.persistence.OrgUnitStore;
import net.bluemind.directory.service.BaseDirStoreService;
import net.bluemind.document.storage.DocumentStorage;
import net.bluemind.document.storage.IDocumentStore;

public class DirEntryStoreService extends BaseDirStoreService<DirEntry> {

	private static final Logger logger = LoggerFactory.getLogger(DirEntryStoreService.class);
	private DirEntryStore entryStore;
	private VCardStore vcardStore;
	private IDocumentStore documentStore;
	private OrgUnitStore ouStore;
	private final String domainUid;
	private DirEntriesCache cache;

	public DirEntryStoreService(BmContext context, Container container, String domainUid) {
		super(context, context.getDataSource(), context.getSecurityContext(), container, "dir",
				new DirEntryStore(context.getDataSource(), container));
		this.ouStore = new OrgUnitStore(context.getDataSource(), container);
		this.entryStore = new DirEntryStore(context.getDataSource(), container);
		this.vcardStore = new VCardStore(context.getDataSource(), container);
		this.domainUid = domainUid;
		documentStore = DocumentStorage.store;
		this.cache = DirEntriesCache.get(context, domainUid);
	}

	public ListResult<ItemValue<DirEntry>> searchManageable(DirEntryQuery query, List<ManageableOrgUnit> manageable) {
		ListResult<Item> items;
		try {
			items = entryStore.searchManageable(query, manageable);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		ListResult<ItemValue<DirEntry>> ret = new ListResult<>();
		ret.total = items.total;
		ret.values = getItemsValue(items.values);
		return ret;
	}

	public ListResult<ItemValue<DirEntry>> search(DirEntryQuery query) throws ServerFault {

		ListResult<Item> items;
		try {
			items = entryStore.search(query);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		ListResult<ItemValue<DirEntry>> ret = new ListResult<>();
		ret.total = items.total;
		ret.values = getItemsValue(items.values);
		return ret;
	}

	public List<ItemValue<DirEntry>> getEntries(String path) throws ServerFault {
		List<String> uids = null;
		try {
			uids = entryStore.path(path);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		return getMultiple(uids);
	}

	public ItemValue<VCard> getVCard(String uid) throws ServerFault {
		return doOrFail(() -> {
			Item item = itemStore.get(uid);
			if (item == null) {
				return null;
			}
			return ItemValue.create(item, vcardStore.get(item));
		});
	}

	public void setPhoto(String uid, byte[] photo, byte[] icon) throws ServerFault {
		doOrFail(() -> {
			Item item = itemStore.getForUpdate(uid);
			if (item == null) {
				throw ServerFault.notFound("entry[" + uid + "]@" + container.uid + " not found");
			}

			item = itemStore.update(uid, item.displayName);
			if (hasChangeLog) {
				changelogStore.itemUpdated(LogEntry.create(item.version, item.uid, item.externalId,
						securityContext.getSubject(), securityContext.getOrigin(), item.id, 0));
			}
			documentStore.store(getPhotoUid(uid), photo);
			documentStore.store(getIconUid(uid), icon);
			return null;
		});
	}

	public byte[] getPhoto(String uid) throws ServerFault {
		return documentStore.get(getPhotoUid(uid));
	}

	public boolean hasPhoto(String uid) throws ServerFault {
		return documentStore.exists(getPhotoUid(uid));
	}

	public byte[] getIcon(String uid) throws ServerFault {
		return documentStore.get(getIconUid(uid));
	}

	private String getPhotoUid(String uid) {
		return "dir_" + container.uid + "/photos/" + uid;
	}

	private String getIconUid(String uid) {
		return "dir_" + container.uid + "/icons/" + uid;
	}

	@Override
	protected void decorate(Item item, ItemValue<DirEntry> value) throws ServerFault {
		if (value.value == null) {
			logger.warn("no direntry for {}!!", item.uid);
			return;
		}
		if (value.value.orgUnitUid != null) {
			try {
				value.value.orgUnitPath = ouStore.getPathByUid(value.value.orgUnitUid);
			} catch (SQLException e) {
				throw ServerFault.sqlFault(e);
			}
		}
	}

	@Override
	public ItemVersion update(String uid, String displayName, DirEntry value) throws ServerFault {
		cache.invalidate(uid);
		return super.update(uid, displayName, value);
	}

	@Override
	public ItemVersion delete(String uid) throws ServerFault {
		cache.invalidate(uid);
		return super.delete(uid);
	}

	@Override
	public void deleteAll() throws ServerFault {
		cache.invalidateAll();
		super.deleteAll();
	}

	public ItemValue<DirEntry> getByEmail(String email, boolean isDomainEmail) {
		return doOrFail(() -> {
			String res = entryStore.byEmail(email, isDomainEmail);
			if (res != null) {
				return get(res, null);
			} else {
				return null;
			}
		});
	}

	public void updateAccountType(String uid, AccountType accountType) throws ServerFault {
		try {
			Item item = itemStore.get(uid);
			if (item != null) {
				entryStore.updateAccountType(item, accountType);
				cache.invalidate(uid);
			}
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

}
