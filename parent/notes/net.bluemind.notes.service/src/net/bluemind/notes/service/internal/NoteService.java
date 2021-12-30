/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.notes.service.internal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.service.ChangeLogUtil;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.core.validator.Validator;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.notes.api.INote;
import net.bluemind.notes.api.VNote;
import net.bluemind.notes.api.VNoteChanges;
import net.bluemind.notes.api.VNoteQuery;
import net.bluemind.notes.persistence.VNoteIndexStore;
import net.bluemind.notes.persistence.VNoteStore;
import net.bluemind.notes.service.VNoteContainerStoreService;

public class NoteService implements INote {

	private static final Logger logger = LoggerFactory.getLogger(NoteService.class);
	private VNoteContainerStoreService storeService;
	private VNoteIndexStore indexStore;
	private VNoteSanitizer sanitizer;
	private VNoteValidator validator;
	private NoteEventProducer eventProducer;
	private SecurityContext context;
	private Sanitizer extSanitizer;
	private Validator extValidator;
	private BmContext bmContext;
	private Container container;
	private RBACManager rbacManager;
	private VNoteStore vnoteStore;

	public NoteService(DataSource pool, Client esearchClient, Container container, BmContext bmContext) {
		this.bmContext = bmContext;
		this.container = container;
		this.vnoteStore = new VNoteStore(pool, container);

		storeService = new VNoteContainerStoreService(bmContext, pool, bmContext.getSecurityContext(), container,
				vnoteStore);

		indexStore = new VNoteIndexStore(esearchClient, container, DataSourceRouter.location(bmContext, container.uid));

		eventProducer = new NoteEventProducer(container, bmContext.getSecurityContext(), VertxPlatform.eventBus());

		sanitizer = new VNoteSanitizer();

		validator = new VNoteValidator();
		context = bmContext.getSecurityContext();

		extSanitizer = new Sanitizer(bmContext);
		extValidator = new Validator(bmContext);
		rbacManager = RBACManager.forContext(bmContext).forContainer(container);
	}

	@Override
	public List<ItemValue<VNote>> all() throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.all();
	}

	@Override
	public void create(String uid, VNote note) throws ServerFault {
		Item item = Item.create(uid, null);
		create(item, note);
	}

	@Override
	public void createWithItem(ItemValue<VNote> noteItem) throws ServerFault {
		create(noteItem.item(), noteItem.value);
	}

	private void create(Item item, VNote note) {
		rbacManager.check(Verb.Write.name());
		doCreate(item, note);
		eventProducer.vnoteCreated(item.uid, note);
		eventProducer.changed();
		indexStore.refresh();
	}

	private void doCreate(Item item, VNote note) throws ServerFault {
		sanitizer.sanitize(note);
		extSanitizer.create(note);
		validator.validate(note);
		extValidator.create(note);
		item.displayName = note.subject;
		ItemVersion iv = storeService.create(item, note);
		indexStore.create(Item.create(item.uid, iv.id), note);
	}

	private void doCreateOrUpdate(String uid, VNote note) throws ServerFault {
		Item item = Item.create(uid, null);
		try {
			doCreate(item, note);
		} catch (ServerFault sf) {
			if (sf.getCode() == ErrorCode.ALREADY_EXISTS) {
				logger.warn("Note uid {} was sent as created but already exists. We update it", uid);
				doUpdate(item, note);
			} else {
				throw sf;
			}
		}
	}

	@Override
	public void update(String uid, VNote note) throws ServerFault {
		Item item = Item.create(uid, null);
		update(item, note);
	}

	@Override
	public void updateWithItem(ItemValue<VNote> noteItem) throws ServerFault {
		update(noteItem.item(), noteItem.value);
	}

	private void update(Item item, VNote note) throws ServerFault {
		rbacManager.check(Verb.Write.name());
		ItemValue<VNote> previous = doUpdate(item, note);
		eventProducer.vnoteUpdated(item.uid, previous.value, note);
		eventProducer.changed();
		indexStore.refresh();
	}

	private ItemValue<VNote> doUpdate(Item item, VNote note) throws ServerFault {
		ItemValue<VNote> previousItemValue = storeService.get(item.uid, null);
		if (previousItemValue == null || previousItemValue.value == null) {
			throw new ServerFault("VNote uid:" + item.uid + " doesn't exist !", ErrorCode.NOT_FOUND);
		}

		sanitizer.sanitize(note);
		extSanitizer.update(previousItemValue.value, note);

		validator.validate(note);
		extValidator.update(previousItemValue.value, note);

		storeService.update(item, note.subject, note);
		indexStore.update(Item.create(item.uid, previousItemValue.internalId), note);
		return previousItemValue;
	}

	private void doUpdateOrCreate(String uid, VNote note) throws ServerFault {
		Item item = Item.create(uid, null);
		try {
			doUpdate(item, note);
		} catch (ServerFault sf) {
			if (sf.getCode() == ErrorCode.NOT_FOUND) {
				logger.warn("Note uid {} was sent as created but already exists. We update it", uid);
				doCreate(item, note);
			} else {
				throw sf;
			}
		}
	}

	@Override
	public ItemValue<VNote> getComplete(String uid) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.get(uid, null);
	}

	@Override
	public List<ItemValue<VNote>> multipleGet(List<String> uids) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.getMultiple(uids);
	}

	@Override
	public List<ItemValue<VNote>> multipleGetById(List<Long> ids) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.getMultipleById(ids);
	}

	@Override
	public void delete(String uid) throws ServerFault {
		rbacManager.check(Verb.Write.name());
		ItemValue<VNote> previousItemValue = doDelete(uid);
		if (previousItemValue != null) {
			eventProducer.vnoteDeleted(uid, previousItemValue.value);
			eventProducer.changed();
			indexStore.refresh();
		}
	}

	private ItemValue<VNote> doDelete(String uid) throws ServerFault {
		ItemValue<VNote> previousItemValue = storeService.get(uid, null);
		if (previousItemValue != null) {
			storeService.delete(uid);
			indexStore.delete(previousItemValue.internalId);
			return previousItemValue;
		}
		return null;
	}

	@Override
	public ContainerChangelog containerChangelog(Long since) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.changelog(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<String> changeset(Long since) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.changeset(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<Long> changesetById(Long since) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.changesetById(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<ItemVersion> filteredChangesetById(Long since, ItemFlagFilter filter) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.changesetById(since, filter);
	}

	@Override
	public ItemChangelog itemChangelog(String itemUid, Long since) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return ChangeLogUtil.getItemChangeLog(itemUid, since, bmContext, storeService, container.domainUid);
	}

	@Override
	public void reset() throws ServerFault {
		rbacManager.check(Verb.Manage.name());
		storeService.deleteAll();
		eventProducer.changed();
		indexStore.deleteAll();
	}

	@Override
	public long getVersion() throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.getVersion();
	}

	@Override
	public Count count(ItemFlagFilter filter) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.count(filter);
	}

	@Override
	public ItemValue<VNote> getCompleteById(long id) {
		return storeService.get(id, null);
	}

	@Override
	public Ack updateById(long id, VNote value) {
		ItemVersion upd = storeService.update(id, value.subject, value);
		return Ack.create(upd.version);
	}

	@Override
	public Ack createById(long id, VNote value) {
		ItemVersion version = storeService.createWithId("note-by-id-" + id, id, null, value.subject, value);
		return Ack.create(version.version);
	}

	@Override
	public void deleteById(long id) {
		ItemValue<VNote> prev = getCompleteById(id);
		if (prev != null) {
			delete(prev.uid);
		}
	}

	public List<String> allUids() throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.allUids();
	}

	@Override
	public List<Long> sortedIds(SortDescriptor sorted) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		try {
			return vnoteStore.sortedIds(sorted);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void xfer(String serverUid) throws ServerFault {

		DataSource ds = bmContext.getMailboxDataSource(serverUid);
		ContainerStore cs = new ContainerStore(null, ds, bmContext.getSecurityContext());
		Container c;
		try {
			c = cs.get(container.uid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		storeService.xfer(ds, c, new VNoteStore(ds, c));

	}

	@Override
	public void multipleDeleteById(List<Long> ids) throws ServerFault {
		ids.forEach(this::deleteById);
	}

	@Override
	public ListResult<ItemValue<VNote>> search(VNoteQuery query) throws ServerFault {
		rbacManager.check(Verb.Read.name());

		ListResult<String> res = indexStore.search(query);

		List<ItemValue<VNote>> values = new ArrayList<>(res.values.size());
		res.values.forEach(uid -> values.add(getComplete(uid)));

		ListResult<ItemValue<VNote>> ret = new ListResult<>();
		ret.total = res.total;
		ret.values = values;

		return ret;
	}

	@Override
	public ContainerUpdatesResult updates(VNoteChanges changes) throws ServerFault {
		rbacManager.check(Verb.Write.name());
		boolean change = false;

		ContainerUpdatesResult ret = new ContainerUpdatesResult();
		ret.added = new ArrayList<String>();
		ret.updated = new ArrayList<String>();
		ret.removed = new ArrayList<String>();
		ret.errors = new ArrayList<>();

		if (changes.add != null && !changes.add.isEmpty()) {
			change = true;
			for (VNoteChanges.ItemAdd add : changes.add) {
				try {
					doCreateOrUpdate(add.uid, add.value);
					ret.added.add(add.uid);
				} catch (ServerFault sf) {
					ret.errors.add(ContainerUpdatesResult.InError.create(sf.getMessage(), sf.getCode(), add.uid));
					logger.error(sf.getMessage(), sf);
				}
			}
		}

		if (changes.modify != null && !changes.modify.isEmpty()) {
			change = true;
			for (VNoteChanges.ItemModify update : changes.modify) {
				try {
					doUpdateOrCreate(update.uid, update.value);
					ret.updated.add(update.uid);
				} catch (ServerFault sf) {
					ret.errors.add(ContainerUpdatesResult.InError.create(sf.getMessage(), sf.getCode(), update.uid));
					logger.error(sf.getMessage(), sf);
				}
			}
		}

		if (changes.delete != null && !changes.delete.isEmpty()) {
			change = true;
			for (VNoteChanges.ItemDelete item : changes.delete) {
				try {
					doDelete(item.uid);
					ret.removed.add(item.uid);
				} catch (ServerFault sf) {
					if (sf.getCode() == ErrorCode.NOT_FOUND) {
						logger.warn("Note uid {} was sent as deleted but does not exist.", item.uid);
						ret.removed.add(item.uid);
					} else {
						ret.errors.add(ContainerUpdatesResult.InError.create(sf.getMessage(), sf.getCode(), item.uid));
						logger.error(sf.getMessage(), sf);
					}
				}
			}

		}

		if (change) {
			eventProducer.changed();
			indexStore.refresh();
		}
		ret.version = storeService.getVersion();
		return ret;
	}

}
