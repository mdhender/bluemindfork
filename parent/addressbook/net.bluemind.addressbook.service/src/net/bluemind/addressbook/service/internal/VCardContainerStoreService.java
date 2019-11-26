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
package net.bluemind.addressbook.service.internal;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Kind;
import net.bluemind.addressbook.persistence.VCardIndexStore;
import net.bluemind.addressbook.persistence.VCardStore;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.IItemValueStore;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.document.storage.DocumentStorage;
import net.bluemind.document.storage.IDocumentStore;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.tag.api.TagRef;
import net.bluemind.tag.service.IInCoreTagRef;

public class VCardContainerStoreService extends ContainerStoreService<VCard> {

	private IDocumentStore documentStore;
	private IInCoreTagRef tagRefService;
	private VCardIndexStore indexStore;

	public VCardContainerStoreService(BmContext context, DataSource dataSource, SecurityContext securityContext,
			Container container) {
		this(context, dataSource, securityContext, container, IAddressBookUids.TYPE,
				new VCardStore(dataSource, container), new VCardIndexStore(ESearchActivator.getClient(), container));
	}

	public VCardContainerStoreService(BmContext context, DataSource dataSource, SecurityContext securityContext,
			Container container, String itemType, IItemValueStore<VCard> itemValueStore, VCardIndexStore indexStore) {
		super(dataSource, securityContext, container, itemType, itemValueStore, card -> ItemFlag.SEEN, c -> 0L, s -> s);
		tagRefService = context.su().provider().instance(IInCoreTagRef.class, container.uid);
		documentStore = DocumentStorage.store;
		this.indexStore = indexStore;
	}

	@Override
	protected void decorate(List<Item> items, List<ItemValue<VCard>> values) throws ServerFault {
		List<List<TagRef>> refs = tagRefService.getMultiple(items);

		Iterator<Item> itItems = items.iterator();
		Iterator<ItemValue<VCard>> itValues = values.iterator();
		Iterator<List<TagRef>> itRefs = refs.iterator();
		for (; itItems.hasNext();) {
			Item item = itItems.next();
			ItemValue<VCard> value = itValues.next();
			List<TagRef> ref = itRefs.next();
			if (ref != null) {
				value.value.explanatory.categories = ref;
			}
			value.value.identification.photo = hasPhoto(item.uid);
		}

	}

	@Override
	protected void decorate(Item item, ItemValue<VCard> value) throws ServerFault {
		if (value.value == null) {
			return;
		}

		List<TagRef> tags = tagRefService.get(item);
		value.value.explanatory.categories = tags;
		value.value.identification.photo = hasPhoto(item.uid);

	}

	@Override
	protected void createValue(Item item, VCard value) throws ServerFault, SQLException {
		super.createValue(item, value);
		List<TagRef> tags = value.explanatory.categories;
		if (tags == null) {
			tags = Collections.emptyList();
		}
		tagRefService.create(item, tags);

		indexStore.create(item.uid, value);

	}

	@Override
	protected void updateValue(Item item, VCard value) throws ServerFault, SQLException {
		super.updateValue(item, value);
		List<TagRef> tags = value.explanatory.categories;
		if (tags == null) {
			tags = Collections.emptyList();
		}
		tagRefService.update(item, tags);

		indexStore.update(item.uid, value);
	}

	@Override
	protected void deleteValue(Item item) throws ServerFault, SQLException {
		super.deleteValue(item);
		tagRefService.delete(item);
		if (documentStore.exists(getPhotoUid(item.uid))) {
			documentStore.delete(getPhotoUid(item.uid));
		}

		if (documentStore.exists(getIconUid(item.uid))) {
			documentStore.delete(getIconUid(item.uid));
		}
		indexStore.delete(item.uid);
	}

	private String getPhotoUid(String uid) {
		return "book_" + container.uid + "/photos/" + uid;
	}

	private String getIconUid(String uid) {
		return "book_" + container.uid + "/icons/" + uid;
	}

	@Override
	protected void deleteValues() throws ServerFault {
		super.deleteValues();
		tagRefService.deleteAll();
		indexStore.deleteAll();
	}

	public void setPhoto(String uid, byte[] photo) throws ServerFault {
		documentStore.store(getPhotoUid(uid), photo);
	}

	public void deletePhoto(String uid) throws ServerFault {
		documentStore.delete(getPhotoUid(uid));
	}

	public void deleteIcon(String uid) throws ServerFault {
		documentStore.delete(getIconUid(uid));
	}

	public void setIcon(String uid, byte[] photo) throws ServerFault {
		documentStore.store(getIconUid(uid), photo);
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

	public List<String> findByEmail(String email) throws ServerFault {
		try {
			return ((VCardStore) itemValueStore).findByEmail(email);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public List<String> findGroupsContaining(String[] uid) {
		try {
			return ((VCardStore) itemValueStore).findGroupsContaining(uid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public List<String> findGroups() {
		try {
			return ((VCardStore) itemValueStore).findByKind(Kind.group);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

}
