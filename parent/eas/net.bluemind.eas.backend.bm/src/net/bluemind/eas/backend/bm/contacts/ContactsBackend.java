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
package net.bluemind.eas.backend.bm.contacts;

import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Kind;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.Changes;
import net.bluemind.eas.backend.HierarchyNode;
import net.bluemind.eas.backend.IApplicationData;
import net.bluemind.eas.backend.ItemChangeReference;
import net.bluemind.eas.backend.MSContact;
import net.bluemind.eas.backend.bm.compat.OldFormats;
import net.bluemind.eas.backend.bm.impl.CoreConnect;
import net.bluemind.eas.dto.base.AirSyncBaseResponse;
import net.bluemind.eas.dto.base.AppData;
import net.bluemind.eas.dto.base.BodyType;
import net.bluemind.eas.dto.base.ChangeType;
import net.bluemind.eas.dto.base.CollectionItem;
import net.bluemind.eas.dto.base.DisposableByteSource;
import net.bluemind.eas.dto.base.LazyLoaded;
import net.bluemind.eas.dto.contact.ContactResponse;
import net.bluemind.eas.dto.sync.CollectionId;
import net.bluemind.eas.dto.sync.CollectionSyncRequest.Options.ConflicResolution;
import net.bluemind.eas.dto.sync.SyncState;
import net.bluemind.eas.dto.type.ItemDataType;
import net.bluemind.eas.exception.ActiveSyncException;
import net.bluemind.eas.store.ISyncStorage;
import net.bluemind.eas.utils.EasLogUser;

/**
 * Contacts backend implementation
 * 
 * 
 */
public class ContactsBackend extends CoreConnect {

	private final ISyncStorage storage;

	public ContactsBackend(ISyncStorage storage) {
		this.storage = storage;
	}

	public Changes getContentChanges(BackendSession bs, long version, CollectionId collectionId) {
		Changes changes = new Changes();

		try {
			HierarchyNode folder = storage.getHierarchyNode(bs, collectionId);
			IAddressBook service = getAddressbookService(bs, folder.containerUid);

			ContainerChangeset<Long> changeset = service.changesetById(version);
			EasLogUser.logDebugAsUser(bs.getLoginAtDomain(), logger,
					"[{}][{}] get contacts changes. created: {}, updated: {}, deleted: {}, folder: {}, version: {}",
					bs.getLoginAtDomain(), bs.getDevId(), changeset.created.size(), changeset.updated.size(),
					changeset.deleted.size(), folder.containerUid, version);

			changes.version = changeset.version;

			for (long id : changeset.created) {
				changes.items.add(getItemChange(collectionId, id, ItemDataType.CONTACTS, ChangeType.ADD));
			}

			for (long id : changeset.updated) {
				changes.items.add(getItemChange(collectionId, id, ItemDataType.CONTACTS, ChangeType.CHANGE));
			}

			for (long id : changeset.deleted) {
				ItemChangeReference ic = getItemChange(collectionId, id, ItemDataType.CONTACTS, ChangeType.DELETE);
				changes.items.add(ic);
			}

		} catch (ServerFault e) {
			if (e.getCode() == ErrorCode.PERMISSION_DENIED) {
				EasLogUser.logWarnAsUser(bs.getLoginAtDomain(), logger, e.getMessage());
			} else {
				EasLogUser.logExceptionAsUser(bs.getLoginAtDomain(), e, logger);
			}
			changes.version = version;
		} catch (Exception e) {
			EasLogUser.logExceptionAsUser(bs.getLoginAtDomain(), e, logger);
			// BM-7227
			// Something went wrong
			// Send current version number to prevent full sync
			changes.version = version;
		}

		return changes;
	}

	public CollectionItem store(BackendSession bs, CollectionId collectionId, Optional<String> sid,
			IApplicationData data, ConflicResolution conflictPolicy, SyncState syncState) throws ActiveSyncException {
		CollectionItem ret = null;
		try {
			HierarchyNode folder = storage.getHierarchyNode(bs, collectionId);
			IAddressBook service = getAddressbookService(bs, folder.containerUid);

			MSContact d = (MSContact) data;

			ContactConverter contactConverter = new ContactConverter();
			VCard vcard = contactConverter.contact(d);
			Long id = null;
			String uid = null;
			if (sid.isPresent()) {
				EasLogUser.logInfoAsUser(bs.getLoginAtDomain(), logger,
						"update in " + collectionId + " (contact: " + d.getFirstName() + " " + d.getLastName() + ")");
				String serverId = sid.get();
				id = getItemId(serverId);
				ItemValue<VCard> prevVersion = service.getCompleteById(id);
				if (prevVersion == null) {
					EasLogUser.logDebugAsUser(bs.getLoginAtDomain(), logger, "Fail to find VCard {}", id);
					return CollectionItem.of(collectionId, id);
				}

				uid = prevVersion.uid;

				if (conflictPolicy == ConflicResolution.SERVER_WINS && prevVersion.version > syncState.version) {
					throw new ActiveSyncException("Both server and client changes. Conflict resolution is SERVER_WINS");
				}

				if (prevVersion.value.identification.photo && (d.getPicture() == null || d.getPicture().isEmpty())) {
					service.deletePhoto(uid);
				}

				contactConverter.mergeEmails(vcard, prevVersion.value);

				service.updateById(id, vcard);
			} else {
				uid = UUID.randomUUID().toString();
				EasLogUser.logInfoAsUser(bs.getLoginAtDomain(), logger,
						"create in " + collectionId + " (contact: " + d.getFirstName() + " " + d.getLastName() + ")");
				service.create(uid, vcard);

				ItemValue<VCard> created = service.getComplete(uid);
				id = created.internalId;
			}

			if (d.getPicture() != null && !d.getPicture().isEmpty()) {
				try {
					EasLogUser.logInfoAsUser(bs.getLoginAtDomain(), logger, "Setting picture for contact");
					service.setPhoto(uid, Base64.getDecoder().decode(d.getPicture()));
				} catch (Exception e) {
					EasLogUser.logErrorExceptionAsUser(bs.getLoginAtDomain(), e, logger,
							"Fail to set contact picture");
				}
			}
			ret = CollectionItem.of(collectionId, id);
		} catch (ServerFault e) {
			throw new ActiveSyncException(e);
		}

		return ret;

	}

	public void delete(BackendSession bs, Collection<CollectionItem> serverIds) throws ActiveSyncException {
		if (serverIds != null) {
			try {

				for (CollectionItem serverId : serverIds) {
					HierarchyNode folder = storage.getHierarchyNode(bs, serverId.collectionId);
					IAddressBook service = getAddressbookService(bs, folder.containerUid);
					service.deleteById(serverId.itemId);
				}
			} catch (ServerFault e) {
				if (e.getCode() == ErrorCode.PERMISSION_DENIED) {
					throw new ActiveSyncException(e);
				}
				EasLogUser.logExceptionAsUser(bs.getLoginAtDomain(), e, logger);
			}
		}
	}

	public AppData fetch(BackendSession bs, ItemChangeReference ic) throws ActiveSyncException {
		try {
			HierarchyNode folder = storage.getHierarchyNode(bs, ic.getServerId().collectionId);
			IAddressBook service = getAddressbookService(bs, folder.containerUid);

			ItemValue<VCard> vcard = service.getCompleteById(ic.getServerId().itemId);
			AppData ret = toAppData(service, vcard);

			return ret;
		} catch (Exception e) {
			throw new ActiveSyncException(e.getMessage(), e);
		}
	}

	public Map<Long, AppData> fetchMultiple(BackendSession bs, CollectionId collectionId, List<Long> ids)
			throws ActiveSyncException {
		HierarchyNode folder = storage.getHierarchyNode(bs, collectionId);
		IAddressBook service = getAddressbookService(bs, folder.containerUid);

		List<ItemValue<VCard>> vcards = service.multipleGetById(ids);
		Map<Long, AppData> res = new HashMap<>(ids.size());

		vcards.stream().filter(v -> v.value.kind == Kind.individual).forEach(vcard -> {
			try {
				AppData data = toAppData(service, vcard);
				res.put(vcard.internalId, data);
			} catch (Exception e) {
				EasLogUser.logErrorExceptionAsUser(bs.getLoginAtDomain(), e, logger, "Fail to convert vcard {}",
						vcard.uid);
			}
		});

		return res;
	}

	private AppData toAppData(IAddressBook service, ItemValue<VCard> vcard) {
		MSContact msContact = new ContactConverter().convert(service, vcard);

		ContactResponse cr = OldFormats.update(msContact);
		AppData data = AppData.of(cr);

		if (!msContact.getData().trim().isEmpty()) {
			final AirSyncBaseResponse airSyncBase = new AirSyncBaseResponse();
			airSyncBase.body = new AirSyncBaseResponse.Body();
			airSyncBase.body.type = BodyType.PlainText;
			airSyncBase.body.data = DisposableByteSource.wrap(msContact.getData().trim());
			airSyncBase.body.estimatedDataSize = (int) airSyncBase.body.data.size();
			data.body = LazyLoaded.loaded(airSyncBase);
		}
		return data;
	}

}
