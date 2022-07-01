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
package net.bluemind.directory.service;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.persistence.VCardStore;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.persistence.ChangelogStore.LogEntry;
import net.bluemind.core.container.persistence.IItemValueStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.ReservedIds;
import net.bluemind.directory.persistence.DirEntryStore;
import net.bluemind.directory.persistence.DirItemStore;
import net.bluemind.directory.service.internal.DirEntriesCache;
import net.bluemind.document.storage.DocumentStorage;
import net.bluemind.document.storage.IDocumentStore;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.role.hook.IRoleHook;
import net.bluemind.role.hook.RoleEvent;
import net.bluemind.role.hook.RoleHooks;
import net.bluemind.role.persistence.RoleStore;

public abstract class DirValueStoreService<T> extends BaseDirStoreService<DirEntryAndValue<T>> {

	public interface MailboxAdapter<T> {
		public Mailbox asMailbox(String domainUid, String uid, T value);
	}

	public interface DirEntryAdapter<T> {
		public DirEntry asDirEntry(String domainUid, String uid, T value);
	}

	public interface VCardAdapter<T> {
		public VCard asVCard(ItemValue<Domain> domain, String uid, T value) throws ServerFault;
	}

	protected abstract byte[] getDefaultImage();

	private DirEntryStore entryStore;
	protected RoleStore roleStore;
	protected DirEntryAdapter<T> adapter;
	protected VCardStore vcardStore;
	protected VCardAdapter<T> vcardAdapter;
	protected Kind kind;
	private IDocumentStore documentStore;
	private List<IRoleHook> roleHooks;
	private MailboxAdapter<T> mailboxAdapter;
	protected ItemValue<Domain> domain;
	private DirEntriesCache cache;
	private final OrgUnitHierarchyBackup<T> orgUnitHierarchyBackup;

	protected DirValueStoreService(BmContext context, DataSource pool, SecurityContext securityContext,
			ItemValue<Domain> domain, Container container, Kind kind, IItemValueStore<T> itemValueStore,
			DirEntryAdapter<T> adapter, VCardAdapter<T> vcardAdapter, MailboxAdapter<T> mailboxAdapter) {
		super(context, pool, securityContext, container, new DirEntryAndValueStore<>(pool, container, itemValueStore));
		this.domain = domain;
		this.kind = kind;
		this.roleStore = new RoleStore(pool, container);
		this.roleHooks = RoleHooks.get();
		this.itemStore = new DirItemStore(pool, container, securityContext, kind);

		this.entryStore = new DirEntryStore(pool, container);
		this.vcardStore = new VCardStore(pool, container);
		this.mailboxAdapter = mailboxAdapter;
		this.adapter = adapter;
		this.vcardAdapter = vcardAdapter;
		this.documentStore = DocumentStorage.store;
		this.cache = DirEntriesCache.get(context, container.domainUid);
		this.orgUnitHierarchyBackup = new OrgUnitHierarchyBackup<>(context, pool, securityContext, container);
	}

	@Override
	protected void decorate(List<Item> items, List<ItemValue<DirEntryAndValue<T>>> values) throws ServerFault {
		super.decorate(items, values);
		for (ItemValue<DirEntryAndValue<T>> v : values) {
			if (v.value.vcard != null && hasPhoto(v.uid)) {
				v.value.vcard.identification.photo = true;
			}
		}
	}

	@Override
	protected void decorate(Item item, ItemValue<DirEntryAndValue<T>> value) throws ServerFault {
		super.decorate(item, value);
		if (value.value.vcard != null && hasPhoto(value.uid)) {
			value.value.vcard.identification.photo = true;
		}
	}

	@Override
	protected void deleteValue(Item item) throws ServerFault, SQLException {
		roleStore.set(item, new HashSet<>());
		vcardStore.delete(item);
		super.deleteValue(item);
		entryStore.delete(item);
		documentStore.delete(getPhotoUid(item.uid));
		documentStore.delete(getIconUid(item.uid));
		cache.invalidate(item.uid);
	}

	public void setRoles(String uid, Set<String> roles) throws ServerFault {
		doOrFail(() -> {
			Item item = itemStore.get(uid);
			roleStore.set(item, roles);
			return null;
		});

		RoleEvent event = new RoleEvent(domain.uid, uid, kind, roles);
		roleHooks.forEach(hook -> hook.onRolesSet(event));
	}

	@Override
	protected void deleteValues() throws ServerFault {
		throw new ServerFault("Should not be called !");
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

	public void create(String uid, DirEntry dirEntry, T value) throws ServerFault {
		super.create(uid, dirEntry.displayName, new DirEntryAndValue<>(dirEntry, value,
				vcardAdapter.asVCard(domain, uid, value), asMailbox(container.domainUid, uid, value)));
	}

	private Mailbox asMailbox(String domainUid, String uid, T value) {
		if (mailboxAdapter == null) {
			return null;
		} else {
			return mailboxAdapter.asMailbox(domainUid, uid, value);
		}
	}

	public void create(String uid, DirEntry dirEntry, Mailbox mailbox, T value) throws ServerFault {
		super.create(uid, dirEntry.displayName,
				new DirEntryAndValue<>(dirEntry, value, vcardAdapter.asVCard(domain, uid, value), mailbox));
	}

	public void createWithExtId(String uid, String extId, DirEntry dirEntry, T value) throws ServerFault {
		super.create(uid, extId, dirEntry.displayName, new DirEntryAndValue<>(dirEntry, value,
				vcardAdapter.asVCard(domain, uid, value), asMailbox(container.domainUid, uid, value)));
	}

	public void create(String uid, T value) throws ServerFault {
		DirEntry dirEntry = adapter.asDirEntry(container.domainUid, uid, value);
		super.create(uid, dirEntry.displayName, new DirEntryAndValue<>(dirEntry, value,
				vcardAdapter.asVCard(domain, uid, value), asMailbox(container.domainUid, uid, value)));
	}

	public void createWithExtId(String uid, String extId, T value) throws ServerFault {
		DirEntry dirEntry = adapter.asDirEntry(container.domainUid, uid, value);
		VCard card = vcardAdapter.asVCard(domain, uid, value);
		super.create(uid, extId, dirEntry.displayName,
				new DirEntryAndValue<>(dirEntry, value, card, asMailbox(container.domainUid, uid, value)));
	}

	public void create(ItemValue<T> itemValue) throws ServerFault {
		create(itemValue, doNothingOnIdsReservation);
	}

	public void create(ItemValue<T> itemValue, ReservedIds.ConsumerHandler handler) throws ServerFault {
		T value = itemValue.value;
		DirEntry dirEntry = adapter.asDirEntry(container.domainUid, itemValue.uid, value);
		itemValue.displayName = (itemValue.displayName == null) ? dirEntry.displayName : itemValue.displayName;
		DirEntryAndValue<T> dirEntryValue = new DirEntryAndValue<>(dirEntry, value,
				vcardAdapter.asVCard(domain, itemValue.uid, value),
				asMailbox(container.domainUid, itemValue.uid, value));
		super.create(itemValue.item(), dirEntryValue, handler);
	}

	@Override
	protected void beforeCreationInBackupStore(ItemValue<DirEntryAndValue<T>> itemValue) {
		super.beforeCreationInBackupStore(itemValue);
		orgUnitHierarchyBackup.process(itemValue);
	}

	public void update(String uid, T value) throws ServerFault {
		DirEntry dirEntry = adapter.asDirEntry(container.domainUid, uid, value);
		update(uid, dirEntry.displayName, new DirEntryAndValue<>(dirEntry, value,
				vcardAdapter.asVCard(domain, uid, value), asMailbox(container.domainUid, uid, value)));
		cache.invalidate(uid);
	}

	public void update(ItemValue<T> itemValue) throws ServerFault {
		update(itemValue, doNothingOnIdsReservation);
	}

	public void update(ItemValue<T> itemValue, ReservedIds.ConsumerHandler handler) throws ServerFault {
		T value = itemValue.value;
		DirEntry dirEntry = adapter.asDirEntry(container.domainUid, itemValue.uid, value);
		DirEntryAndValue<T> dirEntryValue = new DirEntryAndValue<>(dirEntry, value,
				vcardAdapter.asVCard(domain, itemValue.uid, value),
				asMailbox(container.domainUid, itemValue.uid, value));
		update(itemValue.item(), dirEntry.displayName, dirEntryValue, reservedIdsConsumer -> {
			cache.invalidate(itemValue.uid);
			handler.acceptConsumer(reservedIdsConsumer);
		});
	}

	public ItemValue<T> get(String uid) throws ServerFault {
		ItemValue<DirEntryAndValue<T>> item = get(uid, null);
		if (item == null) {
			return null;
		} else {
			return ItemValue.create(item, item.value.value);
		}
	}

	public ItemValue<T> findByExtId(String extId) throws ServerFault {
		ItemValue<DirEntryAndValue<T>> item = getByExtId(extId);
		if (item == null) {
			return null;
		} else {
			return ItemValue.create(item, item.value.value);
		}
	}

	public ItemValue<DirEntryAndValue<T>> findByEmailFull(String email) throws ServerFault {
		return doOrFail(() -> {
			Item ret = ((DirItemStore) itemStore).getByEmail(email.toLowerCase());
			if (ret == null) {
				return null;
			} else {
				return getItemValue(ret);
			}
		});
	}

	public ItemValue<T> findByEmail(String email) throws ServerFault {
		ItemValue<DirEntryAndValue<T>> item = findByEmailFull(email);
		if (item == null) {
			return null;
		} else {
			return ItemValue.create(item, item.value.value);
		}
	}

	public List<ItemValue<T>> getMultipleValues(List<String> uids) throws ServerFault {
		return getMultiple(uids).stream() //
				.map(item -> ItemValue.create(item, item.value.value)) //
				.collect(Collectors.toList());
	}

	public void updateVCard(String uid, T dirEntry) throws ServerFault {
		doOrFail(() -> {
			Item item = itemStore.getForUpdate(uid);
			if (item == null) {
				throw ServerFault.notFound("entry[" + uid + "]@" + container.uid + " not found");
			}

			item = itemStore.update(uid, item.displayName);
			vcardStore.update(item, vcardAdapter.asVCard(domain, uid, dirEntry));

			if (hasChangeLog) {
				changelogStore.itemUpdated(LogEntry.create(item.version, item.uid, item.externalId,
						securityContext.getSubject(), securityContext.getOrigin(), item.id, 0));
			}
			return null;
		});
	}

	public void deletePhoto(String uid) throws ServerFault {
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

			documentStore.delete(getPhotoUid(uid));
			documentStore.delete(getIconUid(uid));

			return null;
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
		byte[] photo = documentStore.get(getPhotoUid(uid));
		return null != photo ? photo : getDefaultImage();
	}

	public boolean hasPhoto(String uid) throws ServerFault {
		try {
			return documentStore.exists(getPhotoUid(uid));
		} catch (ServerFault e) {
			if (e.getCode() == ErrorCode.TIMEOUT) {
				logger.warn("Check for photo of {} failed", uid, e);
				return false;
			}
			throw e;
		}
	}

	public byte[] getIcon(String uid) throws ServerFault {
		byte[] icon = documentStore.get(getIconUid(uid));
		return null != icon ? icon : getDefaultImage();
	}

	private String getPhotoUid(String uid) {
		return "dir_" + container.uid + "/photos/" + uid;
	}

	private String getIconUid(String uid) {
		return "dir_" + container.uid + "/icons/" + uid;
	}

	@Override
	public ItemVersion delete(String uid) throws ServerFault {
		ItemVersion deleted = super.delete(uid);
		cache.invalidate(uid);
		return deleted;
	}

	@Override
	public void deleteAll() throws ServerFault {
		super.deleteAll();
		cache.invalidateAll();
	}

}
