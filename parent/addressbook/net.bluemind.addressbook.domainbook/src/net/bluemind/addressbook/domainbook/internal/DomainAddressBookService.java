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
package net.bluemind.addressbook.domainbook.internal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCardChanges;
import net.bluemind.addressbook.api.VCardChanges.ItemAdd;
import net.bluemind.addressbook.api.VCardChanges.ItemDelete;
import net.bluemind.addressbook.api.VCardChanges.ItemModify;
import net.bluemind.addressbook.domainbook.IDomainAddressBook;
import net.bluemind.addressbook.domainbook.IDomainAddressBookHook;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ContainerSyncStatus;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.persistance.ContainerSyncStore;
import net.bluemind.core.container.persistance.DataSourceRouter;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider.IServerSideServiceFactory;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class DomainAddressBookService implements IDomainAddressBook {

	public static final class Factory implements IServerSideServiceFactory<IDomainAddressBook> {

		@Override
		public Class<IDomainAddressBook> factoryClass() {
			return IDomainAddressBook.class;
		}

		@Override
		public IDomainAddressBook instance(BmContext context, String... params) throws ServerFault {
			if (params == null || params.length < 1) {
				throw new ServerFault("wrong number of instance parameters");
			}

			String domain = params[0];
			ContainerStore cs = new ContainerStore(context, context.getDataSource(), SecurityContext.SYSTEM);
			Container abContainer = null;
			try {
				abContainer = cs.get("addressbook_" + domain);
				if (abContainer == null) {
					logger.warn("no domain addressbook for domain {}", domain);
					return null;
				}

			} catch (SQLException e) {
				throw ServerFault.sqlFault(e);
			}

			return new DomainAddressBookService(context, domain, abContainer);
		}

	}

	private static final Logger logger = LoggerFactory.getLogger(DomainAddressBookService.class);
	protected static final List<IDomainAddressBookHook> hooks = getHooks();
	private BmContext context;
	private String domain;
	private Container abContainer;

	public DomainAddressBookService(BmContext context, String domain, Container abContainer) {
		this.context = context;
		this.abContainer = abContainer;
		this.domain = domain;
	}

	protected static List<IDomainAddressBookHook> getHooks() {
		RunnableExtensionLoader<IDomainAddressBookHook> loader = new RunnableExtensionLoader<IDomainAddressBookHook>();
		return loader.loadExtensions("net.bluemind.addressbook.domainbook", "hook", "hook", "impl");
	}

	@Override
	public void reset() throws ServerFault {

		ContainerSyncStore containerSyncStore = new ContainerSyncStore(DataSourceRouter.get(context, abContainer.uid),
				abContainer);

		IAddressBook domainBook = context.provider().instance(IAddressBook.class, "addressbook_" + domain);
		VCardChanges changes = new VCardChanges();
		changes.add = Collections.emptyList();
		changes.modify = Collections.emptyList();
		changes.delete = domainBook.allUids().stream().map(uid -> VCardChanges.ItemDelete.create(uid))
				.collect(Collectors.toList());

		domainBook.updates(changes);

		containerSyncStore.suspendSync();
		containerSyncStore.initSync();
		sync();
	}

	@Override
	public void sync() throws ServerFault {
		Long lastVersion = 0l;

		ContainerSyncStore containerSyncStore = new ContainerSyncStore(DataSourceRouter.get(context, abContainer.uid),
				abContainer);
		ContainerSyncStatus status = containerSyncStore.getSyncStatus();
		if (status != null && !Strings.isNullOrEmpty(status.syncToken)) {
			lastVersion = Long.parseLong(status.syncToken);
		}

		IDirectory dir = context.provider().instance(IDirectory.class, domain);
		IAddressBook domainBook = context.provider().instance(IAddressBook.class, "addressbook_" + domain);

		ContainerChangeset<String> changeset = dir.changeset(lastVersion);
		VCardChanges change = new VCardChanges();
		change.add = new ArrayList<>(changeset.created.size());
		change.modify = new ArrayList<>(changeset.updated.size());
		change.delete = new ArrayList<>(changeset.deleted.size());
		for (String c : changeset.created) {
			DirEntry dirEntry = dir.findByEntryUid(c);
			if (dirEntry == null) {
				logger.warn("no direntry for uid {}", c);
				continue;
			}

			if (dirEntry.hidden || dirEntry.system || dirEntry.archived) {
				continue;
			}

			ItemValue<VCard> vcardItem = dir.getVCard(c);
			if (vcardItem != null && vcardItem.value != null) {
				ItemAdd add = VCardChanges.ItemAdd.create(c, vcardItem.value);
				byte[] photo = dir.getEntryPhoto(c);
				if (photo == null) {
					add.photo = new byte[0];
				} else {
					add.photo = photo;
				}
				beforeAdd(add);
				logger.info("create vcard {}@{} photo {} ", c, domain, add.photo.length > 0);
				change.add.add(add);

			} else {
				logger.warn("no vcard for direntry {}@{}", c, domain);
			}
		}

		for (String c : changeset.updated) {
			DirEntry dirEntry = dir.findByEntryUid(c);
			if (dirEntry == null) {
				logger.warn("no direntry for uid {}", c);
				continue;
			}

			if (!dirEntry.system) {

				if (dirEntry.archived || dirEntry.hidden) {
					logger.debug("delete archived({})/hidden({}) user vcard {}@{}", dirEntry.archived, dirEntry.hidden,
							c, domain);
					change.delete.add(ItemDelete.create(c));
				} else {
					ItemValue<VCard> vcardItem = dir.getVCard(c);
					if (vcardItem != null && vcardItem.value != null) {
						ItemModify mod = VCardChanges.ItemModify.create(c, vcardItem.value);
						byte[] photo = dir.getEntryPhoto(c);
						if (photo == null) {
							mod.photo = new byte[0];
						} else {
							mod.photo = photo;
						}
						beforeUpdate(mod);
						change.modify.add(mod);
						logger.info("update vcard {}@{} photo {} ", c, domain, mod.photo.length > 0);

					} else {
						logger.warn("no vcard for direntry {}@{}", c, domain);
					}
				}
			}
		}

		for (String c : changeset.deleted) {
			ItemDelete del = ItemDelete.create(c);
			beforeDelete(del);
			logger.debug("delete vcard {}@{}", c, domain);
			change.delete.add(del);
		}

		domainBook.updates(change);

		ContainerSyncStatus ntatus = new ContainerSyncStatus();
		ntatus.lastSync = new Date();
		ntatus.nextSync = null;
		ntatus.syncToken = Long.toString(changeset.version);
		containerSyncStore.setSyncStatus(ntatus);
	}

	private void beforeAdd(ItemAdd add) throws ServerFault {
		hooks.forEach(h -> h.beforeAdd(add));
	}

	private void beforeUpdate(ItemModify mod) throws ServerFault {
		hooks.forEach(h -> h.beforeUpdate(mod));
	}

	private void beforeDelete(ItemDelete rm) throws ServerFault {
		hooks.forEach(h -> h.beforeDelete(rm));
	}
}
